package com.example.kafkalearn.producer.partitioner;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.RoundRobinPartitioner;
import org.apache.kafka.common.Cluster;
import org.junit.jupiter.api.Test;

public class ProducerPartitionerTest {


  public static class CustomPartitioner implements Partitioner {

    @Override
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
      int numPartitions = cluster.availablePartitionsForTopic(topic).size();

      if (key != null) {
        return Objects.hash(key) % numPartitions;
      }
      return Objects.hash(value) % numPartitions;
    }

    @Override
    public void close() {

    }

    @Override
    public void configure(Map<String, ?> configs) {

    }
  }


  @Test
  void settingTest() {
    Map<String, Object> configs = new HashMap();
    Properties prop = new Properties();
    prop.setProperty("bootstrap.servers", "localhost:9092,localhost:9093,localhost:9094");
    prop.setProperty("partitioner.class", RoundRobinPartitioner.class.getName());

    Producer<String, String> producer = new KafkaProducer<>(prop);
  }

}
