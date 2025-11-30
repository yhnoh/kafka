package org.example.kafkabasic.eda.repository;

import org.example.kafkabasic.eda.domain.OrderOutboxEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderOutboxEventRepository extends JpaRepository<OrderOutboxEventJpaEntity, String> {


    /**
     * 발행되지 않은 이벤트들을 생성일시 오름차순으로 조회
     */
    List<OrderOutboxEventJpaEntity> findByPublishedIsFalseOrderByCreatedAtAsc();

    /**
     * 재시도 횟수 제한
     */
    List<OrderOutboxEventJpaEntity> findByPublishedIsFalseAndRetryCountLessThanOrderByCreatedAtAsc(int maxRetryCount);

    /**
     * 오래전 발행된 이벤트 정리를 위한 조회
     */
    List<OrderOutboxEventJpaEntity> findByPublishedTrueAndPublishedAtBefore(LocalDateTime publishedAt);


    /**
     * 전송 되지 않은 특정 AggregateId의 이벤트 조회
     */
    Optional<OrderOutboxEventJpaEntity> findByPublishedIsFalseAndId(String Id);
}
