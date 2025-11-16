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

- Partition 내에서만 순서를 보장
- Key 기반 파티셔닝: 같은 Key는 같은 Partition에 저장
- Consumer Group 내 각 Partition은 하나의 Consumer에 할당

> > https://shinwusub.tistory.com/133
> https://docs.confluent.io/platform/current/installation/configuration/index.html
https://docs.confluent.io/platform/current/get-started/platform-quickstart.html
[Github > Kafka Confluent Docker](https://github.com/confluentinc/cp-all-in-one)