package org.example.kafkabasic.eda.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.kafkabasic.eda.domain.OrderItemJpaEntity;
import org.example.kafkabasic.eda.domain.OrderJpaEntity;
import org.example.kafkabasic.eda.event.OrderCreatedEvent;
import org.example.kafkabasic.eda.event.OrderOutboxEvent;
import org.example.kafkabasic.eda.repository.OrderJpaRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderJpaRepository orderJpaRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public OrderJpaEntity createOrder(String userId, List<OrderItemJpaEntity> orderItems) {
        log.info("주문 생성 시작: userId={}", userId);

        // 1. 주문 저장
        OrderJpaEntity order = new OrderJpaEntity(userId, orderItems);
        log.info("주문 저장 완료: orderId={}", order.getId());
        orderJpaRepository.save(order);


        // 2. Spring Event 발행
        OrderCreatedEvent event = new OrderCreatedEvent(
                order.getId(),
                order.getUserId(),
                order.getOrderItems().stream()
                        .map(OrderCreatedEvent.OrderItem::from)
                        .toList(),
                order.getTotalAmount()
        );

        //Outbox Pattern을 활용하기 위한 Event 생성
        OrderOutboxEvent<OrderCreatedEvent> orderOutboxEvent = new OrderOutboxEvent<>(
                "Order",
                order.getId(),
                "OrderCreated",
                event,
                "order-event"
        );

        eventPublisher.publishEvent(orderOutboxEvent);
        log.info("Spring Event 발행: eventId={}", event.getEventId());

        return order;
    }

}
