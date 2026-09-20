package co.wm.warehouse.order;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record OrderRequest(@NotEmpty List<@Valid OrderLineRequest> lines) {
}
