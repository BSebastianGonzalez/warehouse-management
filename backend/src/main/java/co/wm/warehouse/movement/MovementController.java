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

@RestController
@RequestMapping("/api/movements")
public class MovementController {

    private final MovementService movementService;
    private final AdminService adminService;

    public MovementController(MovementService movementService, AdminService adminService) {
        this.movementService = movementService;
        this.adminService = adminService;
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
