# 06. 주키퍼(ZooKeeper)와 KRaft

> Kafka가 왜 ZooKeeper에 의존했고, 왜 걷어냈는지, 그리고 KRaft가 어떻게 대체하는지를 다룬다.

---

## ZooKeeper가 Kafka에서 맡았던 역할

### Spring 비유

```
Spring Cloud:  Eureka 서버가 서비스 목록을 관리하고, 누가 살아있는지 모니터링
Kafka:         ZooKeeper가 브로커 목록을 관리하고, 누가 리더인지 모니터링
```

차이점: Eureka는 서비스 디스커버리만 하지만, ZooKeeper는 **리더 선출, 설정 저장, 분산 락**까지 담당했다.
Kafka의 "뇌" 역할이었던 셈이다.

### ZooKeeper의 5가지 역할

```
┌─────────────────────────────────────────────────────┐
│                    ZooKeeper의 역할                    │
├─────────────────────────────────────────────────────┤
│                                                      │
│  1. 브로커 등록/관리                                    │
│     "kafka-1, kafka-2, kafka-3이 살아있다"              │
│                                                      │
│  2. 컨트롤러 선출                                      │
│     브로커 중 하나를 "컨트롤러"로 선출                     │
│     → 컨트롤러가 파티션 리더 할당, 리밸런싱 담당            │
│                                                      │
│  3. 토픽/파티션 메타데이터 저장                           │
│     "test-topic은 파티션 3개, 리플리카 3개"               │
│     "파티션 0의 리더는 broker-1"                         │
│                                                      │
│  4. ACL(접근 제어) 저장                                 │
│     "이 사용자는 이 토픽에 읽기만 가능"                    │
│                                                      │
│  5. 컨슈머 그룹 오프셋 (구버전 — Kafka 0.9 이전)          │
│     → 현재는 __consumer_offsets 토픽으로 이동             │
└─────────────────────────────────────────────────────┘
```

### docker-compose.yml에서 보는 ZooKeeper 의존 구조

```yaml
# ZooKeeper가 먼저 뜨고
zookeeper:
  image: confluentinc/cp-zookeeper:7.5.0
  environment:
    ZOOKEEPER_CLIENT_PORT: 2181       # 브로커가 여기로 접속

# 각 브로커가 ZooKeeper에 연결
kafka-1:
  environment:
    KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181   # ← 이 설정이 핵심
  depends_on:
    - zookeeper                               # ← ZK 없으면 브로커 시작 불가
```

---

## ZooKeeper 모드의 내부 동작

### 브로커 시작 시

```
kafka-1 ──── "나 broker.id=1이야" ────→ ZooKeeper
                                          │
                                          ▼
                                    /brokers/ids/1 (임시 노드 생성)
                                    /brokers/ids/2
                                    /brokers/ids/3
```

ZooKeeper의 **임시 노드(Ephemeral Node)** 특성을 활용한다:
- 브로커가 ZK에 연결하면 임시 노드가 생성됨
- 브로커가 죽으면 ZK 세션이 끊기고, 임시 노드가 자동 삭제됨
- → 다른 브로커(컨트롤러)가 이 변화를 감지하고 리밸런싱 시작

### 컨트롤러 선출

```
컨트롤러 선출:
                                    /controller (임시 노드)
kafka-1 ──── "내가 컨트롤러 할게" ──→ ZooKeeper에 먼저 등록한 놈이 컨트롤러
kafka-2 ──── "나도!" ──→ 늦었음, 실패
kafka-3 ──── "나도!" ──→ 늦었음, 실패
```

- ZooKeeper의 `/controller` 경로에 **먼저 임시 노드를 생성한 브로커**가 컨트롤러가 됨
- 나머지 브로커들은 이 노드에 **watch**를 걸어서 컨트롤러가 죽으면 바로 재선출

### 메타데이터 전파 방식

```
컨트롤러(kafka-1)가 하는 일:
  - 파티션 리더 할당: "topic-A partition-0의 리더는 broker-2"
  - 브로커가 죽으면 리더 재선출
  - 이 결정들을 ZooKeeper에 기록 + 다른 브로커에게 전파

┌──────────┐    메타데이터 변경     ┌──────────┐
│Controller│ ──────────────────→  │ZooKeeper │ (저장)
│(broker-1)│                      └──────────┘
│          │ ──── UpdateMetadata ──→ broker-2  (전파)
│          │ ──── UpdateMetadata ──→ broker-3  (전파)
└──────────┘
```

