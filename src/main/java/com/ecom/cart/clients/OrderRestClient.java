package com.ecom.cart.clients;

import com.ecom.cart.model.Order;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "orders-service", url = "${orders.service.url}")
public interface OrderRestClient {

    @PostMapping("/_internal/orderFindUserOrderStatus")
    @CircuitBreaker(name="order", fallbackMethod = "getDefaultFindByUserIdAndOrderStatus")
    Order findByUserIdAndOrderStatus(@RequestHeader("Authorization") String authorization, @RequestBody Map<String, String> mapOrder);

    @PostMapping("/_internal/order-save")
    ResponseEntity<Void> orderSave(@RequestHeader("Authorization") String authorization, @RequestBody Order order);

    @GetMapping("/_internal/orderFindById/{id}")
    @CircuitBreaker(name="order", fallbackMethod = "getDefaultOrderFindById")
    Order findById(@RequestHeader("Authorization") String authorization, @PathVariable Long id);

    default Order getDefaultFindByUserIdAndOrderStatus(String authorization, Map<String, String> mapOrder, Exception e) {
       Order order = new Order();
       order.setId(null);
       return order;
   }

    default Order getDefaultOrderFindById(String authorization,Long id, Exception e) {
        Order order = new Order();
        order.setId(null);
        return order;
    }


}
