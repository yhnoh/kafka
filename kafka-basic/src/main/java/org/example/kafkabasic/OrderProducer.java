package org.example.kafkabasic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderProducer {


    private final KafkaTemplate<String, Order> kafkaTemplate;

    public void sendOrder(Order order) {
        log.info("Sending order: {}", order);

        // kafka 토픽 key/value 전송
        CompletableFuture<SendResult<String, Order>> sender = kafkaTemplate.send(
                KafkaTopicConfig.ORDER_TOPIC,
                order.getOrderId(),
                order);

        sender.whenComplete((result, e) -> {
            if (e == null) {
                log.info("Order sent successfully: orderId={}, partition={}, offset={}",
                        order.getOrderId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send order: orderId={}, error={}",
                        order.getOrderId(), e.getMessage());
            }
        });
    }

    /**
     * 동기 방식 전송 (응답 대기)
     */
    public void sendOrderSync(Order order) throws Exception {
        log.info("Sending order synchronously: {}", order);

        SendResult<String, Order> result = kafkaTemplate
                .send(KafkaTopicConfig.ORDER_TOPIC, order.getOrderId(), order)
                .get(); // 블로킹

        log.info("Order sent: partition={}, offset={}",
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
    }
}
