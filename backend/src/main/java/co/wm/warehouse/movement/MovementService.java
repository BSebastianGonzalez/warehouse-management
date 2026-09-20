package co.wm.warehouse.movement;

import co.wm.warehouse.product.Product;
import co.wm.warehouse.product.ProductNotFoundException;
import co.wm.warehouse.product.ProductRepository;
import co.wm.warehouse.admin.Admin;
import co.wm.warehouse.stock.Stock;
import co.wm.warehouse.stock.StockRepository;
import co.wm.warehouse.warehouse.Warehouse;
import co.wm.warehouse.warehouse.WarehouseNotFoundException;
import co.wm.warehouse.warehouse.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.JoinType;
import java.util.Objects;
import java.time.Instant;

@Service
public class MovementService {

    private final MovementRepository movementRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockRepository stockRepository;

    public MovementService(
            MovementRepository movementRepository,
            ProductRepository productRepository,
            WarehouseRepository warehouseRepository,
            StockRepository stockRepository) {
        this.movementRepository = movementRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.stockRepository = stockRepository;
    }

    @Transactional
    public MovementResponse inbound(MovementRequest request, Admin administrator) {
        Product product = findProduct(request.productId());
        ensureNotDiscontinued(product);
        Warehouse warehouse = findWarehouse(request.warehouseId());
        incrementStock(product, warehouse, request.quantity());
        Movement movement = movementRepository.save(new Movement(
                MovementType.INBOUND, product, administrator, null, warehouse, request.quantity(), request.reference()));
        return MovementResponse.from(movement);
    }

    @Transactional(readOnly = true)
    public Page<MovementReadResponse> findAll(
            MovementType type,
            Long productId,
            Long warehouseId,
            Long administratorId,
            Instant from,
            Instant to,
            String reference,
            Pageable pageable) {
        Specification<Movement> specification = Specification.where((Specification<Movement>) null);
        if (type != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("type"), type));
        }
        if (productId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("product").get("id"), productId));
        }
        if (warehouseId != null) {
            specification = specification.and((root, query, builder) -> {
                var source = root.join("sourceWarehouse", JoinType.LEFT);
                var destination = root.join("destinationWarehouse", JoinType.LEFT);
                return builder.or(
                        builder.equal(source.get("id"), warehouseId),
                        builder.equal(destination.get("id"), warehouseId));
            });
        }
        if (administratorId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("administrator").get("id"), administratorId));
        }
        if (from != null) {
            specification = specification.and((root, query, builder) ->
                    builder.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            specification = specification.and((root, query, builder) ->
                    builder.lessThanOrEqualTo(root.get("createdAt"), to));
        }
        if (reference != null && !reference.isBlank()) {
            specification = specification.and((root, query, builder) ->
                    builder.like(builder.lower(root.get("reference")), "%" + reference.trim().toLowerCase() + "%"));
        }
        return movementRepository.findAll(specification, pageable).map(MovementReadResponse::from);
    }

    @Transactional
    public MovementResponse outbound(MovementRequest request, Admin administrator) {
        Product product = findProduct(request.productId());
        Warehouse warehouse = findWarehouse(request.warehouseId());
        decrementStock(product, warehouse, request.quantity());
        Movement movement = movementRepository.save(new Movement(
                MovementType.OUTBOUND, product, administrator, warehouse, null, request.quantity(), request.reference()));
        return MovementResponse.from(movement);
    }

    @Transactional
    public MovementResponse transfer(TransferRequest request, Admin administrator) {
        if (Objects.equals(request.sourceWarehouseId(), request.destinationWarehouseId())) {
            throw new InvalidMovementException("Source and destination warehouses must be different");
        }
        Product product = findProduct(request.productId());
        ensureNotDiscontinued(product);
        Warehouse source = findWarehouse(request.sourceWarehouseId());
        Warehouse destination = findWarehouse(request.destinationWarehouseId());
        decrementStock(product, source, request.quantity());
        incrementStock(product, destination, request.quantity());
        Movement movement = movementRepository.save(new Movement(
                MovementType.TRANSFER, product, administrator, source, destination, request.quantity(), request.reference()));
        return MovementResponse.from(movement);
    }

    private void decrementStock(Product product, Warehouse warehouse, Integer quantity) {
        int updatedRows = stockRepository.decrementIfSufficient(product.getId(), warehouse.getId(), quantity);
        if (updatedRows == 0) {
            throw new InsufficientStockException(product.getId(), warehouse.getId());
        }
    }

    private void incrementStock(Product product, Warehouse warehouse, Integer quantity) {
        int updatedRows = stockRepository.incrementOrCreate(product.getId(), warehouse.getId(), quantity);
        if (updatedRows <= 0) {
            throw new InvalidMovementException("Stock could not be updated");
        }
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    private Warehouse findWarehouse(Long id) {
        return warehouseRepository.findById(id).orElseThrow(() -> new WarehouseNotFoundException(id));
    }

    private void ensureNotDiscontinued(Product product) {
        if (product.isDiscontinued()) {
            throw new InvalidMovementException("Discontinued products cannot receive inbound stock");
        }
    }
}
