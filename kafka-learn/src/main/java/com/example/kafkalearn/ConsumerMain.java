package com.example.kafkalearn;

import java.util.List;
import java.util.Properties;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

public class ConsumerMain {

  public static void main(String[] args) {

    // 1. Consumer 설정
    Properties properties = new Properties();
    properties.put("bootstrap.servers", "localhost:9092,localhost:9093,localhost:9094");
    properties.put("group.id", "test-group-01");  // 컨슈머 그룹 아이디 설정
    properties.put("enable.auto.commit", "true"); // 오토 커밋
    properties.put("auto.offest.reset", "latest");  // 오프셋이 없는 경우 최신 메시지부터 수신

    // 역직렬화 설정
    properties.put("key.deserializer", StringDeserializer.class.getName());
    properties.put("value.deserializer", StringDeserializer.class.getName());

    Consumer<String, String> consumer = new KafkaConsumer<>(properties);
    // 2. 토픽 지정
    consumer.subscribe(List.of("test-topic"));

    try {
      while (true) {
        // 3. 메시지 수신
        ConsumerRecords<String, String> records = consumer.poll(1000);
        records.forEach(record -> {
          System.out.println("메시지 수신 - topic: " + record.topic() + ", partition: " + record.partition()
              + ", offset: " + record.offset() + ", key: " + record.key() + ", value: " + record.value());
        });
      }
    }catch (Exception e) {
      e.printStackTrace();
    } finally {
      consumer.close();
    }

  }
}