**문제**: 메타데이터가 변경될 때마다 컨트롤러가 ZooKeeper에 쓰고, 모든 브로커에게 하나씩 전파해야 한다.
브로커가 100대면? 파티션이 10만개면? 이 과정이 엄청나게 느려진다.

---

## ZooKeeper가 제거된 이유 — 실전 고통

### 고통 1: 이중 시스템 운영

```
Kafka 클러스터를 운영하려면:

  ZooKeeper 3대 (앙상블)  +  Kafka 브로커 N대

  → 모니터링 대상이 2배
  → 장애 포인트가 2배
  → 설정 관리가 2배
  → 보안 설정(인증/인가)도 각각 따로
```

### 고통 2: 메타데이터 병목

- 파티션 수가 많아질수록 컨트롤러 장애 복구 시간 증가
- ZooKeeper에서 전체 메타데이터를 읽어와야 하므로
- 파티션 100만개 → 컨트롤러 페일오버에 **수 분** 소요

### 고통 3: 스케일 한계

- ZooKeeper는 쓰기가 느림 (모든 노드에 동기 복제)
- Kafka 파티션 수가 늘어날수록 ZK가 병목이 됨
- 실무에서 **"파티션 20만개 넘으면 불안정"** 이라는 경험칙이 존재

---

## KRaft — ZooKeeper 없는 Kafka

### KRaft = Kafka + Raft

Raft 합의 알고리즘을 Kafka 자체에 내장하여 ZooKeeper 의존성을 완전히 제거한 모드.

### 아키텍처 비교

```
ZooKeeper 모드 (기존):                    KRaft 모드 (현재):

┌──────────┐   ┌──────────┐              ┌──────────────────────┐
│ Broker 1 │   │ZooKeeper │              │ Controller 1 (voter) │
│ Broker 2 │───│ 앙상블    │              │ Controller 2 (voter) │
│ Broker 3 │   │ (별도)   │              │ Controller 3 (voter) │
└──────────┘   └──────────┘              │ Broker 1             │
                                         │ Broker 2             │
  2개 시스템, 2개 배포,                    │ Broker 3             │
  2개 모니터링                             └──────────────────────┘

                                           1개 시스템으로 통합!
```

소규모 클러스터에서는 **하나의 노드가 Controller + Broker 역할을 동시에** 수행할 수도 있다.
대규모 클러스터에서는 Controller 전용 노드를 분리하여 메타데이터 처리 성능을 확보한다.

### 핵심 발상: __cluster_metadata 토픽

```
┌───────────────────────────────────────────────────────────────┐
│  __cluster_metadata 토픽 (내부 토픽)                            │
│                                                                │
│  메타데이터를 Kafka 토픽 자체에 저장!                              │
│  → Kafka가 이미 잘하는 것 = 로그를 복제하는 것                     │
│  → 메타데이터도 똑같이 로그로 복제하면 되지 않나? ← 핵심 발상        │
│                                                                │
│  Controller 노드들이 Raft 합의로 이 토픽을 관리                    │
│  - 리더 컨트롤러: 메타데이터 변경을 로그에 기록                     │
│  - 팔로워 컨트롤러: 로그를 복제하고 따라감                          │
│  - 브로커: 이 로그를 읽어서 자기 메타데이터를 업데이트               │
└───────────────────────────────────────────────────────────────┘
```

### 메타데이터 전파 방식의 변화

```
ZooKeeper 모드:
  컨트롤러 → ZK에 쓰기 → 브로커 100대에 하나씩 RPC 전파
  (O(N) 전파, 느림)

KRaft 모드:
  컨트롤러 → __cluster_metadata 토픽에 쓰기
  브로커들 → 각자 이 토픽을 읽어서 자기 상태 업데이트
  (이벤트 소싱 패턴!)
```

