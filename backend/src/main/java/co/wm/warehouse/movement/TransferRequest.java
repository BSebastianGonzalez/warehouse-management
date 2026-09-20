package co.wm.warehouse.movement;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TransferRequest(
        @NotNull Long productId,
        @NotNull Long sourceWarehouseId,
        @NotNull Long destinationWarehouseId,
        @NotNull @Min(1) Integer quantity,
        @Size(max = 255) String reference) {
}
