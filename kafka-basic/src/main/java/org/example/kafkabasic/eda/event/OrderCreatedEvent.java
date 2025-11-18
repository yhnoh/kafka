package org.example.kafkabasic.eda.event;

import lombok.*;
import org.example.kafkabasic.eda.domain.OrderItemJpaEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class OrderCreatedEvent {

    // 이벤트 메타데이터
    private final String eventId = UUID.randomUUID().toString();
    private final String eventType = "OrderCreated";
    private final LocalDateTime occurredAt = LocalDateTime.now();

    // 비즈니스 데이터
    private String orderId;
    private String userId;
    private List<OrderItem> items;
    private BigDecimal totalAmount;

    public OrderCreatedEvent(String orderId, String userId, List<OrderItem> items, BigDecimal totalAmount) {
        this.orderId = orderId;
        this.userId = userId;
        this.items = items;
        this.totalAmount = totalAmount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        private String productId;
        private String productName;
        private Integer quantity;
        private BigDecimal price;

        public static OrderItem from(OrderItemJpaEntity orderItem) {
            return new OrderItem(
                    orderItem.getProductId(),
                    orderItem.getProductName(),
                    orderItem.getQuantity(),
                    orderItem.getPrice()
            );
        }
    }
}
