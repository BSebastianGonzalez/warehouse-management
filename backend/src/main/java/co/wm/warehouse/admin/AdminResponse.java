package co.wm.warehouse.admin;

public record AdminResponse(Long id, String username, String name) {

    public static AdminResponse from(Admin admin) {
        return new AdminResponse(admin.getId(), admin.getUsername(), admin.getName());
    }
}
