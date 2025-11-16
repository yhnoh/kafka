package org.example.kafkabasic;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderConsumer {

    @KafkaListener(
            topics = KafkaTopicConfig.ORDER_TOPIC,
            groupId = "order-consumer-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload Order order,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("Received order: orderId={}, partition={}, offset={}",
                order.getOrderId(), partition, offset);

        try {
            // 주문 처리 로직
            processOrder(order);

            // 수동 커밋 (처리 완료 후)
            acknowledgment.acknowledge();
            log.info("Order processed successfully: {}", order.getOrderId());

        } catch (Exception e) {
            log.error("Failed to process order: {}", order.getOrderId(), e);
            // 커밋하지 않으면 재처리됨
            // 또는 DLT로 전송
        }
    }

    private void processOrder(Order order) {
        // 실제 비즈니스 로직
        log.info("Processing order: product={}, amount={}",
                order.getProductName(), order.getAmount());

        // 예: DB 저장, 재고 확인, 결제 처리 등
        // orderRepository.save(order);
    }
}
