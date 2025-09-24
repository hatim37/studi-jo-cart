package com.ecom.cart.services;


import com.ecom.cart.clients.OrderRestClient;
import com.ecom.cart.clients.ProductRestClient;
import com.ecom.cart.clients.UserRestClient;
import com.ecom.cart.dto.DecryptDto;
import com.ecom.cart.dto.QrCodeDto;
import com.ecom.cart.entity.CartItems;
import com.ecom.cart.model.Order;
import com.ecom.cart.model.Product;
import com.ecom.cart.model.User;
import com.ecom.cart.repository.CartRepository;
import com.ecom.cart.response.UserNotFoundException;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

@Slf4j
@Service
@Transactional
public class QrCodeService {

    private static final String TEXT_PREFIX = "Billet valide pour le client : ";
    private final OrderRestClient orderRestClient;
    private final UserRestClient userRestClient;
    private final CartRepository cartRepository;
    private final TokenTechnicService tokenTechnicService;
    private final ProductRestClient productRestClient;

    public QrCodeService(OrderRestClient orderRestClient, UserRestClient userRestClient, CartRepository cartRepository, TokenTechnicService tokenTechnicService, ProductRestClient productRestClient) {
        this.orderRestClient = orderRestClient;
        this.userRestClient = userRestClient;
        this.cartRepository = cartRepository;
        this.tokenTechnicService = tokenTechnicService;
        this.productRestClient = productRestClient;
    }

    public void generateQrCode(Long userId, Long orderId) {
        User user = this.userRestClient.findUserById("Bearer " + this.tokenTechnicService.getTechnicalToken(), userId);
        if (user.getId() == null) {
            throw new UserNotFoundException("Service indisponible");
        }
        List<CartItems> cartItems = cartRepository.findByOrderId(orderId);
        cartItems.forEach(item->{
            Product product = this.productRestClient.findById("Bearer " + this.tokenTechnicService.getTechnicalToken(), item.getProductId());
            if (product.getId() == null) {
                throw new UserNotFoundException("Service indisponible");
            }

            try {
                Map<String, String> qrCodeDataMap = Map.of(
                        "commande",item.getOrderId().toString(),
                        "client", user.getId().toString(),
                        "Nom", user.getName(),
                        "Type de billet", product.getName(),
                        "Nombre de place", item.getQuantity().toString(),
                        "Key", this.encryptKey(userId, orderId, user.getName())
                );

                String jsonString = new JSONObject(qrCodeDataMap).toString();

                QRCodeWriter qrCodeWriter = new QRCodeWriter();
                BitMatrix bitMatrix = qrCodeWriter.encode(jsonString, BarcodeFormat.QR_CODE, 400, 400
                );
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                MatrixToImageWriter.writeToStream(bitMatrix, "png", baos);

                item.setQrCode(baos.toByteArray());

                cartRepository.save(item);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public QrCodeDto decryptQrCode(MultipartFile imageQrCode) throws Exception {
        try
        {
            BufferedImage image = ImageIO.read(imageQrCode.getInputStream());
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Reader reader = new MultiFormatReader();
            Result result = reader.decode(bitmap);

            JSONObject obj = new JSONObject(result.getText());
            Iterator<String> it = obj.keys();

            QrCodeDto qrCodeDto = new QrCodeDto();
            qrCodeDto.setCode((String) obj.get("Key"));
            qrCodeDto.setName((String) obj.get("Nom"));
            qrCodeDto.setType((String) obj.get("Type de billet"));
            qrCodeDto.setQuantity((String) obj.get("Nombre de place"));
            qrCodeDto.setCommande((String) obj.get("commande"));
            qrCodeDto.setClient((String) obj.get("client"));
            return qrCodeDto;
        }
        catch(IOException ex)
        {
            System.out.println("I/O Error: " + ex.getMessage());
        } catch (ChecksumException | NotFoundException | FormatException | JSONException e) {
            throw new RuntimeException(e);
        }
        return null;
    }


    public String encryptKey(Long userId, Long orderId, String text) throws Exception {
        SecretKeySpec secretKey = this.getKeyFormUserAndOrder(userId,orderId);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        // IV aléatoire 16 octets
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

        byte[] plaintext = (TEXT_PREFIX + text).getBytes(StandardCharsets.UTF_8);
        byte[] ciphertext = cipher.doFinal(plaintext);

        // Concaténer
        byte[] ivAndCipher = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, ivAndCipher, 0, iv.length);
        System.arraycopy(ciphertext, 0, ivAndCipher, iv.length, ciphertext.length);

        return Base64.getEncoder().encodeToString(ivAndCipher);
    }

    public DecryptDto decryptKey(Long userId, Long orderId, String codeDecrypt) throws Exception {
        //Récupération de la SecretKey
        SecretKeySpec secretKey = this.getKeyFormUserAndOrder(userId, orderId);
        // Décodage Base64
        byte[] ivAndCiphertext = Base64.getDecoder().decode(codeDecrypt);
        if (ivAndCiphertext.length < 17) {
            throw new IllegalArgumentException("Données chiffrées non valides");
        }

        // Extraire IV (16 octets) et ciphertext
        byte[] iv = Arrays.copyOfRange(ivAndCiphertext, 0, 16);
        byte[] ciphertext = Arrays.copyOfRange(ivAndCiphertext, 16, ivAndCiphertext.length);
        // Déchiffrement AES
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
        byte[] decryptedBytes = cipher.doFinal(ciphertext);
        String result = new String(decryptedBytes, StandardCharsets.UTF_8);

        DecryptDto decryptDto = new DecryptDto();
        decryptDto.setOutputCode(result);
        return decryptDto;
    }

    //Récupérer, déchiffrer et concaténer les 2 clés
    public SecretKeySpec getKeyFormUserAndOrder(Long userId, Long orderId) throws NoSuchAlgorithmException {
        //on récupère les clés
        User user = userRestClient.findUserById("Bearer " + this.tokenTechnicService.getTechnicalToken(), userId);
        if (user.getId() == null) {
            throw new UserNotFoundException("Service indisponible");
        }

        Order order = orderRestClient.findById("Bearer " + this.tokenTechnicService.getTechnicalToken(),orderId);
        if (order.getId() == null) {
            throw new UserNotFoundException("Service indisponible");
        }
        // Décodage Base64 (vérifier format)
        byte[] userKey;
        byte[] orderKey;
        try {
            userKey = Base64.getDecoder().decode(user.getSecretKey());
            orderKey = Base64.getDecoder().decode(order.getSecretKey());
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException("Invalid Base64 key in DB", iae);
        }
        // Concaténation
        byte[] combined = new byte[userKey.length + orderKey.length];
        System.arraycopy(userKey, 0, combined, 0, userKey.length);
        System.arraycopy(orderKey, 0, combined, userKey.length, orderKey.length);
        // Dérivation via SHA-256 -> 32 octets => AES-256
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] derived = sha.digest(combined);

        return new SecretKeySpec(derived, "AES"); // 32 bytes -> AES-256
    }


}
