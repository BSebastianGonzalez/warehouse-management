package co.wm.warehouse.movement;

import java.time.Instant;

import co.wm.warehouse.product.Product;
import co.wm.warehouse.warehouse.Warehouse;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "movements")
public class Movement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private MovementType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_warehouse_id")
    private Warehouse sourceWarehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_warehouse_id")
    private Warehouse destinationWarehouse;

    private Integer quantity;
    private Instant createdAt;
    private String reference;

    protected Movement() {
    }

    public Movement(
            MovementType type,
            Product product,
            Warehouse sourceWarehouse,
            Warehouse destinationWarehouse,
            Integer quantity,
            String reference) {
        this.type = type;
        this.product = product;
        this.sourceWarehouse = sourceWarehouse;
        this.destinationWarehouse = destinationWarehouse;
        this.quantity = quantity;
        this.reference = reference;
        this.createdAt = Instant.now();
    }
}
