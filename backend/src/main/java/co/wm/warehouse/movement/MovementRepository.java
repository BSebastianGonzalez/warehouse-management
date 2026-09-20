package co.wm.warehouse.movement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovementRepository extends JpaRepository<Movement, Long> {

    @Query("""
            select coalesce(sum(
                case when movement.destinationWarehouse.id = :warehouseId then movement.quantity
                     when movement.sourceWarehouse.id = :warehouseId then -movement.quantity
                     else 0 end), 0)
            from Movement movement
            where movement.product.id = :productId
              and (movement.sourceWarehouse.id = :warehouseId
                   or movement.destinationWarehouse.id = :warehouseId)
            """)
    Integer calculateBalance(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId);
}
