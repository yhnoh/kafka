### 카프카 서버

#### 메시지 보존

```
ls -lh /var/lib/kafka/data/

Partition-0/ -> Partition 0의 데이터
Partition-1/ -> Partition 1의 데이터
Partition-2/ -> Partition 2의 데이터

# 실제 파일들:
# 00000000000000000000.log    → 실제 메시지 데이터
# 00000000000000000000.index  → offset 인덱스
# 00000000000000000000.timeindex → timestamp 인덱스
```

- 메시지는 Partition 단위로 순차적으로 저장
- Consumer가 읽어도 메시지는 삭제되지 않음
- 카프카 서버에 설정한 보관 기간 만큼 메시지 영구 저장
    - `log.retention.hours` : 시간 단위
    - `log.retention.minutes` : 분 단위
    - `log.retention.ms` : 밀리초 단위

> [Configure Brokers and Controllers](https://docs.confluent.io/platform/current/installation/configuration/broker-configs.html)

- 메시지가 보존되어 있지만 Consumer가 읽은 offset 이후의 메시지만 읽음
- offset이 가능한 이유는 Consumer가 메시지를 읽은후, 자신이 읽은 offset을 카프카 서버에 저장하기 때문

```java
public void consume(Message message, Acknowledgment ack) {

    // 1. 메시지 처리
    process(message);

    // 2. ack.acknowledge() 호출 시 해당 메시지가 처리되었다는 것을 알림
    ack.acknowledge();
}

``` 

- __consumer_offsets 에서 마지막 읽은 offset 정보 조회
- 해당 offset + 1 부터 메시지 읽기 시작
- Consumer가 메시지 소비
- 메시지 처리를 한 뒤에 ack.acknowledge() 호출
- __consumer_offsets에 Consumer가 읽은 offset 정보 저장

```
ls -lh /var/lib/kafka/data/

__consumer_offsets_xx

kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group order-consumer-group \
  --describe

```

- __consumer_offsets

### 토픽

### 브로커

### 프로듀서

### 컨슈머

- 파티션은 Topic의 물리적 구조 (모든 Consumer Group이 공유), 파티션 = 최대 병렬 처리 단위
- Offset은 Consumer Group 별로 offset을 독립적으로 관리
    - 이로 인해서 Broadcasting이 가능
    - 예: Order Topic을 Payment Service와 Inventory Service가 각각 구독하는 경우, 각각의 Consumer Group이 독립적으로 offset을 관리하여 같은 메시지를 각각
      처리 가능
      ```java
      @KafkaListener(topics = "order-events", groupId = "payment-service")
      public void paymentProcess(Event event) {
          // 결제 처리 로직
      }
      @KafkaListener(topics = "order-events", groupId = "inventory-service")
      public void decreaseStock(Event event) {
          // 재고 처리 로직
      }
      ```
- Consumer Group 내부에서 파티션을 분산 (Rebalancing)
    - 하나의 Consumer Group 내에서 여러 Consumer가 있을 때, 각 파티션을 여러 Consumer에 분배하여 병렬 처리
    -


- Partition 내에서만 순서를 보장
- Key 기반 파티셔닝: 같은 Key는 같은 Partition에 저장
- Consumer Group 내 각 Partition은 하나의 Consumer에 할당

> https://shinwusub.tistory.com/133
> https://docs.confluent.io/platform/current/installation/configuration/index.html
https://docs.confluent.io/platform/current/get-started/platform-quickstart.html
[Github > Kafka Confluent Docker](https://github.com/confluentinc/cp-all-in-one)

### EDA

#### 전통적인 동기식 호출 방식의 문제점

```java

@Service
@Transactional
public class OrderService {
    private final OrderRepository orderRepository;
    private final PaymentClient paymentClient;
    private final InventoryClient inventoryClient;
    private final NotificationClient notificationClient;

    public Order createOrder(OrderCreatingRequest request) {
        // 1. 주문 생성
        OrderJpaEntity order = orderRepository.save(new OrderJpaEntity(request));

        // 2. 결제 처리 (동기 호출)
        paymentClient.processPayment(order);

        // 3. 재고 차감 (동기 호출)
        inventoryClient.decreaseStock(order);

        // 4. 알림 전송 (동기 호출)
        notificationClient.sendNotification(order);

        return order;
    }
}
```

- 하나의 트랜잭션에서 모든 내용을 처리하게 되는 경우 (예: 주문 생성 -> 결제 처리 -> 재고 차감 -> 알림 전송)
    - 모든 서비스가 강하게 결합됨 (예: 결제, 재고, 알림 API의 수정은 곧 주문 서비스의 수정으로 이어짐)
    - 한 서비스 장애가 전체 실패로 이어짐(예: 알림 서비스 장애가 주문 생성 실패로 이루어짐)
    - 모든 서비스의 응답을 기다려야하므로 응답 시간이 느려짐

#### EDA 방식의 해결

```java

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    public Order createOrder(OrderCreatingRequest request) {
        // 1. 주문 생성
        OrderJpaEntity order = orderRepository.save(new OrderJpaEntity(request));

        // 2. 이벤트 발행(비동기 처리)
        eventPublisher.publishEvent(new OrderCreatedEvent(order));

        // 주문 생성 이후 즉시 반환 가능
        return order;
    }
}

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventHandler {

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("트랜잭션 커밋 후 이벤트 처리 시작");

        kafkaTemplate.send("order-events", event.getOrderId(), event)
                .whenComplete((result, ex) -> {

                });
    }
}

// Payment Service (독립적으로 실행)
@KafkaListener(topics = "order-events")
public void handleOrderCreated(OrderCreatedEvent event) {
    processPayment(event);
}

// Inventory Service (독립적으로 실행)
@KafkaListener(topics = "order-events")
public void handleOrderCreated(OrderCreatedEvent event) {
    decreaseStock(event);
}

// Notification Service (독립적으로 실행)
@KafkaListener(topics = "order-events")
public void handleOrderCreated(OrderCreatedEvent event) {
    sendNotification(event);
}
```

- 하나의 트랜잭션에서 모든 내용을 처리하지 않고 이벤트 발행
    - 느슨한 결합: 주문 서비스는 이벤트 발행만 담당, 결제/재고/알림 서비스는 독립적으로 동작하며 결제/재고/알람의 코드 수정이 주문 서비스로 이어지지 않음
    - 장애 격리: 결제/재고/알림 서비스 장애가 주문 생성 실패로 이어지지 않음
    - 빠른 응답 시간: 주문 생성 후 즉시 반환 가능, 결제/재고/알림 처리는 비동기적으로 처리

#### Transaction Outbox

- 메시지 서비스의 장애로 인해서 메시지 발행이 불가능한 경우 or 애플리케이션 장애로 인해서 메시지 발행이 불가능한 경우
    - 트랜잭션 커밋 -> 네티워크 장애 or 메시지 서비스 장애(Kafka 다운) -> 이벤트 전송 불가
    - 트랜잭션 커밋 -> 애플리케이션 장애 -> 이벤트 전송 불가
- 위와 같은 문제를 해결하기 위해서 Transaction Outbox 패턴 사용
    - DB 저장과 이벤트 저장을 같은 트랜잭션에 포함
    - 별도의 프로세스가 Outbox 테이블을 주기적으로 확인하여 이벤트 전송
    - 이벤트 전송이 실패한 경우 재시도 로직 구현 가능

