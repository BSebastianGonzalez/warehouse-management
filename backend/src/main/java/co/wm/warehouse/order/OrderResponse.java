package co.wm.warehouse.order;

import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        Instant createdAt,
        OrderStatus status,
        Long administratorId,
        String administratorUsername,
        List<OrderLineResponse> lines) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCreatedAt(),
                order.getStatus(),
                order.getAdministratorId(),
                order.getAdministratorUsername(),
                order.getLines().stream().map(OrderLineResponse::from).toList());
    }
}
