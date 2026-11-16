package co.edu.uniquindio.sga.infrastructure.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.yaml.snakeyaml.Yaml;

/**
 * El contrato {@code docs/api/openapi.yaml} se escribe a mano (Guía 07) y la rúbrica cuenta como incoherencia un
 * endpoint que esté en uno y no en el otro. Esta prueba lo comprueba en cada build: compara las operaciones
 * "MÉTODO ruta" del yaml con las que de verdad expone la aplicación (solo los controllers de negocio, no los de
 * springdoc ni /error), y exige 401 y 403 en todo endpoint protegido.
 */
@SpringBootTest
class OpenApiCoherenciaTest {

    private static final Set<String> METODOS_HTTP = Set.of("get", "post", "put", "patch", "delete");
    private static final String PAQUETE_CONTROLLERS = "co.edu.uniquindio.sga.infrastructure.rest.controller";

    @Autowired private RequestMappingHandlerMapping mapeos;

    @SuppressWarnings("unchecked")
    private Map<String, Object> paths() throws IOException {
        String texto = Files.readString(Path.of("docs", "api", "openapi.yaml"), StandardCharsets.UTF_8);
        Map<String, Object> raiz = new Yaml().load(texto);
        return (Map<String, Object>) raiz.get("paths");
    }

    private static String normalizar(String ruta) {
        return ruta.replaceAll("\\{[^}]+}", "{}");
    }

    @SuppressWarnings("unchecked")
    private Set<String> operacionesDelYaml() throws IOException {
        Set<String> operaciones = new TreeSet<>();
        paths().forEach((ruta, definicion) -> ((Map<String, Object>) definicion).forEach((metodo, operacion) -> {
            if (METODOS_HTTP.contains(metodo)) {
                operaciones.add(metodo.toUpperCase() + " " + normalizar(ruta));
            }
        }));
        return operaciones;
    }

    private Set<String> operacionesDelCodigo() {
        Set<String> operaciones = new TreeSet<>();
        for (Map.Entry<RequestMappingInfo, ?> entrada : mapeos.getHandlerMethods().entrySet()) {
            var manejador = (org.springframework.web.method.HandlerMethod) entrada.getValue();
            if (!manejador.getBeanType().getName().startsWith(PAQUETE_CONTROLLERS)) {
                continue;
            }
            Set<String> rutas = entrada.getKey().getPathPatternsCondition().getPatternValues();
            for (RequestMethod metodo : entrada.getKey().getMethodsCondition().getMethods()) {
                rutas.forEach(ruta -> operaciones.add(metodo.name() + " " + normalizar(ruta)));
            }
        }
        return operaciones;
    }

    @Test
    void elYamlYLaAplicacionExponenExactamenteLasMismasOperaciones() throws IOException {
        Set<String> yaml = operacionesDelYaml();
        Set<String> codigo = operacionesDelCodigo();

        Set<String> soloEnElCodigo = new TreeSet<>(codigo);
        soloEnElCodigo.removeAll(yaml);
        Set<String> soloEnElYaml = new TreeSet<>(yaml);
        soloEnElYaml.removeAll(codigo);

        assertEquals(Set.of(), soloEnElCodigo, "Endpoints que están en la aplicación y faltan en openapi.yaml");
        assertEquals(Set.of(), soloEnElYaml, "Endpoints que están en openapi.yaml y no existen en la aplicación");
        assertTrue(codigo.contains("GET /api/caracteristicas"), "el catálogo de L-06 debe estar expuesto");
    }

    @Test
    @SuppressWarnings("unchecked")
    void todoEndpointProtegidoDocumentaLas401YLas403() throws IOException {
        // login y registro son públicos (security: []); todos los demás exigen token
        Set<String> publicos = Set.of("/api/auth/login", "/api/auth/registro");
        Set<String> incompletos = new TreeSet<>();
        paths().forEach((ruta, definicion) -> ((Map<String, Object>) definicion).forEach((metodo, operacion) -> {
            if (!METODOS_HTTP.contains(metodo) || publicos.contains(ruta)) {
                return;
            }
            Map<String, Object> respuestas = (Map<String, Object>) ((Map<String, Object>) operacion).get("responses");
            for (String codigo : List.of("401", "403")) {
                if (!respuestas.containsKey(codigo)) {
                    incompletos.add(metodo.toUpperCase() + " " + ruta + " sin " + codigo);
                }
            }
        }));

        assertEquals(Set.of(), incompletos);
    }
}
