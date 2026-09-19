package co.wm.warehouse.warehouse;

public record WarehouseResponse(Long id, String name, String location) {

    public static WarehouseResponse from(Warehouse warehouse) {
        return new WarehouseResponse(warehouse.getId(), warehouse.getName(), warehouse.getLocation());
    }
}
