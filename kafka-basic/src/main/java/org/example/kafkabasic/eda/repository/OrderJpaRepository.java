package org.example.kafkabasic.eda.repository;

import org.example.kafkabasic.eda.domain.OrderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, String> {
}
