package co.wm.warehouse.stock;

public record StockReconciliationResponse(
        Long productId,
        Long warehouseId,
        Integer projectedQuantity,
        Integer reconstructedQuantity,
        boolean consistent) {
}
