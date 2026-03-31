package com.example.kafkalearn.consumer.rebalance;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

/**
 * Rebalance 동작 확인용 예제
 *
 * 실행 방법:
 * 1. docker-compose up -d 로 브로커 실행
 * 2. 이 클래스를 실행하면 컨슈머 2개가 같은 그룹으로 시작
 * 3. 로그에서 파티션 배정 확인
 * 4. 20초 후 컨슈머 1개가 종료되면서 Rebalance 발생
 * 5. 남은 컨슈머가 모든 파티션을 담당하는 것을 확인
 */
public class RebalanceMain {

    private static final String TOPIC = "rebalance-test";
    private static final String GROUP_ID = "rebalance-group";
    private static final String BOOTSTRAP_SERVERS = "localhost:9092,localhost:9093,localhost:9094";

    public static void main(String[] args) throws InterruptedException {

        // 컨슈머 2개를 별도 스레드로 실행
        CountDownLatch latch = new CountDownLatch(2);
        AtomicBoolean consumer1Running = new AtomicBoolean(true);
        AtomicBoolean consumer2Running = new AtomicBoolean(true);

        Thread thread1 = new Thread(() -> runConsumer("컨슈머-1", consumer1Running, latch));
        Thread thread2 = new Thread(() -> runConsumer("컨슈머-2", consumer2Running, latch));

        thread1.start();
        thread2.start();

        // 20초 후 컨슈머-2를 종료 → Rebalance 발생
        System.out.println("\n[메인] 20초 후 컨슈머-2를 종료합니다...\n");
        Thread.sleep(20_000);

        System.out.println("\n========================================");
        System.out.println("[메인] 컨슈머-2 종료 → Rebalance 발생 예상");
        System.out.println("========================================\n");
        consumer2Running.set(false);

        // 리밸런스 후 30초간 관찰
        Thread.sleep(30_000);

        System.out.println("\n[메인] 컨슈머-1도 종료합니다.");
        consumer1Running.set(false);

        latch.await();
        System.out.println("[메인] 모든 컨슈머 종료 완료");
    }

    private static void runConsumer(String name, AtomicBoolean running, CountDownLatch latch) {
        Properties props = new Properties();
        props.put("bootstrap.servers", BOOTSTRAP_SERVERS);
        props.put("group.id", GROUP_ID);
        props.put("key.deserializer", StringDeserializer.class.getName());
        props.put("value.deserializer", StringDeserializer.class.getName());
        props.put("enable.auto.commit", "true");
        props.put("auto.offset.reset", "latest");
        // 리밸런스를 빠르게 감지하기 위해 세션 타임아웃을 짧게 설정
        props.put("session.timeout.ms", "10000");
        props.put("heartbeat.interval.ms", "3000");

        Consumer<String, String> consumer = new KafkaConsumer<>(props);

        // ConsumerRebalanceListener로 Rebalance 과정을 로그로 확인
        consumer.subscribe(List.of(TOPIC), new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                System.out.println("[" + name + "] 파티션 회수됨 (revoked): " + partitions);
            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                System.out.println("[" + name + "] 파티션 할당됨 (assigned): " + partitions);
            }
        });

        try {
            while (running.get()) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                records.forEach(record ->
                    System.out.println("[" + name + "] 메시지 수신 - partition: " + record.partition()
                        + ", offset: " + record.offset() + ", value: " + record.value())
                );
            }
        } catch (Exception e) {
            System.out.println("[" + name + "] 에러 발생: " + e.getMessage());
        } finally {
            consumer.close();
            System.out.println("[" + name + "] 종료됨");
            latch.countDown();
        }
    }
}
