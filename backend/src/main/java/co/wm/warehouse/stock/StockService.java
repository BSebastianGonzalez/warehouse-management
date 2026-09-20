package co.wm.warehouse.stock;

import java.util.List;

import co.wm.warehouse.product.Product;
import co.wm.warehouse.product.ProductRepository;
import co.wm.warehouse.warehouse.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StockService {

    private final StockRepository stockRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;

    public StockService(
            StockRepository stockRepository,
            ProductRepository productRepository,
            WarehouseRepository warehouseRepository) {
        this.stockRepository = stockRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
    }

    public StockResponse find(Long productId, Long warehouseId) {
        return stockRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                .map(StockResponse::from)
                .orElse(new StockResponse(productId, warehouseId, 0));
    }

    public List<StockResponse> findByProduct(Long productId) {
        return stockRepository.findAllByProductId(productId).stream()
                .map(StockResponse::from)
                .toList();
    }

    public List<ProductStockResponse> findAllProducts() {
        return productRepository.findAll().stream()
                .map(this::summarize)
                .toList();
    }

    private ProductStockResponse summarize(Product product) {
        var stocks = stockRepository.findAllByProductId(product.getId());
        var quantities = stocks.stream().collect(java.util.stream.Collectors.toMap(
                Stock::getWarehouseId, Stock::getQuantity));
        var warehouses = warehouseRepository.findAll().stream()
                .map(warehouse -> new WarehouseStockResponse(
                        warehouse.getId(),
                        warehouse.getName(),
                        quantities.getOrDefault(warehouse.getId(), 0)))
                .toList();
        int total = warehouses.stream().mapToInt(WarehouseStockResponse::quantity).sum();
        return new ProductStockResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getMinimumStock(),
                total,
                total < product.getMinimumStock(),
                warehouses);
    }
}
