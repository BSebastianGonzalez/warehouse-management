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

@Entity
@Table(name = "stocks", uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "warehouse_id"}))
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
        this.product = product;
        this.warehouse = warehouse;
        this.quantity = quantity;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void increase(Integer amount) {
        quantity += amount;
    }
}
