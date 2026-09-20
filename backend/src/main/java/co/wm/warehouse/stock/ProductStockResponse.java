package co.wm.warehouse.stock;

import java.util.List;

public record ProductStockResponse(
        Long productId,
        String sku,
        String productName,
        Integer minimumStock,
        Integer totalQuantity,
        boolean belowMinimum,
        List<WarehouseStockResponse> warehouses) {
}
