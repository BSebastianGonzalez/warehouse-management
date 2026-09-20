package co.wm.warehouse.order;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import co.wm.warehouse.admin.Admin;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "administrator_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private Admin administrator;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<OrderLine> lines = new ArrayList<>();

    protected Order() {
    }

    public Order(Admin administrator, OrderStatus status) {
        if (administrator == null || status == null) {
            throw new IllegalArgumentException("Order administrator and status are required");
        }
        this.administrator = administrator;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public void addLine(OrderLine line) {
        lines.add(line);
        line.assignOrder(this);
    }

    public Long getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
    public OrderStatus getStatus() { return status; }
    public Long getAdministratorId() { return administrator == null ? null : administrator.getId(); }
    public String getAdministratorUsername() { return administrator == null ? null : administrator.getUsername(); }
    public List<OrderLine> getLines() { return List.copyOf(lines); }
}
