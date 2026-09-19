package co.wm.warehouse.warehouse;

public class DuplicateWarehouseNameException extends RuntimeException {

    public DuplicateWarehouseNameException(String name) {
        super("A warehouse with name '" + name + "' already exists");
    }
}
