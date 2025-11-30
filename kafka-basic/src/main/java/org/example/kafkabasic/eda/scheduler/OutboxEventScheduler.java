package org.example.kafkabasic.eda.scheduler;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.kafkabasic.eda.domain.OrderOutboxEventJpaEntity;
import org.example.kafkabasic.eda.service.OutboxEventService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventScheduler {

    private static final int MAX_RETRY = 5;
    private final OutboxEventService outboxEventService;


    /**
     * 1초마다 미발행 이벤트 확인 및 발행
     * 트랜잭션 없음 (각 이벤트는 별도 트랜잭션)
     */
    @Scheduled(cron = "* * * * * *")
    public void publishOutboxEvents() {
        List<OrderOutboxEventJpaEntity> events = outboxEventService
                .findByPublishedIsFalseAndRetryCountLessThanOrderByCreatedAtAsc(MAX_RETRY);

        if (events.isEmpty()) {
            return;
        }

        for (OrderOutboxEventJpaEntity event : events) {
            outboxEventService.publishSingleEvent(event);
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void cleanupOldEvents() {
        var cutoffDate = LocalDateTime.now().minusDays(7);
        List<OrderOutboxEventJpaEntity> oldEvents = outboxEventService
                .findByPublishedTrueAndPublishedAtBefore(cutoffDate);

        if (!oldEvents.isEmpty()) {
            outboxEventService.deleteAll(oldEvents);
            log.info("오래된 Outbox 이벤트 {}개 정리 완료", oldEvents.size());
        }
    }
}
