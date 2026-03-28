package com.example.kafkalearn;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

@Slf4j
public class ProducerMain {

  public static void main(String[] args) throws ExecutionException, InterruptedException {

    // 1. Producer 설정
    Properties properties = new Properties();
    properties.put("bootstrap.servers", "localhost:9092,localhost:9093,localhost:9094");
    properties.put("key.serializer", StringSerializer.class.getName()); //
    properties.put("value.serializer", StringSerializer.class.getName());


    Producer<String, String> producer = new KafkaProducer<>(properties);

    // 2. 메시지 전송
    String topic = "test-topic";
    ProducerRecord<String, String> record = new ProducerRecord<>(topic, "key1", "hello world");
    Future<RecordMetadata> send = producer.send(record, (metadata, exception) -> {

      if(exception == null) {
        log.info("메지지 전송 성공 - topic: {}, partition: {}, offset: {}",
            metadata.topic(), metadata.partition(), metadata.offset());
        return;
      }

      log.error("메시지 전송 실패", exception);
    });


    send.get();

    // 3. 프로듀서 연결 종료
    producer.close();

  }

}
