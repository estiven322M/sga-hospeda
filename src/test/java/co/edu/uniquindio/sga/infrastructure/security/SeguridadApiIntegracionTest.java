package co.edu.uniquindio.sga.infrastructure.security;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.Filter;

/**
 * Verificación de la sección 9 de la Guía 12 con la aplicación completa: cadena de filtros real, usuarios iniciales
 * reales en H2 y tokens emitidos por el login. Cubre 401 frente a 403, la matriz de roles y el propietario.
 */
@SpringBootTest
class SeguridadApiIntegracionTest {

    @Autowired private WebApplicationContext contexto;
    @Autowired @Qualifier("springSecurityFilterChain") private Filter filtrosDeSeguridad;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtrosDeSeguridad).build();
    }

    private String token(String correo, String contrasena) throws Exception {
        String cuerpo = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"contrasena\":\"" + contrasena + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(cuerpo, "$.token");
    }

    private String recepcion() throws Exception { return token("recepcion@sga.local", "Recepcion2026"); }
    private String huesped1() throws Exception { return token("huesped1@sga.local", "Huesped2026"); }
    private String huesped2() throws Exception { return token("huesped2@sga.local", "Huesped2026"); }
    private String servicio() throws Exception { return token("servicio@sga.local", "Servicio2026"); }

    /** Código de una reserva sembrada cuyo titular es el huésped 1 (documento 1094000111). */
    private String reservaDelHuesped1() throws Exception {
        String cuerpo = mvc.perform(get("/api/reservas").param("titular", "1094000111")
                        .header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(cuerpo, "$.contenido[0].codigo");
    }

    @Test
    void caso3_loginCorrectoEmiteTokenBearerConElRol() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"recepcion@sga.local\",\"contrasena\":\"Recepcion2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.roles.length()").value(1))
                .andExpect(jsonPath("$.roles[0]").value("RECEPCIONISTA"));
    }

    @Test
    void caso4_loginConContrasenaErradaResponde401ConErrorResponse() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"recepcion@sga.local\",\"contrasena\":\"Equivocada2026\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Credenciales inválidas"));
    }

    @Test
    void caso6_sinTokenResponde401ConErrorResponseYWwwAuthenticate() throws Exception {
        mvc.perform(get("/api/reservas").param("estado", "PENDIENTE"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void caso7_unHuespedNoListaLasReservasDeTodos() throws Exception {
        mvc.perform(get("/api/reservas").param("estado", "PENDIENTE").header(HttpHeaders.AUTHORIZATION, huesped1()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void caso8_recepcionListaLasReservas() throws Exception {
        mvc.perform(get("/api/reservas").param("estado", "PENDIENTE").header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(status().isOk());
    }

    @Test
    void caso9y10_elHuespedVeSuReservaYOtroHuespedNo() throws Exception {
        String codigo = reservaDelHuesped1();

        mvc.perform(get("/api/reservas/" + codigo).header(HttpHeaders.AUTHORIZATION, huesped1()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(codigo));
        mvc.perform(get("/api/reservas/" + codigo).header(HttpHeaders.AUTHORIZATION, huesped2()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/reservas/" + codigo + "/folio").header(HttpHeaders.AUTHORIZATION, huesped2()))
                .andExpect(status().isForbidden());
    }

    @Test
    void caso11_unaReservaInexistenteParaRecepcionEs404() throws Exception {
        mvc.perform(get("/api/reservas/RES-2026-99999").header(HttpHeaders.AUTHORIZATION, recepcion()))
                .andExpect(status().isNotFound());
    }

    @Test
    void caso12_elPersonalDeServicioNoConfirmaReservas() throws Exception {
        mvc.perform(put("/api/reservas/" + reservaDelHuesped1() + "/confirmar")
                        .header(HttpHeaders.AUTHORIZATION, servicio())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"horaEstimadaLlegada\":\"16:00\",\"autor\":\"Servicio\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void caso13_unTokenModificadoAManoResponde401() throws Exception {
        String alterado = recepcion() + "x";

        mvc.perform(get("/api/reservas").header(HttpHeaders.AUTHORIZATION, alterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void elHuespedNoReservaANombreDeOtroNiPorElCanalDirecto() throws Exception {
        String aNombreDeOtro = """
                {"identificacionApartamento":"APT-101","fechaEntrada":"2028-05-10","fechaSalida":"2028-05-12",
                 "canalOrigen":"PORTAL","horaEstimadaLlegada":"16:00",
                 "titular":{"documento":"1094000222","nombre":"Andres Rios","fechaNacimiento":"1988-09-12"},
                 "ocupantes":[{"documento":"1094000222","nombre":"Andres Rios","fechaNacimiento":"1988-09-12"}]}""";

        mvc.perform(post("/api/reservas").header(HttpHeaders.AUTHORIZATION, huesped1())
                        .contentType(MediaType.APPLICATION_JSON).content(aNombreDeOtro))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/reservas").header(HttpHeaders.AUTHORIZATION, huesped2())
                        .contentType(MediaType.APPLICATION_JSON).content(aNombreDeOtro.replace("PORTAL", "DIRECTO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void elRegistroPublicoNoExigeTokenYCreaUnHuespedQuePuedeIniciarSesion() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre":"Prueba Registro","correo":"registro.integracion@correo.com",
                         "contrasena":"Segura2026","telefono":"3005550001","documento":"1094999001",
                         "fechaNacimiento":"1990-01-01"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("HUESPED"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"registro.integracion@correo.com\",\"contrasena\":\"Segura2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("HUESPED"));
    }
}
