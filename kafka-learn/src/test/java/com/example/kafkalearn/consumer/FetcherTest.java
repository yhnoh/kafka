package com.example.kafkalearn.consumer;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;

@Slf4j
public class FetcherTest {


  private static final String BOOTSTRAP_SERVERS = "localhost:9092,localhost:9093,localhost:9094";
  private static final String CONSUMER_GROUP_ID = "consumer_group_1";

  private static void createTopic(String topicName, int numPartitions, short replicationFactor) throws ExecutionException, InterruptedException {
    Properties props = new Properties();
    props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);

    try (AdminClient admin = AdminClient.create(props)) {
      // 삭제가 비동기로 처리되므로, 생성이 성공할 때까지 재시도
      NewTopic topic = new NewTopic(topicName, numPartitions, replicationFactor);
      while (true) {
        try {
          admin.createTopics(List.of(topic)).all().get();
          break;
        } catch (ExecutionException e) {
          if (e.getCause() instanceof TopicExistsException) {
            admin.deleteTopics(List.of(topicName));
            Thread.sleep(500);
          } else {
            throw e;
          }
        }
      }
    }
  }

  private static void produce(String topicName, String key, String value) throws ExecutionException, InterruptedException {
    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
//    props.put(ProducerConfig., StringSerializer.class.getName());
    Producer<String, String> producer = new KafkaProducer<>(props);

    ProducerRecord<String, String> record = new ProducerRecord<>(topicName, key, value);
    producer.send(record).get();
  }

  @Test
  void consumeTest() throws ExecutionException, InterruptedException {

    String topicName = "consume-test-1";
    createTopic(topicName, 1, (short) 3);
    produce(topicName, "1", "1");

    // Consumer 설정
    Properties props = new Properties();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP_ID);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

    Consumer<String, String> consumer = new KafkaConsumer<>(props);
    consumer.subscribe(List.of(topicName));

    while (true) {
      ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
      if (!records.isEmpty()) {
        records.forEach(record ->
        {
          String append = "topic=" + record.topic()
              + ", partition=" + record.partition()
              + ", offset=" + record.offset()
              + ", key=" + record.key()
              + ", value=" + record.value();

          System.out.println("append = " + append);
        });
        break;
      }
    }
  }

}
