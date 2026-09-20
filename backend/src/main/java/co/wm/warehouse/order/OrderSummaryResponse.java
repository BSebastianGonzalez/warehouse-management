package co.wm.warehouse.order;

import java.time.Instant;

public record OrderSummaryResponse(
        Long id,
        Instant createdAt,
        OrderStatus status,
        Long administratorId,
        String administratorUsername,
        int lineCount,
        int requestedQuantityTotal,
        int missingQuantityTotal) {

    public static OrderSummaryResponse from(Order order) {
        var lines = order.getLines();
        return new OrderSummaryResponse(
                order.getId(),
                order.getCreatedAt(),
                order.getStatus(),
                order.getAdministratorId(),
                order.getAdministratorUsername(),
                lines.size(),
                lines.stream().mapToInt(OrderLine::getRequestedQuantity).sum(),
                lines.stream().mapToInt(OrderLine::getMissingQuantity).sum());
    }
}
