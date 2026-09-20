package co.wm.warehouse.movement;

import java.time.Instant;

public record MovementResponse(
        Long id,
        MovementType type,
        Long productId,
        Long sourceWarehouseId,
        Long destinationWarehouseId,
        Integer quantity,
        Instant createdAt,
        String reference) {

    public static MovementResponse from(Movement movement) {
        return new MovementResponse(
                movement.getId(),
                movement.getType(),
                movement.getProductId(),
                movement.getSourceWarehouseId(),
                movement.getDestinationWarehouseId(),
                movement.getQuantity(),
                movement.getCreatedAt(),
                movement.getReference());
    }
}
