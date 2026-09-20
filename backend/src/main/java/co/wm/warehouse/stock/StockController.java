package co.wm.warehouse.stock;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/products/{productId}/warehouses/{warehouseId}")
    public StockResponse find(@PathVariable Long productId, @PathVariable Long warehouseId) {
        return stockService.find(productId, warehouseId);
    }

    @GetMapping
    public List<ProductStockResponse> findAllProducts() {
        return stockService.findAllProducts();
    }

    @GetMapping("/products/{productId}")
    public List<StockResponse> findByProduct(@PathVariable Long productId) {
        return stockService.findByProduct(productId);
    }
}
