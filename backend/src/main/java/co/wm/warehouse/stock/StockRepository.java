package co.wm.warehouse.stock;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByProduct_IdAndWarehouse_Id(Long productId, Long warehouseId);

    java.util.List<Stock> findAllByProductId(Long productId);

    @Query("""
            select stock from Stock stock
            join fetch stock.warehouse
            where stock.product.id = :productId
            """)
    List<Stock> findAllByProductIdWithWarehouse(@Param("productId") Long productId);

    @Query("""
            select stock from Stock stock
            join fetch stock.product
            join fetch stock.warehouse
            """)
    List<Stock> findAllWithProductAndWarehouse();

    @Modifying
    @Query(value = """
            insert into stocks (product_id, warehouse_id, quantity)
            values (:productId, :warehouseId, :quantity)
            on duplicate key update quantity = quantity + values(quantity)
            """, nativeQuery = true)
    int incrementOrCreate(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId,
            @Param("quantity") Integer quantity);

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
