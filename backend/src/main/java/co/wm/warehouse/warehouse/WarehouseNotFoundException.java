package co.wm.warehouse.warehouse;

public class WarehouseNotFoundException extends RuntimeException {

    public WarehouseNotFoundException(Long id) {
        super("Warehouse with id " + id + " was not found");
    }
}
