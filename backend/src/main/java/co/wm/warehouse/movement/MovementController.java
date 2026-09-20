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
    public MovementResponse inbound(@Valid @RequestBody MovementRequest request) {
        return movementService.inbound(request);
    }

    @PostMapping("/outbound")
    @ResponseStatus(HttpStatus.CREATED)
    public MovementResponse outbound(@Valid @RequestBody MovementRequest request) {
        return movementService.outbound(request);
    }

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    public MovementResponse transfer(@Valid @RequestBody TransferRequest request) {
        return movementService.transfer(request);
    }
}
