package co.wm.warehouse.movement;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(Long productId, Long warehouseId) {
        super("Insufficient stock for product " + productId + " in warehouse " + warehouseId);
    }
}
