package co.wm.warehouse.movement;

import java.time.Instant;

public record MovementReadResponse(
        Long id,
        MovementType type,
        Long productId,
        Long sourceWarehouseId,
        Long destinationWarehouseId,
        Integer quantity,
        Instant createdAt,
        Long administratorId,
        String administratorUsername) {

    public static MovementReadResponse from(Movement movement) {
        return new MovementReadResponse(
                movement.getId(),
                movement.getType(),
                movement.getProductId(),
                movement.getSourceWarehouseId(),
                movement.getDestinationWarehouseId(),
                movement.getQuantity(),
                movement.getCreatedAt(),
                movement.getAdministratorId(),
                movement.getAdministratorUsername());
    }
}
