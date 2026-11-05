package co.edu.uniquindio.sga.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Esquema de seguridad para Swagger UI: el mismo bearerAuth de docs/api/openapi.yaml (Guía 12, 6.5).
 * Hace aparecer el botón Authorize; los endpoints públicos lo anulan con @SecurityRequirements vacío.
 * Además documenta en un solo lugar las respuestas 401 y 403 de todos los endpoints protegidos, en vez de
 * repetir las dos anotaciones @ApiResponse en cada método de cada controller.
 */
@Configuration
@OpenAPIDefinition(security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {

    @Bean
    public OperationCustomizer respuestasDeSeguridad() {
        return (operation, handlerMethod) -> {
            boolean esPublico = handlerMethod.hasMethodAnnotation(SecurityRequirements.class);
            if (esPublico) {
                return operation;
            }
            if (operation.getResponses() == null) {
                operation.setResponses(new ApiResponses());
            }
            operation.getResponses()
                    .addApiResponse("401", new ApiResponse().description("Sin token, token inválido o vencido"))
                    .addApiResponse("403", new ApiResponse()
                            .description("El rol no lo permite o no es el titular de la reserva"));
            return operation;
        };
    }
}
