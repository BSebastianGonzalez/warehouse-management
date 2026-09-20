package co.wm.warehouse.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;

import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Warehouse Management API",
                version = "v1",
                description = "REST API for catalog, authenticated inventory movements, and stock queries in the multi-warehouse inventory system"),
        servers = @Server(url = "/", description = "Current server"))
public class OpenApiConfig {
}
