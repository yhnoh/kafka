package org.example.kafkabasic.eda.domain;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@ToString
@Entity
@Table(name = "orders")
public class OrderJpaEntity {

    @Id
    private String id;

    private String userId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "order", orphanRemoval = true)
    private List<OrderItemJpaEntity> orderItems = new ArrayList<>();

    private BigDecimal totalAmount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public OrderJpaEntity(String userId, List<OrderItemJpaEntity> orderItems) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.orderItems = orderItems;
        this.totalAmount = calculateTotal();
        this.status = OrderStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    private BigDecimal calculateTotal() {
        return orderItems.stream()
                .map(item -> item.getPrice().multiply(
                        BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void confirm() {
        this.status = OrderStatus.CONFIRMED;
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    enum OrderStatus {
        PENDING,      // 대기
        CONFIRMED,    // 확인
        PAID,         // 결제 완료
        SHIPPED,      // 배송 중
        DELIVERED,    // 배송 완료
        CANCELLED     // 취소
    }
}
