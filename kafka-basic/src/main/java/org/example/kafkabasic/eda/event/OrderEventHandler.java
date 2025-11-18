package org.example.kafkabasic.eda.event;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventHandler {

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;


    /**
     * AFTER_COMMIT: DB 트랜잭션 성공 후에만 실행
     * - DB 저장 실패하면 이벤트 발행 안 됨
     * - DB 저장 성공하면 이벤트 발행
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("트랜잭션 커밋 후 이벤트 처리 시작: eventId={}",
                event.getEventId());

        try {
            kafkaTemplate.send("order-events", event.getOrderId(), event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Kafka 전송 성공: orderId={}, partition={}, offset={}",
                                    event.getOrderId(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("Kafka 전송 실패: orderId={}, error={}",
                                    event.getOrderId(), ex.getMessage());
                            // TODO: 재시도 로직 또는 Outbox 패턴 필요
                        }
                    });
        } catch (Exception e) {
            log.error("Kafka 전송 중 예외 발생: orderId={}",
                    event.getOrderId(), e);
        }
    }
}
