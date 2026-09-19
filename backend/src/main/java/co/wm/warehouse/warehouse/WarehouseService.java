package co.wm.warehouse.warehouse;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    public WarehouseService(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    public List<WarehouseResponse> findAll() {
        return warehouseRepository.findAll().stream()
                .map(WarehouseResponse::from)
                .toList();
    }

    public WarehouseResponse findById(Long id) {
        return WarehouseResponse.from(findWarehouse(id));
    }

    @Transactional
    public WarehouseResponse create(WarehouseRequest request) {
        String name = request.name().trim();
        ensureNameIsAvailable(name);
        Warehouse warehouse = new Warehouse(name, request.location().trim());
        return WarehouseResponse.from(warehouseRepository.save(warehouse));
    }

    @Transactional
    public WarehouseResponse update(Long id, WarehouseRequest request) {
        Warehouse warehouse = findWarehouse(id);
        String name = request.name().trim();
        if (warehouseRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateWarehouseNameException(name);
        }
        warehouse.update(name, request.location().trim());
        return WarehouseResponse.from(warehouse);
    }

    private Warehouse findWarehouse(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new WarehouseNotFoundException(id));
    }

    private void ensureNameIsAvailable(String name) {
        if (warehouseRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateWarehouseNameException(name);
        }
    }
}
