package co.wm.warehouse.movement;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import co.wm.warehouse.product.Product;
import co.wm.warehouse.product.ProductRepository;
import co.wm.warehouse.stock.StockRepository;
import co.wm.warehouse.warehouse.Warehouse;
import co.wm.warehouse.warehouse.WarehouseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MovementServiceTest {

    @Mock
    private MovementRepository movementRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private MovementService movementService;

    @Test
    void rejectsOutboundWhenAtomicDecrementDoesNotUpdateStock() {
        Product product = org.mockito.Mockito.mock(Product.class);
        Warehouse warehouse = org.mockito.Mockito.mock(Warehouse.class);
        when(product.getId()).thenReturn(1L);
        when(warehouse.getId()).thenReturn(1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(stockRepository.decrementIfSufficient(1L, 1L, 3)).thenReturn(0);

        assertThatThrownBy(() -> movementService.outbound(new MovementRequest(1L, 1L, 3, "sale")))
                .isInstanceOf(InsufficientStockException.class);
        verify(movementRepository, never()).save(any(Movement.class));
    }

    @Test
    void rejectsTransferToSameWarehouse() {
        TransferRequest request = new TransferRequest(1L, 2L, 2L, 1, "relocation");

        assertThatThrownBy(() -> movementService.transfer(request))
                .isInstanceOf(InvalidMovementException.class);
        verify(productRepository, never()).findById(1L);
    }

    @Test
    void rejectsInboundForDiscontinuedProduct() {
        Product product = new Product("SKU-1", "Paper", "unit", 10, true);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> movementService.inbound(new MovementRequest(1L, 1L, 2, "receipt")))
                .isInstanceOf(InvalidMovementException.class);
        verify(warehouseRepository, never()).findById(1L);
    }
}
