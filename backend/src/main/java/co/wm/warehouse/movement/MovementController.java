package co.wm.warehouse.movement;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpSession;
import co.wm.warehouse.admin.AdminService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

@RestController
@RequestMapping("/api/movements")
public class MovementController {

    private final MovementService movementService;
    private final AdminService adminService;

    public MovementController(MovementService movementService, AdminService adminService) {
        this.movementService = movementService;
        this.adminService = adminService;
    }

    @GetMapping
    public Page<MovementReadResponse> findAll(
            @RequestParam(required = false) MovementType type,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long administratorId,
            @RequestParam(required = false) java.time.Instant from,
            @RequestParam(required = false) java.time.Instant to,
            @RequestParam(required = false) String reference,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpSession session) {
        adminService.requireAuthenticated(session);
        return movementService.findAll(
                type, productId, warehouseId, administratorId, from, to, reference, pageable);
    }

    @PostMapping("/inbound")
    @ResponseStatus(HttpStatus.CREATED)
    public MovementResponse inbound(@Valid @RequestBody MovementRequest request, HttpSession session) {
        return movementService.inbound(request, adminService.requireAuthenticated(session));
    }

    @PostMapping("/outbound")
    @ResponseStatus(HttpStatus.CREATED)
    public MovementResponse outbound(@Valid @RequestBody MovementRequest request, HttpSession session) {
        return movementService.outbound(request, adminService.requireAuthenticated(session));
    }

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    public MovementResponse transfer(@Valid @RequestBody TransferRequest request, HttpSession session) {
        return movementService.transfer(request, adminService.requireAuthenticated(session));
    }
}
