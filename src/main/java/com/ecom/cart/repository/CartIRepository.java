package com.ecom.cart.repository;

import com.ecom.cart.entity.CartItems;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface CartIRepository extends JpaRepository<CartItems, Long> {
    List<CartItems> findByOrderId(Long orderId);
}
