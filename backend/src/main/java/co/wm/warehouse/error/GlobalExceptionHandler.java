package co.wm.warehouse.error;

import java.time.Instant;

import co.wm.warehouse.product.DuplicateSkuException;
import co.wm.warehouse.product.ProductNotFoundException;
import co.wm.warehouse.warehouse.DuplicateWarehouseNameException;
import co.wm.warehouse.warehouse.WarehouseNotFoundException;
import co.wm.warehouse.movement.InsufficientStockException;
import co.wm.warehouse.movement.InvalidMovementException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleProductNotFound(ProductNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(DuplicateSkuException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleDuplicateSku(DuplicateSkuException exception) {
        return error(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(WarehouseNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleWarehouseNotFound(WarehouseNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(DuplicateWarehouseNameException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleDuplicateWarehouseName(DuplicateWarehouseNameException exception) {
        return error(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler({InsufficientStockException.class, InvalidMovementException.class})
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiError handleInvalidMovement(RuntimeException exception) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleInvalidArgument(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .orElse("Request validation failed");
        return error(HttpStatus.BAD_REQUEST, message);
    }

    private ApiError error(HttpStatus status, String message) {
        return new ApiError(Instant.now(), status.value(), message);
    }
}
