package org.example.kafkabasic;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderProducer orderProducer;

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody CreateOrderRequest request) {
        Order order = new Order(
                UUID.randomUUID().toString(),
                request.getUserId(),
                request.getProductName(),
                request.getAmount(),
                LocalDateTime.now(),
                Order.OrderStatus.PENDING
        );

        orderProducer.sendOrder(order);

        return ResponseEntity.ok(order);
    }

    @PostMapping("/bulk")
    public ResponseEntity<String> createBulkOrders(@RequestParam int count) {
        for (int i = 0; i < count; i++) {
            Order order = new Order(
                    UUID.randomUUID().toString(),
                    "user-" + (i % 5), // 5명의 사용자
                    "Product-" + i,
                    BigDecimal.valueOf(1000 + i),
                    LocalDateTime.now(),
                    Order.OrderStatus.PENDING
            );
            orderProducer.sendOrder(order);
        }
        return ResponseEntity.ok(count + " orders sent");
    }

    @Data
    static class CreateOrderRequest {
        private String userId;
        private String productName;
        private BigDecimal amount;
    }
}
