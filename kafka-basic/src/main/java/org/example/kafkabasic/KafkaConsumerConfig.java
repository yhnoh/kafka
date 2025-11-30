package org.example.kafkabasic;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {


    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            DefaultKafkaConsumerFactory<Object, Object> kafkaConsumerFactory,
            DefaultErrorHandler errorHandler) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(kafkaConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }


    /**
     * Kafka Consumer 에러 발생시 처리하기 위한 핸들러 설정
     */
    @Bean
    public DefaultErrorHandler kafkaDefaultErrorHandler() {

        // Dead Letter 토픽으로 메시지를 보내는 설정
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (consumerRecord, ex) -> {
                    String originalTopic = consumerRecord.topic();
                    String dltTopic = originalTopic + "-dlt";
                    // DLT 토픽으로 라우팅
                    return new TopicPartition(
                            dltTopic,
                            0);
                });

        // 2초 간격으로 최대 3회 재시도
        FixedBackOff backOff = new FixedBackOff(
                2000L,
                3L
        );

        return new DefaultErrorHandler(recoverer, backOff);
    }
}
