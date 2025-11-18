package org.example.kafkabasic.eda.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.kafkabasic.eda.event.OrderCreatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEdaListener {

    @KafkaListener(topics = "order-events", groupId = "payment-service")
    public void processPayment(
            @Payload OrderCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("이벤트 수신 이후 결제 처리 시작");

        try {
            // 로직 수행 및 처리 완료 후 메시지 커밋
            acknowledgment.acknowledge();
            log.info("이벤트 수신 이후 결제 처리 완료");

        } catch (Exception e) {
            log.error("결제 처리 도중 에러 발생", e);
            // 커밋하지 않으면 재처리됨
            // 또는 DLT로 전송
        }
    }

    @KafkaListener(topics = "order-events", groupId = "inventory-service")
    public void decreaseStock(
            @Payload OrderCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {

        log.info("이벤트 수신 이후 재고 차감 시작");

        try {
            // 로직 수행 및 처리 완료 후 메시지 커밋
            acknowledgment.acknowledge();
            log.info("이벤트 수신 이후 재고 차감 완료");

        } catch (Exception e) {
            log.error("재고 차감 도중 에러 발생", e);
            // 커밋하지 않으면 재처리됨
            // 또는 DLT로 전송
        }

    }

    @KafkaListener(topics = "order-events", groupId = "notification-service")
    public void sendNotification(
            @Payload OrderCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("이벤트 수신 이후 알람 전송 시작");

        try {
            // 로직 수행 및 처리 완료 후 메시지 커밋
            acknowledgment.acknowledge();
            log.info("이벤트 수신 이후 알람 전송 완료");

        } catch (Exception e) {
            log.error("알람 전송 도중 에러 발생", e);
            // 커밋하지 않으면 재처리됨
            // 또는 DLT로 전송
        }
    }

}
