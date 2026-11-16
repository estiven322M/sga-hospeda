package co.edu.uniquindio.sga.infrastructure.notificaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import co.edu.uniquindio.sga.domain.exception.NotificacionFallidaException;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.valueobject.CorreoElectronico;
import co.edu.uniquindio.sga.domain.valueobject.EstadoNotificacion;
import co.edu.uniquindio.sga.domain.valueobject.Notificacion;
import co.edu.uniquindio.sga.domain.valueobject.RegistroNotificacion;
import co.edu.uniquindio.sga.domain.valueobject.TipoNotificacion;

/**
 * Adaptador de Brevo del puerto Notificador (I-01 a I-08): habla con un servidor simulado en 127.0.0.1, nunca con Brevo. Se comprueba
 * la petición (ruta, encabezado api-key, JSON), el tiempo de espera, el fallo y que el código de recuperación no quede en la bandeja.
 */
class NotificadorBrevoTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-16T15:00:00Z"), ZoneId.of("America/Bogota"));
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 16, 10, 0);

    private final BandejaEnMemoria bandeja = new BandejaEnMemoria();
    private HttpServer servidor;
    private final AtomicReference<String> cuerpo = new AtomicReference<>();
    private final AtomicReference<String> ruta = new AtomicReference<>();
    private final AtomicReference<String> clave = new AtomicReference<>();
    private final AtomicReference<String> tipoContenido = new AtomicReference<>();

    @BeforeEach
    void levantar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    }

    @AfterEach
    void detener() {
        servidor.stop(0);
    }

    private void responder(int estado, String respuesta, long demoraMs) {
        servidor.createContext("/", intercambio -> {
            ruta.set(intercambio.getRequestMethod() + " " + intercambio.getRequestURI().getPath());
            clave.set(intercambio.getRequestHeaders().getFirst("api-key"));
            tipoContenido.set(intercambio.getRequestHeaders().getFirst("content-type"));
            cuerpo.set(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            if (demoraMs > 0) {
                try {
                    Thread.sleep(demoraMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
            try {
                intercambio.sendResponseHeaders(estado, bytes.length);
                intercambio.getResponseBody().write(bytes);
            } catch (IOException ignorada) {
                // el cliente ya se fue: es lo que se prueba con el tiempo de espera
            } finally {
                intercambio.close();
            }
        });
        servidor.start();
    }

    private NotificadorBrevo notificador(long tiempoEsperaMs) {
        return new NotificadorBrevo(bandeja, RELOJ, "clave-de-prueba", "sga@prueba.local", "SGA Hospeda",
                "http://127.0.0.1:" + servidor.getAddress().getPort(), tiempoEsperaMs);
    }

    private static Notificacion reserva() {
        return new Notificacion(TipoNotificacion.RESERVA_CREADA, new CorreoElectronico("laura@correo.com"),
                "Reserva \"RES-1\" registrada", "Hola Laura,\nsu reserva quedó registrada. Saludos \\ el equipo", null, AHORA);
    }

    private static Notificacion codigo() {
        return new Notificacion(TipoNotificacion.CODIGO_RECUPERACION, new CorreoElectronico("laura@correo.com"), "Código", "Su código es 123456.", null, AHORA);
    }

    private RegistroNotificacion ultimo() {
        return bandeja.listar(SolicitudPagina.primera()).contenido().get(0);
    }

    @Test
    void envia_a_la_api_de_brevo_con_la_clave_en_el_encabezado_y_un_json_correcto() {
        responder(201, "{\"messageId\":\"<abc@brevo>\"}", 0);

        notificador(5000).enviar(reserva());

        assertEquals("POST /v3/smtp/email", ruta.get());
        assertEquals("clave-de-prueba", clave.get());
        assertEquals("application/json", tipoContenido.get());
        String json = cuerpo.get();
        assertTrue(json.contains("\"sender\":{\"name\":\"SGA Hospeda\",\"email\":\"sga@prueba.local\"}"), json);
        assertTrue(json.contains("\"to\":[{\"email\":\"laura@correo.com\"}]"), json);
        assertTrue(json.contains("\"subject\":\"Reserva \\\"RES-1\\\" registrada\""), "las comillas se escapan: " + json);
        assertTrue(json.contains("\"textContent\":\"Hola Laura,\\nsu reserva quedó registrada. Saludos \\\\ el equipo\""), "saltos y barras escapados: " + json);
        assertFalse(json.contains("clave-de-prueba"), "la clave va en el encabezado, no en el cuerpo");
        assertEquals(EstadoNotificacion.ENVIADA, ultimo().estado());
    }

    @Test
    void cualquier_2xx_es_un_envio_correcto() {
        responder(202, "{}", 0);

        notificador(5000).enviar(reserva());

        assertEquals(EstadoNotificacion.ENVIADA, ultimo().estado());
    }

    @Test
    void si_brevo_rechaza_queda_fallida_con_el_codigo_y_se_lanza_la_excepcion_del_puerto() {
        responder(401, "{\"code\":\"unauthorized\",\"message\":\"Key not found\"}", 0);

        NotificacionFallidaException e = assertThrows(NotificacionFallidaException.class, () -> notificador(5000).enviar(reserva()));

        assertTrue(e.getMessage().contains("401"));
        assertEquals(EstadoNotificacion.FALLIDA, ultimo().estado());
        assertTrue(ultimo().detalle().contains("401"));
        assertFalse(ultimo().detalle().contains("clave-de-prueba"), "la clave nunca queda en la bandeja");
    }

    @Test
    void si_brevo_tarda_mas_que_el_tiempo_de_espera_falla_rapido_sin_colgarse() {
        responder(201, "{}", 2000);
        long inicio = System.nanoTime();

        assertThrows(NotificacionFallidaException.class, () -> notificador(300).enviar(reserva()));

        long ms = (System.nanoTime() - inicio) / 1_000_000;
        assertTrue(ms < 1800, "I-03: debe rendirse al vencer el tiempo máximo, tardó " + ms + " ms");
        assertEquals(EstadoNotificacion.FALLIDA, ultimo().estado());
    }

    @Test
    void sin_conexion_falla_con_la_excepcion_del_puerto() {
        // un puerto cerrado de esta misma máquina: nunca se llama a Brevo
        NotificadorBrevo sinServidor = new NotificadorBrevo(bandeja, RELOJ, "k", "sga@prueba.local", "SGA", "http://127.0.0.1:1", 1000);

        assertThrows(NotificacionFallidaException.class, () -> sinServidor.enviar(reserva()));

        assertEquals(EstadoNotificacion.FALLIDA, ultimo().estado());
    }

    @Test
    void el_codigo_de_recuperacion_viaja_en_el_correo_pero_no_queda_en_la_bandeja() {
        responder(201, "{}", 0);

        notificador(5000).enviar(codigo());

        assertTrue(cuerpo.get().contains("123456"), "el código sí viaja en el correo");
        assertEquals(Notificacion.CONTENIDO_OMITIDO, ultimo().mensaje());
    }

    @Test
    void un_aviso_sensible_fallido_tampoco_deja_el_codigo_en_la_bandeja() {
        responder(500, "{}", 0);

        assertThrows(NotificacionFallidaException.class, () -> notificador(5000).enviar(codigo()));

        assertEquals(Notificacion.CONTENIDO_OMITIDO, ultimo().mensaje());
    }

    @Test
    void sin_clave_o_sin_remitente_la_aplicacion_no_arranca_y_dice_que_falta() {
        IllegalStateException sinClave = assertThrows(IllegalStateException.class,
                () -> new NotificadorBrevo(bandeja, RELOJ, "", "sga@prueba.local", "SGA", "https://x", 1000));
        IllegalStateException sinRemitente = assertThrows(IllegalStateException.class,
                () -> new NotificadorBrevo(bandeja, RELOJ, "k", " ", "SGA", "https://x", 1000));

        assertTrue(sinClave.getMessage().contains("brevo.api-key"));
        assertTrue(sinRemitente.getMessage().contains("brevo.remitente"));
    }

    @Test
    void el_literal_json_escapa_comillas_barras_y_caracteres_de_control() {
        assertEquals("\"a\\\"b\\\\c\\nd\\te\\u0001\"", NotificadorBrevo.texto("a\"b\\c\nd\te\u0001"));
        assertEquals("\"código ñ 😀\"", NotificadorBrevo.texto("código ñ 😀"));
    }
}
