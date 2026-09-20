package co.wm.warehouse.order;

import java.util.List;

public record OrderLineResponse(
        Long id,
        Long productId,
        Integer requestedQuantity,
        Integer missingQuantity,
        List<DispatchDetailResponse> dispatchDetails) {

    public static OrderLineResponse from(OrderLine line) {
        return new OrderLineResponse(
                line.getId(),
                line.getProductId(),
                line.getRequestedQuantity(),
                line.getMissingQuantity(),
                line.getDispatchDetails().stream().map(DispatchDetailResponse::from).toList());
    }
}
