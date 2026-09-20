package co.wm.warehouse.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderLineRequest(
        @NotNull Long productId,
        @NotNull @Min(1) Integer requestedQuantity) {
}
