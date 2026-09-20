package co.wm.warehouse.warehouse;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private WarehouseService warehouseService;

    @Test
    void rejectsDuplicateWarehouseName() {
        WarehouseRequest request = new WarehouseRequest("Main", "North");
        when(warehouseRepository.existsByNameIgnoreCase("Main")).thenReturn(true);

        assertThatThrownBy(() -> warehouseService.create(request))
                .isInstanceOf(DuplicateWarehouseNameException.class);
        verify(warehouseRepository, never()).save(any(Warehouse.class));
    }

    @Test
    void rejectsUnknownWarehouseOnLookup() {
        when(warehouseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseService.findById(99L))
                .isInstanceOf(WarehouseNotFoundException.class);
    }
}
