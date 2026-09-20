package co.wm.warehouse.stock;

import co.wm.warehouse.product.Product;
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
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "stocks", uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "warehouse_id"}))
@Check(constraints = "quantity >= 0")
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(nullable = false)
    private Integer quantity;

    protected Stock() {
    }

    public Stock(Product product, Warehouse warehouse, Integer quantity) {
        if (quantity == null || quantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }
        this.product = product;
        this.warehouse = warehouse;
        this.quantity = quantity;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void increase(Integer amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Stock increase must be positive");
        }
        quantity += amount;
    }

    public void decrease(Integer amount) {
        if (amount == null || amount <= 0 || amount > quantity) {
            throw new IllegalArgumentException("Stock decrease must be positive and available");
        }
        quantity -= amount;
    }

    public Long getProductId() {
        return product.getId();
    }

    public Long getWarehouseId() {
        return warehouse.getId();
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }
}
