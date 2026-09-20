package co.wm.warehouse.movement;

import jakarta.validation.Valid;
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

    public MovementController(MovementService movementService) {
        this.movementService = movementService;
    }

    @PostMapping("/inbound")
    @ResponseStatus(HttpStatus.CREATED)
    public void inbound(@Valid @RequestBody MovementRequest request) {
        movementService.inbound(request);
    }

    @PostMapping("/outbound")
    @ResponseStatus(HttpStatus.CREATED)
    public void outbound(@Valid @RequestBody MovementRequest request) {
        movementService.outbound(request);
    }

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    public void transfer(@Valid @RequestBody TransferRequest request) {
        movementService.transfer(request);
    }
}
