package org.example.kafkabasic.eda.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.kafkabasic.eda.domain.OrderOutboxEventJpaEntity;
import org.example.kafkabasic.eda.event.OrderOutboxEvent;
import org.example.kafkabasic.eda.repository.OrderOutboxEventRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class OrderOutboxEventListener {

    private final ObjectMapper objectMapper;
    private final OrderOutboxEventRepository orderOutboxEventRepository;

    @EventListener
    public void handleOrderOutbox(OrderOutboxEvent event) throws JsonProcessingException {


        String payload = objectMapper.writeValueAsString(event.getPayload());

        OrderOutboxEventJpaEntity orderOutboxEvent = new OrderOutboxEventJpaEntity(event.getAggregateType(),
                event.getAggregateId(),
                event.getEventType(),
                payload,
                event.getTopic());

        orderOutboxEventRepository.save(orderOutboxEvent);

        event.setOutboxEventId(orderOutboxEvent.getId());
    }
}
