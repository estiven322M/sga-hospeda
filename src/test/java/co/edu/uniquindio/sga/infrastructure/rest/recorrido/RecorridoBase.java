package co.edu.uniquindio.sga.infrastructure.rest.recorrido;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

import co.edu.uniquindio.sga.testsupport.RelojMutable;
import jakarta.servlet.Filter;

/**
 * Base de los recorridos de extremo a extremo: aplicación completa (H2 propia, datos sembrados, seguridad JWT) con un
 * reloj que la prueba puede mover, login real por HTTP y JSON armado a mano para que cada petición sea legible.
 *
 * Datos sembrados con el reloj en {@link #INICIO} (10-mar-2027, temporada BASE): los 7 apartamentos, las reservas de
 * cada estado y canal, la política de cancelación (72 h: 0 %, 24 h: 50 %, 0 h: 100 %) y los parámetros de
 * application.properties (anticipo 30 %, entrada 15:00, salida 11:00, no-show desde las 22:00).
 */
abstract class RecorridoBase {

    /** "Ahora" de cada prueba: se restablece antes de cada una. */
    protected static final LocalDateTime INICIO = LocalDateTime.of(2027, 3, 10, 10, 0);
    protected static final LocalDate HOY = INICIO.toLocalDate();

    private static final AtomicInteger SECUENCIA = new AtomicInteger(500);

    @Autowired private WebApplicationContext contexto;
    @Autowired @Qualifier("springSecurityFilterChain") private Filter filtrosDeSeguridad;
    @Autowired protected RelojMutable reloj;
    protected MockMvc mvc;