Spring 비유: `ApplicationEventMulticaster`가 모든 리스너에게 동기적으로 이벤트를 보내던 것을,
Kafka 토픽을 통한 비동기 이벤트 소싱으로 바꾼 셈이다.

### Raft 합의 알고리즘 — 컨트롤러 간 동작

```
3대의 Controller가 Raft 쿼럼을 구성:

  Controller-1 (리더)     "메타데이터 변경을 내가 기록"
  Controller-2 (팔로워)    "리더의 로그를 복제"
  Controller-3 (팔로워)    "리더의 로그를 복제"

리더 선출:
  1. 리더가 죽으면 팔로워들이 투표 (term 번호 증가)
  2. 과반수 득표한 팔로워가 새 리더
  3. 새 리더가 메타데이터 변경을 이어서 처리

커밋 규칙:
  메타데이터 변경은 과반수(쿼럼)가 복제해야 커밋됨
  → 3대 중 2대 이상 동의해야 변경 확정
```

### __cluster_metadata에 저장되는 것들

| ZooKeeper에 있던 것 | KRaft에서는 |
|---|---|
| /brokers/ids/* (브로커 목록) | BrokerRegistration 레코드 |
| /brokers/topics/* (토픽 메타데이터) | TopicRecord, PartitionRecord |
| /controller (컨트롤러 정보) | Raft 리더 선출로 대체 |
| /config/* (동적 설정) | ConfigRecord |
| /admin/delete_topics (토픽 삭제) | RemoveTopicRecord |
| /isr_change_notification | 불필요 — 브로커가 직접 컨트롤러에 보고 |

---

## ZooKeeper 제거 타임라인

```
2019         KIP-500 제안 — "ZooKeeper를 제거하자"
  │
2022 (3.3)   KRaft 프로덕션 준비 완료 선언
  │
2024.11 (3.9) 마지막 ZooKeeper 지원 버전
  │            "ZK 모드 브로커 시작 시 deprecation 경고 출력"
  │
2025.03 (4.0) ★ ZooKeeper 모드 완전 제거 ★
  │            KRaft만 지원, 구버전 메시지 포맷(v0/v1)도 제거
  │
2026.현재 (4.2) 최신 안정 버전, KRaft 전용
```

현재 프로젝트의 `cp-kafka:7.5.0`은 Kafka 3.5 기반이므로 아직 ZooKeeper 모드가 동작한다.
하지만 신규 클러스터는 KRaft가 표준이며, 기존 클러스터도 마이그레이션이 필요하다.

---

## ZooKeeper vs KRaft 비교

| | ZooKeeper 모드 | KRaft 모드 |
|---|---|---|
| 운영 복잡도 | 높음 (2시스템 관리) | 낮음 (1시스템) |
| 파티션 스케일 | ~20만개 경험적 한계 | 수백만개 가능 |
| 컨트롤러 페일오버 | 수 초~수 분 | 수 초 이내 |
| 메타데이터 전파 | 컨트롤러 → 브로커 RPC (O(N)) | 토픽 기반 이벤트 소싱 |
| 생태계 성숙도 | 10년+ 운영 경험 축적 | 프로덕션 3~4년차 |
| 마이그레이션 | - | 기존 ZK 클러스터 전환 시 주의 필요 |
| 학습 가치 | ZK 자체는 범용 분산 코디네이터 | Kafka 전용 |

---

## 참고 자료

- [Apache Kafka 4.0 Release Announcement](https://kafka.staged.apache.org/blog/2025/03/18/apache-kafka-4.0.0-release-announcement/)
- [Kafka 4.0: KRaft Simplifies Architecture - InfoQ](https://www.infoq.com/news/2025/04/kafka-4-kraft-architecture/)
- [A deep dive into Apache Kafka's KRaft protocol - Red Hat](https://developers.redhat.com/articles/2025/09/17/deep-dive-apache-kafkas-kraft-protocol)
- [Amazon MSK에서 KRaft 모드 사용하기 - AWS 기술 블로그](https://aws.amazon.com/ko/blogs/tech/amazon-msk-kraft-mode/)
- [ZooKeeper and Apache Kafka: From Legacy to KRaft - Confluent](https://www.confluent.io/learn/zookeeper-kafka/)

