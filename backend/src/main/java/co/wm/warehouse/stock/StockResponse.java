package co.wm.warehouse.stock;

public record StockResponse(Long productId, Long warehouseId, Integer quantity) {

    public static StockResponse from(Stock stock) {
        return new StockResponse(stock.getProductId(), stock.getWarehouseId(), stock.getQuantity());
    }
}
