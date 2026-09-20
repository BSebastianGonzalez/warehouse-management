package co.wm.warehouse.stock;

import java.util.List;

import co.wm.warehouse.product.Product;
import co.wm.warehouse.product.ProductNotFoundException;
import co.wm.warehouse.product.ProductRepository;
import co.wm.warehouse.movement.MovementRepository;
import co.wm.warehouse.warehouse.WarehouseRepository;
import co.wm.warehouse.warehouse.WarehouseNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StockService {

    private final StockRepository stockRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final MovementRepository movementRepository;

    public StockService(
            StockRepository stockRepository,
            ProductRepository productRepository,
            WarehouseRepository warehouseRepository,
            MovementRepository movementRepository) {
        this.stockRepository = stockRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.movementRepository = movementRepository;
    }

    public StockResponse find(Long productId, Long warehouseId) {
        ensureProductExists(productId);
        ensureWarehouseExists(warehouseId);
        return stockRepository.findByProduct_IdAndWarehouse_Id(productId, warehouseId)
                .map(StockResponse::from)
                .orElse(new StockResponse(productId, warehouseId, 0));
    }

    public List<StockResponse> findByProduct(Long productId) {
        ensureProductExists(productId);
        var stocksByWarehouse = stockRepository.findAllByProductIdWithWarehouse(productId).stream()
                .collect(java.util.stream.Collectors.toMap(Stock::getWarehouseId, Stock::getQuantity));
        return warehouseRepository.findAll().stream()
                .map(warehouse -> new StockResponse(
                        productId,
                        warehouse.getId(),
                        stocksByWarehouse.getOrDefault(warehouse.getId(), 0)))
                .toList();
    }

    public List<ProductStockResponse> findAllProducts() {
        var warehouses = warehouseRepository.findAll();
        var stocksByProduct = stockRepository.findAllWithProductAndWarehouse().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        Stock::getProductId,
                        java.util.stream.Collectors.toMap(Stock::getWarehouseId, Stock::getQuantity)));
        return productRepository.findAll().stream()
                .map(product -> summarize(product, warehouses, stocksByProduct.getOrDefault(
                        product.getId(), java.util.Map.of())))
                .toList();
    }

    public Integer reconstructedBalance(Long productId, Long warehouseId) {
        ensureProductExists(productId);
        ensureWarehouseExists(warehouseId);
        return movementRepository.calculateBalance(productId, warehouseId);
    }

    public StockReconciliationResponse reconcile(Long productId, Long warehouseId) {
        ensureProductExists(productId);
        ensureWarehouseExists(warehouseId);
        int projectedQuantity = stockRepository.findByProduct_IdAndWarehouse_Id(productId, warehouseId)
                .map(Stock::getQuantity)
                .orElse(0);
        int reconstructedQuantity = movementRepository.calculateBalance(productId, warehouseId);
        return new StockReconciliationResponse(
                productId,
                warehouseId,
                projectedQuantity,
                reconstructedQuantity,
                projectedQuantity == reconstructedQuantity);
    }

    private ProductStockResponse summarize(
            Product product,
            java.util.List<co.wm.warehouse.warehouse.Warehouse> warehouses,
            java.util.Map<Long, Integer> quantities) {
        var warehouseResponses = warehouses.stream()
                .map(warehouse -> new WarehouseStockResponse(
                        warehouse.getId(),
                        warehouse.getName(),
                        quantities.getOrDefault(warehouse.getId(), 0)))
                .toList();
        int total = warehouseResponses.stream().mapToInt(WarehouseStockResponse::quantity).sum();
        return new ProductStockResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getMinimumStock(),
                total,
                total < product.getMinimumStock(),
                warehouseResponses);
    }

    private void ensureProductExists(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }
    }

    private void ensureWarehouseExists(Long warehouseId) {
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new WarehouseNotFoundException(warehouseId);
        }
    }
}
