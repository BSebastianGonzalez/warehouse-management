package co.wm.warehouse.admin;

public class DuplicateAdminUsernameException extends RuntimeException {

    public DuplicateAdminUsernameException(String username) {
        super("Administrator username already exists: " + username);
    }
}
