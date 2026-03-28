## Kafka

Apache Kafka는 **분산 커밋 로그(Distributed Commit Log)** 시스템이다.
단순한 "메시지 큐"가 아니라, 이벤트를 디스크에 영속적으로 저장하고 여러 앱이 독립적인 속도로 소비할 수 있는 플랫폼이다.

### Spring 개발자를 위한 비유

- `ApplicationEventPublisher`로 이벤트를 발행하는 것과 비슷하지만, **프로세스 경계를 넘어서** 동작한다.
- Spring 이벤트는 같은 JVM 내에서만 동작하고, 앱이 죽으면 이벤트도 사라진다.
- Kafka는 이벤트를 디스크에 영속 저장하고, 여러 앱이 독립적으로 소비할 수 있다.

---

## 왜 Kafka가 필요한가?

### Kafka가 없던 시절의 고통

```
[주문 서비스] → 주문 완료 시:
  1. 결제 서비스에 HTTP 호출 → 결제 처리
  2. 재고 서비스에 HTTP 호출 → 재고 차감
  3. 알림 서비스에 HTTP 호출 → 사용자 알림
  4. 정산 서비스에 HTTP 호출 → 매출 기록
```

**문제 1: 강한 결합 (Tight Coupling)**
- 주문 서비스가 4개 서비스의 API 스펙을 전부 알아야 한다.
- 정산 서비스가 추가되면 주문 서비스 코드를 수정해야 한다.

**문제 2: 동기 호출의 연쇄 장애**
- 알림 서비스가 3초 지연되면 주문 API 응답도 3초 느려진다.
- 알림 서비스가 죽으면 주문 자체가 실패한다.

**문제 3: 트래픽 폭주 대응 불가**
- 주문이 10배 들어오면, 4개 서비스 모두 동시에 10배 트래픽을 감당해야 한다.

### Kafka로 해결

```
[주문 서비스] → "주문 완료" 이벤트를 Kafka 토픽에 발행 → 끝!

[결제 서비스]  ← 자기 속도로 소비
[재고 서비스]  ← 자기 속도로 소비
[알림 서비스]  ← 자기 속도로 소비 (3초 지연? 주문에 영향 없음)
[정산 서비스]  ← 나중에 추가해도 주문 서비스 코드 변경 없음
```

---

## Kafka가 빠른 진짜 이유

### 1. 순차 쓰기 (Sequential Write)
메시지를 파일 끝에 append만 한다. HDD에서도 순차 쓰기는 SSD 랜덤 I/O보다 빠르다.

### 2. Zero Copy
```
일반 메시지 시스템:
  디스크 → 커널 버퍼 → [유저 공간 복사] → 커널 버퍼 → NIC (4번 복사)

Kafka (sendfile 시스콜):
  디스크 → 커널 버퍼 → NIC (2번 복사)
```

### 3. 배치 처리
메시지를 하나씩 보내지 않고 묶어서 보낸다. 네트워크 왕복 횟수가 줄어든다.

---

## 전통 MQ(RabbitMQ)와 근본적 차이

| | 전통 MQ (RabbitMQ) | Kafka |
|---|---|---|
| **메시지 읽기** | 읽으면 큐에서 삭제 (Pop) | 읽어도 삭제 안 함. offset만 기록 |
| **재처리** | 불가능 (이미 삭제됨) | offset을 되감으면 가능 |
| **다중 소비자** | 같은 메시지를 여러 서비스가 읽으려면 별도 큐 필요 | Consumer Group별로 독립 소비 |
| **라우팅** | Exchange/Routing Key로 유연한 라우팅 | 토픽 기반 단순 라우팅 |

### 오프셋(Offset)의 핵심

```
파티션 0: [0] [1] [2] [3] [4] [5] [6] [7] ...
                        ↑
                  Consumer A의 현재 offset = 3

          [0] [1] [2] [3] [4] [5] [6] [7] ...
                                    ↑
                              Consumer B의 현재 offset = 5
```

- 같은 데이터를 여러 서비스가 독립적으로 소비 가능
- 재처리가 가능 — offset을 되감으면 과거 데이터를 다시 읽을 수 있음
- 새 서비스 추가 시 처음부터(earliest) 읽어서 과거 데이터도 소비 가능

---

## 로그 보관 정책 (Retention Policy)

메시지를 삭제하지 않으면 디스크가 무한히 차므로, 보관 정책으로 관리한다.

### 보관 정책 2가지

