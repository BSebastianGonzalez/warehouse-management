package co.wm.warehouse.movement;

import java.time.Instant;
import java.util.Objects;

import co.wm.warehouse.product.Product;
import co.wm.warehouse.admin.Admin;
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
import jakarta.persistence.Column;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

@Entity
@Table(name = "movements")
@Check(name = "ck_movements_warehouse_roles", constraints = """
        (type = 'INBOUND' and source_warehouse_id is null and destination_warehouse_id is not null)
        or (type = 'OUTBOUND' and source_warehouse_id is not null and destination_warehouse_id is null)
        or (type = 'TRANSFER' and source_warehouse_id is not null and destination_warehouse_id is not null
            and source_warehouse_id <> destination_warehouse_id)
        """)
@Check(name = "ck_movements_quantity_positive", constraints = "quantity > 0")
public class Movement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MovementType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "administrator_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private Admin administrator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_warehouse_id")
    private Warehouse sourceWarehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_warehouse_id")
    private Warehouse destinationWarehouse;

    @Column(nullable = false)
    private Integer quantity;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(length = 255)
    private String reference;

    protected Movement() {
    }

    public Movement(
            MovementType type,
            Product product,
            Admin administrator,
            Warehouse sourceWarehouse,
            Warehouse destinationWarehouse,
            Integer quantity,
            String reference) {
        validate(type, product, administrator, sourceWarehouse, destinationWarehouse, quantity);
        this.type = type;
        this.product = product;
        this.administrator = administrator;
        this.sourceWarehouse = sourceWarehouse;
        this.destinationWarehouse = destinationWarehouse;
        this.quantity = quantity;
        this.reference = reference;
        this.createdAt = Instant.now();
    }

    private void validate(
            MovementType type,
            Product product,
            Admin administrator,
            Warehouse sourceWarehouse,
            Warehouse destinationWarehouse,
            Integer quantity) {
        if (type == null || product == null || administrator == null || quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Movement type, product, administrator and positive quantity are required");
        }
        if (type == MovementType.INBOUND && (sourceWarehouse != null || destinationWarehouse == null)
                || type == MovementType.OUTBOUND && (sourceWarehouse == null || destinationWarehouse != null)
                || type == MovementType.TRANSFER
                && (sourceWarehouse == null || destinationWarehouse == null
                || Objects.equals(sourceWarehouse.getId(), destinationWarehouse.getId()))) {
            throw new IllegalArgumentException("Movement warehouses do not match movement type");
        }
    }

    public Long getId() { return id; }
    public MovementType getType() { return type; }
    public Integer getQuantity() { return quantity; }
    public Instant getCreatedAt() { return createdAt; }
    public String getReference() { return reference; }
    public Long getAdministratorId() { return administrator == null ? null : administrator.getId(); }
    public String getAdministratorUsername() { return administrator == null ? null : administrator.getUsername(); }
    public Long getProductId() { return product.getId(); }
    public Long getSourceWarehouseId() { return sourceWarehouse == null ? null : sourceWarehouse.getId(); }
    public Long getDestinationWarehouseId() {
        return destinationWarehouse == null ? null : destinationWarehouse.getId();
    }
}
