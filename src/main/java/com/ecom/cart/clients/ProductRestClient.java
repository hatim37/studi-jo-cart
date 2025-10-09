package com.ecom.cart.clients;

import com.ecom.cart.model.Product;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "products-service", url = "${products.service.url}")
public interface ProductRestClient {

    @GetMapping("/_internal/productFindById/{id}")
    @CircuitBreaker(name="product", fallbackMethod = "getDefaultProductFindById")
    Product findById(@RequestHeader("Authorization") String authorization, @PathVariable Long id);

    default Product getDefaultProductFindById(String authorization,Long id, Exception e) {
        Product product = new Product();
        product.setId(null);
        product.setName("non trouvé");
        return product;
    }
}
