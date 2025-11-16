package org.example.kafkabasic;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    private String orderId;
    private String userId;
    private String productName;
    private BigDecimal amount;
    private LocalDateTime createdAt;
    private OrderStatus status;


    public enum OrderStatus {
        PENDING,
        CONFIRMED,
        SHIPPED,
        DELIVERED,
        CANCELLED
    }
}
