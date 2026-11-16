package co.edu.uniquindio.sga.infrastructure.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.Filter;

/**
 * CORS para el frontend (Guía 15, 4.2): lo que el navegador pregunta antes de llamar a la API desde otro origen. El
 * origen autorizado es el de la propiedad {@code sga.cors.origenes-permitidos} (por defecto el servidor de Vite,
 * http://localhost:5173). Aplicación completa, con la cadena de seguridad real.
 */
@SpringBootTest
class CorsIntegracionTest {

    private static final String ORIGEN_FRONTEND = "http://localhost:5173";
    private static final String ORIGEN_AJENO = "http://sitio-ajeno.example";

    @Autowired private WebApplicationContext contexto;
    @Autowired @Qualifier("springSecurityFilterChain") private Filter filtrosDeSeguridad;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtrosDeSeguridad).build();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder preflight(
            String ruta, String origen, String metodo) {
        return options(ruta).header(HttpHeaders.ORIGIN, origen)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, metodo)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type");
    }

    @Test
    void elPreflightDesdeElOrigenDelFrontendSeResponde200SinNecesitarToken() throws Exception {
        mvc.perform(preflight("/api/reservas", ORIGEN_FRONTEND, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEN_FRONTEND))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsStringIgnoringCase("Authorization")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsStringIgnoringCase("Content-Type")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"));
    }

    @Test
    void elPreflightAutorizaGetPostYPutPeroNoDelete() throws Exception {
        for (String metodo : List.of("GET", "POST", "PUT")) {
            mvc.perform(preflight("/api/reservas/RES-2026-00001/confirmar", ORIGEN_FRONTEND, metodo))
                    .andExpect(status().isOk());
        }
        // la eliminación es lógica: DELETE no está autorizado ni siquiera para el origen del frontend
        mvc.perform(preflight("/api/reservas/RES-2026-00001", ORIGEN_FRONTEND, "DELETE"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void elPreflightDesdeOtroOrigenSeRechazaYNoTraeElEncabezadoQueLoAutoriza() throws Exception {
        mvc.perform(preflight("/api/reservas", ORIGEN_AJENO, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS));
    }

    @Test
    void elPreflightTambienFuncionaParaElLoginQueEsPublico() throws Exception {
        mvc.perform(preflight("/api/auth/login", ORIGEN_FRONTEND, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEN_FRONTEND));
    }

    @Test
    void laRespuestaRealALOrigenDelFrontendExponeLocationParaQueElClienteLaLea() throws Exception {
        String cuerpo = mvc.perform(post("/api/auth/login").header(HttpHeaders.ORIGIN, ORIGEN_FRONTEND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"recepcion@sga.local\",\"contrasena\":\"Recepcion2026\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEN_FRONTEND))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(cuerpo, "$.token");

        mvc.perform(get("/api/apartamentos").header(HttpHeaders.ORIGIN, ORIGEN_FRONTEND)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEN_FRONTEND))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, containsString("Location")));
    }

    @Test
    void unaPeticionRealDesdeOtroOrigenNoRecibeElPermisoDeLectura() throws Exception {
        // sin Authorization: el 401 de la seguridad no se disfraza, pero el navegador no podría leerlo (sin permiso CORS)
        mvc.perform(get("/api/apartamentos").header(HttpHeaders.ORIGIN, ORIGEN_AJENO))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void laConfiguracionNoUsaComodinNiCredencialesYAutorizaSoloLosOrigenesDeLaPropiedad() {
        UrlBasedCorsConfigurationSource fuente = (UrlBasedCorsConfigurationSource)
                new SecurityConfig(null, null, null)
                        .corsConfigurationSource(List.of(" http://a.example ", "http://b.example", ""));
        CorsConfiguration configuracion = fuente.getCorsConfigurations().get("/api/**");

        assertEquals(List.of("http://a.example", "http://b.example"), configuracion.getAllowedOrigins());
        assertFalse(configuracion.getAllowedOrigins().contains("*"));
        assertEquals(List.of("GET", "POST", "PUT", "OPTIONS"), configuracion.getAllowedMethods());
        assertEquals(List.of("Authorization", "Content-Type"), configuracion.getAllowedHeaders());
        assertEquals(List.of("Location"), configuracion.getExposedHeaders());
        assertTrue(configuracion.getAllowCredentials() == null || !configuracion.getAllowCredentials());
        assertNull(fuente.getCorsConfigurations().get("/**"), "solo las rutas /api/** admiten CORS");
    }
}
