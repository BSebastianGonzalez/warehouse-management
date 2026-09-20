package co.wm.warehouse.movement;

import co.wm.warehouse.product.Product;
import co.wm.warehouse.product.ProductNotFoundException;
import co.wm.warehouse.product.ProductRepository;
import co.wm.warehouse.stock.Stock;
import co.wm.warehouse.stock.StockRepository;
import co.wm.warehouse.warehouse.Warehouse;
import co.wm.warehouse.warehouse.WarehouseNotFoundException;
import co.wm.warehouse.warehouse.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

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
    public MovementResponse inbound(MovementRequest request) {
        Product product = findProduct(request.productId());
        ensureNotDiscontinued(product);
        Warehouse warehouse = findWarehouse(request.warehouseId());
        incrementStock(product, warehouse, request.quantity());
        Movement movement = movementRepository.save(new Movement(
                MovementType.INBOUND, product, null, warehouse, request.quantity(), request.reference()));
        return MovementResponse.from(movement);
    }

    @Transactional
    public MovementResponse outbound(MovementRequest request) {
        Product product = findProduct(request.productId());
        Warehouse warehouse = findWarehouse(request.warehouseId());
        decrementStock(product, warehouse, request.quantity());
        Movement movement = movementRepository.save(new Movement(
                MovementType.OUTBOUND, product, warehouse, null, request.quantity(), request.reference()));
        return MovementResponse.from(movement);
    }

    @Transactional
    public MovementResponse transfer(TransferRequest request) {
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
                MovementType.TRANSFER, product, source, destination, request.quantity(), request.reference()));
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
