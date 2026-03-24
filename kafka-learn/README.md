
```
Phase 1 — 환경 + 기본 구조 (1장~3장)
Docker로 Kafka 클러스터 구성
토픽·파티션·리플리케이션 직접 만들어보며 구조 체감
→ 지금 여기부터 시작하면 됩니다

Phase 2 — 프로듀서/컨슈머 깊이 (4장~6장)
acks, retries, idempotent producer
컨슈머 그룹, 오프셋 커밋 전략, 리밸런싱
→ 대부분의 실전 버그가 여기서 발생

Phase 3 — 운영 (7장~9장)
모니터링, 클러스터 확장, 보안
→ 개발자도 알아야 장애 대응 가능

Phase 4 — 에코시스템 (10장~12장)
카프카 커넥트, 스트림즈, 스키마 레지스트리
```

```
docker-compose.yml
  ├── ZooKeeper × 1
  ├── Kafka Broker × 3  (리플리케이션을 체감하려면 최소 3대)
```


### 
- Kafka는 이벤트를 디스크에 영속적으로 저장하고, 컨슈머가 읽은 메시지의 위치(offset)를 관리하는 분산 이벤트 스트리밍 플랫폼(distributed event streaming platform)입니다.
- 

전통 MQ: 메시지를 읽으면 큐에서 삭제됨 (Pop)
Kafka:   메시지를 읽어도 삭제 안 함. "어디까지 읽었는지(offset)"만 기록





1. Serializer: 객체 → 바이트 배열 (JSON, Avro 등)                                                                                                      2. Partitioner: 어떤 파티션으로 보낼지 결정
    - key가 있으면: hash(key) % 파티션수 → 같은 key = 항상 같은 파티션 → 순서 보장                                                                         - key가 없으면: Round-Robin 또는 Sticky Partitioner
3. RecordAccumulator: 같은 파티션 행 메시지를 배치로 모음                                                                                              4. Sender: 배치가 차거나 linger.ms가 지나면 브로커로 전송
5. 브로커: 리더 파티션에 쓰기 → 팔로워에 복제 → acks 설정에 따라 응답

docker exec kafka-1 kafka-broker-api-versions --bootstrap-server localhost:9092 2>&1 | head -5