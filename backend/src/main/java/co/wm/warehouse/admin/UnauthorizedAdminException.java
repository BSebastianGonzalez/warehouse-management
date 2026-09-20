package co.wm.warehouse.admin;

public class UnauthorizedAdminException extends RuntimeException {

    public UnauthorizedAdminException() {
        super("An authenticated administrator session is required");
    }
}
