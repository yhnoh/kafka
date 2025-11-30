package org.example.kafkabasic.eda.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.kafkabasic.eda.domain.OrderOutboxEventJpaEntity;
import org.example.kafkabasic.eda.repository.OrderOutboxEventRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OutboxEventService {

    private static final int MAX_RETRY = 5;
    private final OrderOutboxEventRepository orderOutboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 이벤트 발행 완료 처리
     */
    public void markAsPublished(String Id) {
        OrderOutboxEventJpaEntity outboxEventJpaEntity = orderOutboxEventRepository.findByPublishedIsFalseAndId(Id)
                .orElseThrow(() -> new IllegalArgumentException("Outbox event not found or already published: " + Id));

        outboxEventJpaEntity.markAsPublished();
        orderOutboxEventRepository.save(outboxEventJpaEntity);
    }

    public List<OrderOutboxEventJpaEntity> findByPublishedIsFalseAndRetryCountLessThanOrderByCreatedAtAsc(int maxRetry) {
        return orderOutboxEventRepository.findByPublishedIsFalseAndRetryCountLessThanOrderByCreatedAtAsc(MAX_RETRY);
    }

    public List<OrderOutboxEventJpaEntity> findByPublishedTrueAndPublishedAtBefore(LocalDateTime publishedAt) {
        return orderOutboxEventRepository.findByPublishedTrueAndPublishedAtBefore(publishedAt);
    }

    @Transactional
    public void deleteAll(List<OrderOutboxEventJpaEntity> events) {
        orderOutboxEventRepository.deleteAll(events);
    }

    @Transactional
    public void publishSingleEvent(OrderOutboxEventJpaEntity event) {
        log.info("이벤트 발행 시도: id={}, type={}, retry={}",
                event.getId(), event.getEventType(), event.getRetryCount());

        // Kafka 전송 (동기)
        try {
            kafkaTemplate.send(
                    event.getTopic(),
                    event.getAggregateId(),
                    event.getPayload()
            ).get();  // 블로킹 방식으로 전송 확인

            event.markAsPublished();
            orderOutboxEventRepository.save(event);

            log.info("이벤트 발행 성공: id={}, type={}",
                    event.getId(), event.getEventType());

        } catch (InterruptedException | ExecutionException e) {
            log.warn("⚠️ 이벤트 발행 실패 (스케줄러가 재시도 예정): id={}, type={}, error={}",
                    event.getId(), event.getEventType(), e.getMessage());

            // 재시도 카운트 증가
            event.incrementRetryCount(e.getMessage());
            orderOutboxEventRepository.save(event);
        }
    }
}
