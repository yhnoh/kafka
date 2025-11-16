package org.example.kafkabasic;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String ORDER_TOPIC = "orders";
    public static final String ORDER_DLT_TOPIC = "orders-dlt"; // Dead Letter Topic

    @Bean
    public NewTopic orderTopic() {
        return TopicBuilder.name(ORDER_TOPIC)
                .partitions(3)  // 3개 파티션
                .replicas(1)    // 복제본 1개 (로컬이라)
                .build();
    }

    @Bean
    public NewTopic orderDltTopic() {
        return TopicBuilder.name(ORDER_DLT_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
