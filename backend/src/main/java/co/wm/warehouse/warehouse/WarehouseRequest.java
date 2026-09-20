package co.wm.warehouse.warehouse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WarehouseRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 200) String location) {
}
