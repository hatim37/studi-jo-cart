package com.ecom.cart.clients;

import com.ecom.cart.model.User;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "users-service", url = "${users.service.url}")
public interface UserRestClient {

    @GetMapping("/_internal/users/{id}")
    @CircuitBreaker(name="users", fallbackMethod = "getDefaultUser")
    User findUserById(@RequestHeader("Authorization") String authorization,@PathVariable Long id);

   default User getDefaultUser(String authorization,Long id, Exception e) {
       User user = new User();
       user.setId(null);
       user.setName("default");
       user.setEmail("default@email.com");
       user.setActive(false);
       user.setRoles(List.of());
       return user;
   }

}
