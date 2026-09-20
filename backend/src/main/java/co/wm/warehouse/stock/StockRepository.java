package co.wm.warehouse.stock;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByProductIdAndWarehouseId(Long productId, Long warehouseId);

    @Modifying
    @Query("""
            update Stock stock
            set stock.quantity = stock.quantity - :quantity
            where stock.product.id = :productId
              and stock.warehouse.id = :warehouseId
              and stock.quantity >= :quantity
            """)
    int decrementIfSufficient(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId,
            @Param("quantity") Integer quantity);
}
