package co.wm.warehouse.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductRequest(
        @NotBlank @Size(max = 100) String sku,
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 50) String unitOfMeasure,
        @NotNull @Min(0) Integer minimumStock,
        Boolean discontinued) {
}
