package co.wm.warehouse.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "products")
@Check(constraints = "minimum_stock >= 0")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 50)
    private String unitOfMeasure;

    @Column(nullable = false)
    private Integer minimumStock = 10;

    @Column(nullable = false)
    private boolean discontinued;

    protected Product() {
    }

    public Product(String sku, String name, String unitOfMeasure, Integer minimumStock, boolean discontinued) {
        validateMinimumStock(minimumStock);
        this.sku = sku;
        this.name = name;
        this.unitOfMeasure = unitOfMeasure;
        this.minimumStock = minimumStock;
        this.discontinued = discontinued;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public Integer getMinimumStock() {
        return minimumStock;
    }

    public boolean isDiscontinued() {
        return discontinued;
    }

    public void update(String sku, String name, String unitOfMeasure, Integer minimumStock, boolean discontinued) {
        validateMinimumStock(minimumStock);
        this.sku = sku;
        this.name = name;
        this.unitOfMeasure = unitOfMeasure;
        this.minimumStock = minimumStock;
        this.discontinued = discontinued;
    }

    private void validateMinimumStock(Integer minimumStock) {
        if (minimumStock == null || minimumStock < 0) {
            throw new IllegalArgumentException("Minimum stock cannot be negative");
        }
    }
}
