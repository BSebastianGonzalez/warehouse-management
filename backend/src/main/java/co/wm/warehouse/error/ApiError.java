package co.wm.warehouse.error;

import java.time.Instant;

public record ApiError(Instant timestamp, int status, String message) {
}
