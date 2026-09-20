package co.wm.warehouse.admin;

public class AdminAuthenticationException extends RuntimeException {

    public AdminAuthenticationException() {
        super("Invalid administrator credentials");
    }
}