    @BeforeEach
    void prepararRecorrido() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).addFilters(filtrosDeSeguridad).build();
        reloj.fijarEn(INICIO);
    }

    // ---------- sesión ----------

    protected String token(String correo, String contrasena) throws Exception {
        String cuerpo = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"contrasena\":\"" + contrasena + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(cuerpo, "$.token");
    }

    protected String admin() throws Exception { return token("admin@sga.local", "Admin2026"); }
    protected String recepcion() throws Exception { return token("recepcion@sga.local", "Recepcion2026"); }
    protected String servicio() throws Exception { return token("servicio@sga.local", "Servicio2026"); }

    /** Registra (público) a un huésped nuevo y devuelve {token, documento, nombre}. */
    protected String[] huespedNuevo() throws Exception {
        int n = SECUENCIA.incrementAndGet();
        String documento = "88" + n;
        String correo = "recorrido" + n + "@correo.com";
        String nombre = "Huesped Recorrido " + n;
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"" + nombre + "\",\"correo\":\"" + correo
                                + "\",\"contrasena\":\"Recorrido2026Clave\",\"telefono\":\"3200000" + n
                                + "\",\"documento\":\"" + documento + "\",\"fechaNacimiento\":\"1990-01-01\"}"))
                .andExpect(status().isCreated());
        return new String[]{token(correo, "Recorrido2026Clave"), documento, nombre};
    }

    // ---------- peticiones ----------

    protected ResultActions consultar(String ruta, String token) throws Exception {
        return mvc.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, token));
    }

    protected ResultActions enviar(MockHttpServletRequestBuilder peticion, String token, String cuerpo) throws Exception {
        return mvc.perform(peticion.contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, token).content(cuerpo));
    }

    protected ResultActions crear(String ruta, String token, String cuerpo) throws Exception {
        return enviar(post(ruta), token, cuerpo);
    }

    protected ResultActions actualizar(String ruta, String token, String cuerpo) throws Exception {
        return enviar(put(ruta), token, cuerpo);
    }

    /** PUT sin cuerpo (las transiciones de estado operativo no lo llevan). */
    protected ResultActions actualizar(String ruta, String token) throws Exception {
        return mvc.perform(put(ruta).header(HttpHeaders.AUTHORIZATION, token));
    }

    // ---------- JSON ----------

    protected static String persona(String documento, String nombre, String nacimiento) {
        return "{\"documento\":\"" + documento + "\",\"nombre\":\"" + nombre + "\",\"fechaNacimiento\":\"" + nacimiento + "\"}";
    }

    protected static final String ADULTO_1 = persona("CC-7001", "Adulto Uno", "1990-01-01");
    protected static final String ADULTO_2 = persona("CC-7002", "Adulto Dos", "1988-02-02");

    protected static String grupo(String... ocupantes) {
        return "[" + String.join(",", ocupantes) + "]";
    }

    protected static String cotizacion(String apartamento, String entrada, String salida, String... ocupantes) {
        return "{\"identificacionApartamento\":\"" + apartamento + "\",\"fechaEntrada\":\"" + entrada
                + "\",\"fechaSalida\":\"" + salida + "\",\"ocupantes\":" + grupo(ocupantes) + "}";
    }

    protected static String reserva(String apartamento, String entrada, String salida, String canal, String titular,
                                    String... ocupantes) {
        return "{\"identificacionApartamento\":\"" + apartamento + "\",\"fechaEntrada\":\"" + entrada
                + "\",\"fechaSalida\":\"" + salida + "\",\"canalOrigen\":\"" + canal + "\",\"titular\":" + titular
                + ",\"ocupantes\":" + grupo(ocupantes) + "}";
    }

    protected static String reservaExterna(String apartamento, String entrada, String salida, String identificador,
                                           String titular) {
        return "{\"identificacionApartamento\":\"" + apartamento + "\",\"fechaEntrada\":\"" + entrada
                + "\",\"fechaSalida\":\"" + salida + "\",\"canalOrigen\":\"EXTERNO\","
                + (identificador == null ? "" : "\"identificadorExterno\":\"" + identificador + "\",")
                + "\"titular\":" + titular + ",\"ocupantes\":[" + titular + "]}";
    }

    protected static String pago(String medio, LocalDate fecha, long valor) {
        return "{\"medio\":\"" + medio + "\",\"fecha\":\"" + fecha + "\",\"valor\":" + valor + "}";
    }

    protected static String autor(String autor) {
        return "{\"autor\":\"" + autor + "\"}";
    }

    protected static String confirmacion(String hora, String autor) {
        return "{\"horaEstimadaLlegada\":\"" + hora + "\",\"autor\":\"" + autor + "\"}";
    }

    // ---------- lectura de cuerpos ----------

    protected static String cuerpo(ResultActions respuesta) throws Exception {
        return respuesta.andReturn().getResponse().getContentAsString();
    }

    protected static String texto(String json, String ruta) {
        return JsonPath.read(json, ruta);
    }

    protected static long numero(String json, String ruta) {
        return ((Number) JsonPath.read(json, ruta)).longValue();
    }

    // ---------- aserción común de todo 409 ----------

    /**
     * Un 409 de regla de negocio: {@code $.status} = 409, {@code $.mensaje} igual al texto de la regla, {@code $.errores}
     * nulo (solo los 400 de Bean Validation lo llenan) y nada técnico en el cuerpo: ni "trace", ni "exception", ni
     * nombres de clase Java.
     */
    protected static void es409(ResultActions respuesta, String mensajeExacto) throws Exception {
        verificar409(respuesta).andExpect(jsonPath("$.mensaje").value(mensajeExacto));
    }

    /** Igual que {@link #es409} cuando el mensaje lleva datos variables (códigos, valores): se verifica un fragmento. */
    protected static void es409Con(ResultActions respuesta, String fragmento) throws Exception {
        verificar409(respuesta).andExpect(jsonPath("$.mensaje", containsString(fragmento)));
    }

    private static ResultActions verificar409(ResultActions respuesta) throws Exception {
        MvcResult resultado = respuesta
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.errores").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andReturn();
        String cuerpo = resultado.getResponse().getContentAsString();
        String enMinusculas = cuerpo.toLowerCase();
        assertFalse(enMinusculas.contains("trace"), "el cuerpo no debe traer trazas: " + cuerpo);
        assertFalse(enMinusculas.contains("exception"), "el cuerpo no debe nombrar excepciones: " + cuerpo);
        assertFalse(cuerpo.contains("co.edu.uniquindio") || cuerpo.contains("java.") || cuerpo.contains("org.springframework"),
                "el cuerpo no debe nombrar clases Java: " + cuerpo);
        assertFalse(cuerpo.contains("\tat ") || cuerpo.contains("\\tat "), "el cuerpo no debe traer pilas de llamadas: " + cuerpo);
        return respuesta;
    }
}
