package co.wm.warehouse.order;

import java.util.ArrayList;
import java.util.List;

import co.wm.warehouse.product.Product;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_lines")
public class OrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer requestedQuantity;

    @Column(nullable = false)
    private Integer missingQuantity;

    @OneToMany(mappedBy = "orderLine", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<DispatchDetail> dispatchDetails = new ArrayList<>();

    protected OrderLine() {
    }

    public OrderLine(Product product, Integer requestedQuantity, Integer missingQuantity) {
        if (product == null || requestedQuantity == null || requestedQuantity <= 0
                || missingQuantity == null || missingQuantity < 0 || missingQuantity > requestedQuantity) {
            throw new IllegalArgumentException("Order line quantities and product are invalid");
        }
        this.product = product;
        this.requestedQuantity = requestedQuantity;
        this.missingQuantity = missingQuantity;
    }

    void assignOrder(Order order) {
        this.order = order;
    }

    public void addDispatchDetail(DispatchDetail detail) {
        dispatchDetails.add(detail);
        detail.assignOrderLine(this);
    }

    public Long getId() { return id; }
    public Long getProductId() { return product.getId(); }
    public Integer getRequestedQuantity() { return requestedQuantity; }
    public Integer getMissingQuantity() { return missingQuantity; }
    public List<DispatchDetail> getDispatchDetails() { return List.copyOf(dispatchDetails); }
}
