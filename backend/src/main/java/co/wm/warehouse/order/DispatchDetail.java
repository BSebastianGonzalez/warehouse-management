package co.wm.warehouse.order;

import co.wm.warehouse.warehouse.Warehouse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "dispatch_details")
public class DispatchDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_line_id", nullable = false)
    private OrderLine orderLine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(nullable = false)
    private Integer dispatchedQuantity;

    protected DispatchDetail() {
    }

    public DispatchDetail(Warehouse warehouse, Integer dispatchedQuantity) {
        if (warehouse == null || dispatchedQuantity == null || dispatchedQuantity <= 0) {
            throw new IllegalArgumentException("Dispatch warehouse and positive quantity are required");
        }
        this.warehouse = warehouse;
        this.dispatchedQuantity = dispatchedQuantity;
    }

    void assignOrderLine(OrderLine orderLine) {
        this.orderLine = orderLine;
    }

    public Long getWarehouseId() { return warehouse.getId(); }
    public Integer getDispatchedQuantity() { return dispatchedQuantity; }
}
