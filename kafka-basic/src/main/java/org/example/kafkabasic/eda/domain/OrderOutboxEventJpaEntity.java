package org.example.kafkabasic.eda.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@ToString
@Entity
@Table(name = "order_outbox_events")
public class OrderOutboxEventJpaEntity {

    @Id
    private String id;

    /**
     * Aggregate 도메인 객체에서 발생한 이벤트 타입
     */
    private String aggregateType;

    /**
     * Aggregate 도메인 객체의 Id
     */
    private String aggregateId;

    /**
     * 이벤트 타입
     */
    private String eventType;

    /**
     * 이벤트 페이로드(JSON)
     */
    @Column(columnDefinition = "TEXT")
    private String payload;

    /**
     * 토픽 이름
     */
    private String topic;

    /**
     * 이벤트 발행 여부
     */
    private boolean published;

    /**
     * 이벤트 생성 시간
     */
    private LocalDateTime createdAt;

    /**
     * 이벤트 발행 시간
     */
    private LocalDateTime publishedAt;

    /**
     * 재시도 횟수
     */
    private int retryCount;

    @Column(columnDefinition = "TEXT")
    private String lastErrorMessage;

    public OrderOutboxEventJpaEntity(String aggregateType, String aggregateId, String eventType, String payload, String topic) {
        this.id = UUID.randomUUID().toString();
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.topic = topic;
        this.published = false;
        this.createdAt = LocalDateTime.now();
        this.retryCount = 0;
    }

    public void markAsPublished() {
        this.published = true;
        this.publishedAt = LocalDateTime.now();
    }

    public void incrementRetryCount(String errorMessage) {
        this.retryCount++;
        this.lastErrorMessage = errorMessage;
    }
}
