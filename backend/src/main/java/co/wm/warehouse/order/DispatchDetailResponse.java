package co.wm.warehouse.order;

public record DispatchDetailResponse(Long warehouseId, Integer dispatchedQuantity) {

    public static DispatchDetailResponse from(DispatchDetail detail) {
        return new DispatchDetailResponse(detail.getWarehouseId(), detail.getDispatchedQuantity());
    }
}
