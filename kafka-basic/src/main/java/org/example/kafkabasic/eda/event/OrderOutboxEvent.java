package org.example.kafkabasic.eda.event;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class OrderOutboxEvent<T> {

    private String outboxEventId;
    private String aggregateType;
    private String aggregateId;
    private String eventType;
    private T payload;
    private String topic;

    public OrderOutboxEvent(String aggregateType, String aggregateId, String eventType, T payload, String topic) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.topic = topic;
    }
}
