package com.example.kafkalearn.producer.serializer;

import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ProducerSerializerTest {

  @Test
  @DisplayName("Serializer 테스트")
  void serialize(){
    StringSerializer stringSerializer = new StringSerializer();
    byte[] serialize = stringSerializer.serialize("test-topic", "hello world");

    StringDeserializer deserializer = new StringDeserializer();
    String deserialize = deserializer.deserialize("test-topic", serialize);

    System.out.println("deserialize = " + deserialize);
  }

  @Test
  @DisplayName("프로듀서 설정")
  void setting(){

    Properties prop = new Properties();
    prop.setProperty("bootstrap.servers", "localhost:9092,localhost:9093,localhost:9094");
    prop.setProperty("key.serializer", StringSerializer.class.getName());
    prop.setProperty("value.serializer", StringSerializer.class.getName());

    Producer<String, String> producer = new KafkaProducer<>(prop);

  }

}
