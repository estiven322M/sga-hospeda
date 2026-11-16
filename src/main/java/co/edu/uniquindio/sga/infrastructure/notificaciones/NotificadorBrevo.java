package co.edu.uniquindio.sga.infrastructure.notificaciones;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import co.edu.uniquindio.sga.domain.exception.NotificacionFallidaException;
import co.edu.uniquindio.sga.domain.repository.BandejaNotificaciones;
import co.edu.uniquindio.sga.domain.repository.Notificador;
import co.edu.uniquindio.sga.domain.valueobject.EstadoNotificacion;
import co.edu.uniquindio.sga.domain.valueobject.Notificacion;
import co.edu.uniquindio.sga.domain.valueobject.RegistroNotificacion;

/**
 * Tercer adaptador del puerto Notificador: envía por la API HTTP de Brevo ({@code POST /v3/smtp/email}, encabezado {@code api-key}).
 * Existe porque algunos alojamientos en la nube gratuitos (Render, por ejemplo) bloquean los puertos SMTP, pero dejan salir HTTPS. Se
 * activa con {@code sga.notificaciones.proveedor=brevo}; el local sigue siendo el de por defecto.
 * <ul>
 *   <li>Tiempo de espera (I-03): {@code sga.notificaciones.tiempo-espera-ms} (5000) para la conexión y la petición.</li>
 *   <li>Fallo (I-04, I-05): igual que el SMTP: queda FALLIDA con su causa en la bandeja, se registra en el log y se lanza
 *       NotificacionFallidaException, que quien notifica captura; la operación de negocio continúa.</li>
 *   <li>Credenciales (I-06): {@code sga.notificaciones.brevo.api-key} y {@code sga.notificaciones.brevo.remitente} salen de
 *       application-secrets.properties o de variables de entorno; la clave nunca se escribe en el log ni en la bandeja.</li>
 *   <li>Cliente HTTP del JDK, sin dependencias nuevas; la URL de la API es configurable para que las pruebas usen un servidor
 *       simulado (I-08). El código de recuperación viaja en el correo pero en la bandeja solo queda "(contenido sensible omitido)".</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "sga.notificaciones.proveedor", havingValue = "brevo")
public class NotificadorBrevo implements Notificador {

    private static final Logger log = LoggerFactory.getLogger(NotificadorBrevo.class);

    private final BandejaNotificaciones bandeja;
    private final Clock reloj;
    private final String apiKey;
    private final String remitente;
    private final String nombreRemitente;
    private final String urlApi;
    private final Duration tiempoEspera;
    private final HttpClient cliente;

    public NotificadorBrevo(BandejaNotificaciones bandeja, Clock reloj,
                            @Value("${sga.notificaciones.brevo.api-key:}") String apiKey,
                            @Value("${sga.notificaciones.brevo.remitente:}") String remitente,
                            @Value("${sga.notificaciones.brevo.nombre-remitente:SGA Hospeda}") String nombreRemitente,
                            @Value("${sga.notificaciones.brevo.url-api:https://api.brevo.com}") String urlApi,
                            @Value("${sga.notificaciones.tiempo-espera-ms:5000}") long tiempoEsperaMs) {
        if (apiKey.isBlank() || remitente.isBlank()) {
            // Mejor fallar al arrancar que descubrirlo con el primer aviso
            throw new IllegalStateException("sga.notificaciones.proveedor=brevo exige sga.notificaciones.brevo.api-key y "
                    + "sga.notificaciones.brevo.remitente (en application-secrets.properties o como variables de entorno)");
        }
        this.bandeja = bandeja;
        this.reloj = reloj;
        this.apiKey = apiKey;
        this.remitente = remitente;
        this.nombreRemitente = nombreRemitente;
        this.urlApi = urlApi.endsWith("/") ? urlApi.substring(0, urlApi.length() - 1) : urlApi;
        this.tiempoEspera = Duration.ofMillis(tiempoEsperaMs);
        this.cliente = HttpClient.newBuilder().connectTimeout(tiempoEspera).build();
    }

    @Override
    public void enviar(Notificacion notificacion) {
        String causa;
        try {
            HttpRequest peticion = HttpRequest.newBuilder(URI.create(urlApi + "/v3/smtp/email"))
                    .timeout(tiempoEspera)
                    .header("api-key", apiKey)
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(cuerpoJson(notificacion), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (respuesta.statusCode() / 100 == 2) {
                bandeja.registrar(RegistroNotificacion.de(notificacion, notificacion.mensajeRegistrable(), EstadoNotificacion.ENVIADA, null,
                        LocalDateTime.now(reloj)));
                return;
            }
            log.warn("Brevo rechazó el envío de {} (HTTP {}): {}", notificacion.tipo(), respuesta.statusCode(), recortar(respuesta.body()));
            causa = "El servicio de correo respondió HTTP " + respuesta.statusCode();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            causa = "El envío fue interrumpido";
        } catch (IOException | RuntimeException e) {
            log.warn("No se pudo llamar a Brevo para enviar {}: {} {}", notificacion.tipo(), e.getClass().getSimpleName(), e.getMessage());
            causa = "No se pudo enviar el correo: " + e.getClass().getSimpleName();
        }
        bandeja.registrar(RegistroNotificacion.de(notificacion, notificacion.mensajeRegistrable(), EstadoNotificacion.FALLIDA, causa,
                LocalDateTime.now(reloj)));
        throw new NotificacionFallidaException(causa);
    }

    private String cuerpoJson(Notificacion n) {
        return "{\"sender\":{\"name\":" + texto(nombreRemitente) + ",\"email\":" + texto(remitente) + "},"
                + "\"to\":[{\"email\":" + texto(n.destinatario().valor()) + "}],"
                + "\"subject\":" + texto(n.asunto()) + ",\"textContent\":" + texto(n.mensaje()) + "}";
    }

    /** Un texto como literal JSON: escapa comillas, barras y caracteres de control. */
    static String texto(String valor) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : valor.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }

    private static String recortar(String texto) {
        return texto == null ? "" : texto.length() > 300 ? texto.substring(0, 300) + "…" : texto;
    }
}
