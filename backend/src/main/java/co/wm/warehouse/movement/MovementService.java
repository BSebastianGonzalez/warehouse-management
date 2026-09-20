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
    public void inbound(MovementRequest request) {
        Product product = findProduct(request.productId());
        ensureNotDiscontinued(product);
        Warehouse warehouse = findWarehouse(request.warehouseId());
        Stock stock = stockRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .orElseGet(() -> new Stock(product, warehouse, 0));
        stock.increase(request.quantity());
        stockRepository.save(stock);
        movementRepository.save(new Movement(
                MovementType.INBOUND, product, null, warehouse, request.quantity(), request.reference()));
    }

    @Transactional
    public void outbound(MovementRequest request) {
        Product product = findProduct(request.productId());
        Warehouse warehouse = findWarehouse(request.warehouseId());
        decrementStock(product, warehouse, request.quantity());
        movementRepository.save(new Movement(
                MovementType.OUTBOUND, product, warehouse, null, request.quantity(), request.reference()));
    }

    @Transactional
    public void transfer(TransferRequest request) {
        if (request.sourceWarehouseId().equals(request.destinationWarehouseId())) {
            throw new InvalidMovementException("Source and destination warehouses must be different");
        }
        Product product = findProduct(request.productId());
        ensureNotDiscontinued(product);
        Warehouse source = findWarehouse(request.sourceWarehouseId());
        Warehouse destination = findWarehouse(request.destinationWarehouseId());
        decrementStock(product, source, request.quantity());
        Stock destinationStock = stockRepository.findByProductIdAndWarehouseId(product.getId(), destination.getId())
                .orElseGet(() -> new Stock(product, destination, 0));
        destinationStock.increase(request.quantity());
        stockRepository.save(destinationStock);
        movementRepository.save(new Movement(
                MovementType.TRANSFER, product, source, destination, request.quantity(), request.reference()));
    }

    private void decrementStock(Product product, Warehouse warehouse, Integer quantity) {
        int updatedRows = stockRepository.decrementIfSufficient(product.getId(), warehouse.getId(), quantity);
        if (updatedRows == 0) {
            throw new InsufficientStockException(product.getId(), warehouse.getId());
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