| 정책 | 설정 | 동작 |
|---|---|---|
| 시간 기반 | `log.retention.hours=168` (7일) | 7일 지난 세그먼트를 통째로 삭제 |
| 크기 기반 | `log.retention.bytes=1073741824` (1GB) | 파티션 크기가 1GB 넘으면 오래된 세그먼트부터 삭제 |

### Log Compaction

```
Compaction 전:
  [key=A, value=1] [key=B, value=2] [key=A, value=3] [key=B, value=4]

Compaction 후:
  [key=A, value=3] [key=B, value=4]   ← 각 key의 최신 값만 유지
```

CDC(Debezium)에서 주로 사용. 테이블의 "현재 상태"만 유지하고 싶을 때 유용하다.

### 재처리의 제약

- 보관 기간이 지난 이벤트는 재처리 불가
- retention이 너무 짧으면 → 장애 복구 시 재처리 불가
- retention이 너무 길면 → 디스크 비용 증가

---

## Kafka를 쓰면 좋은 상황 vs 안 좋은 상황

### 빛나는 상황
- 대용량 이벤트 스트리밍 (로그, 클릭 이벤트, IoT 센서 데이터)
- 여러 서비스가 같은 이벤트를 독립적으로 소비해야 할 때
- 이벤트 재처리가 필요할 때
- 이벤트 소싱, CDC, CQRS 패턴

### 쓰지 말아야 할 상황

| 상황 | 이유 | 대안 |
|---|---|---|
| 단순 작업 큐 (이메일 발송) | Kafka는 과도한 인프라 | RabbitMQ, SQS |
| 실시간 요청-응답 | Kafka는 비동기 설계 | REST, gRPC |
| 소규모 시스템 (서비스 2~3개) | 복잡도 대비 이점 적음 | Spring Event, Redis Pub/Sub |
| 메시지 라우팅이 복잡할 때 | Kafka는 라우팅 기능 약함 | RabbitMQ |

---

## 재처리와 멱등성

재처리의 주체는 **컨슈머**이다. Kafka가 알아서 다시 보내주는 게 아니라, 컨슈머가 offset을 되감아서 다시 읽는 것이다.

```bash
# offset을 특정 시점으로 리셋
kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group payment-service \
  --topic order-events \
  --reset-offsets \
  --to-datetime 2026-03-24T20:00:00.000 \
  --execute
```

### 멱등성이 반드시 필요하다

```
❌ 멱등하지 않은 컨슈머:
   주문 이벤트 → 잔액에서 10,000원 차감
   재처리 시 → 또 10,000원 차감 → 이중 결제!

✅ 멱등한 컨슈머:
   주문 이벤트 → "이 orderId로 이미 결제했나?" 확인 후 처리
   재처리 시 → 이미 처리됨 → 스킵
```



### Zookeeper
- Kafka의 정상 동작과 클러스터 관리를 담당하는 분산 코디네이터
  - MongoDB로 치면 mo
- 브로커 간 리더 선출, 토픽 메타데이터 관리, 컨슈머 그룹 관리 등 핵심 기능 수행
    - 브로커 간 리더 선출: 장애 발생 시 자동으로 새로운 리더를 선출하여 고가용성 보장
    - 토픽 메타데이터 관리: 토픽과 파티션 정보, 브로커 상태 등을 중앙에서 관리
    - 컨슈머 그룹 관리: 컨슈머 그룹의 오프셋 관리, 리밸런싱 조정 등 컨슈머 그룹 관련 작업 수행
- Kafka 2.8.0부터는 Zookeeper 없이도 운영 가능한 KRaft 모드(KIP-500)가 도입되었지만, 아직은 Zookeeper 기반 클러스터가 일반적이다.



### Kafka 간단하게 사용해보기

#### 1. 토픽 생성 

```shell
docker exec kafka-1 kafka-topics --create \
  --bootstrap-server kafka-1:29092 \
  --topic test-topic \
  --partitions 3 \
  --replication-factor 3
```

#### 2. 토픽 조회

```shell
## 토픽 목록 조회
docker exec kafka-1 kafka-topics --list \
    --bootstrap-server kafka-1:29092
    
## 토픽 상세 조회
docker exec kafka-1 kafka-topics --describe \
  --bootstrap-server kafka-1:29092 \
  --topic test-topic
```

#### 3. 프로듀서로 메시지 전송

```shell
docker exec kafka-1 kafka-console-producer \
  --bootstrap-server kafka-1:29092 \
  --topic test-topic
```

#### 4. 컨슈머로 메시지 소비

```shell
docker exec kafka-1 kafka-console-consumer \
  --bootstrap-server kafka-1:29092 \
  --topic test-topic \
  --from-beginning
```