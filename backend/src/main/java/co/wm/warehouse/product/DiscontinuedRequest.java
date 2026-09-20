package co.wm.warehouse.product;

import jakarta.validation.constraints.NotNull;

public record DiscontinuedRequest(@NotNull Boolean discontinued) {
}
