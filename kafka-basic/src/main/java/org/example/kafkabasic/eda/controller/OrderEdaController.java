package org.example.kafkabasic.eda.controller;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.example.kafkabasic.eda.domain.OrderItemJpaEntity;
import org.example.kafkabasic.eda.domain.OrderJpaEntity;
import org.example.kafkabasic.eda.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/eda/orders")
@RequiredArgsConstructor
public class OrderEdaController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderJpaEntity> createOrder(
            @RequestBody CreateOrderRequest request) {

        List<OrderItemJpaEntity> items = request.getItems().stream()
                .map(dto -> new OrderItemJpaEntity(
                        dto.getProductId(),
                        dto.getProductName(),
                        dto.getQuantity(),
                        dto.getPrice()
                ))
                .toList();

        OrderJpaEntity order = orderService.createOrder(request.getUserId(), items);

        return ResponseEntity.ok(order);
    }

    @Getter
    @Setter
    @ToString
    static class CreateOrderRequest {
        private String userId;
        private List<OrderItemRequest> items;
    }

    @Getter
    @Setter
    @ToString
    static class OrderItemRequest {
        private String productId;
        private String productName;
        private Integer quantity;
        private BigDecimal price;
    }

}
