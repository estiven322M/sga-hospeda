package co.edu.uniquindio.sga.infrastructure.imagenes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.edu.uniquindio.sga.domain.exception.AlmacenamientoImagenesException;
import co.edu.uniquindio.sga.domain.repository.AlmacenImagenes;
import co.edu.uniquindio.sga.domain.valueobject.ContenidoImagen;
import co.edu.uniquindio.sga.domain.valueobject.ReferenciaImagen;

/**
 * Adaptador REAL del puerto AlmacenImagenes: sube las imágenes a Cloudinary (plan gratuito, sin tarjeta, I-07) con su API de carga
 * firmada ({@code POST /v1_1/{cloud}/image/upload}). Se activa con {@code sga.imagenes.proveedor=cloudinary}.
 * <ul>
 *   <li>Tiempo de espera (I-03): {@code sga.imagenes.tiempo-espera-ms} se aplica a la conexión y a la petición completa.</li>
 *   <li>Fallo (I-04, I-05): cualquier error (red, tiempo agotado, credenciales rechazadas, respuesta inesperada) se registra en el
 *       log y se lanza AlmacenamientoImagenesException; la API responde 503 al administrador y nada más se ve afectado.</li>
 *   <li>Credenciales (I-06): llegan por configuración externa (application-secrets.properties o variables de entorno) y nunca se
 *       escriben en el log.</li>
 *   <li>Se usa el cliente HTTP del JDK (sin dependencias nuevas) y la URL de la API es configurable, así las pruebas apuntan a un
 *       servidor simulado en esta misma máquina y no llaman al servicio real (I-08).</li>
 * </ul>
 * El retiro de una imagen es lógico, por eso este adaptador no borra nada de Cloudinary.
 */
public class AlmacenImagenesCloudinary implements AlmacenImagenes {

    private static final Logger log = LoggerFactory.getLogger(AlmacenImagenesCloudinary.class);
    private static final Pattern URL_SEGURA = Pattern.compile("\"secure_url\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ID_PUBLICO = Pattern.compile("\"public_id\"\\s*:\\s*\"([^\"]+)\"");
    private static final String CARPETA_RAIZ = "sga";

    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final String urlApi;
    private final Duration tiempoEspera;
    private final HttpClient cliente;

    public AlmacenImagenesCloudinary(String cloudName, String apiKey, String apiSecret, String urlApi, long tiempoEsperaMs) {
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.urlApi = urlApi.endsWith("/") ? urlApi.substring(0, urlApi.length() - 1) : urlApi;
        this.tiempoEspera = Duration.ofMillis(tiempoEsperaMs);
        this.cliente = HttpClient.newBuilder().connectTimeout(tiempoEspera).build();
    }

    @Override
    public ReferenciaImagen guardar(ContenidoImagen contenido, String carpeta) {
        String carpetaRemota = CARPETA_RAIZ + "/" + carpeta;
        String marcaDeTiempo = String.valueOf(System.currentTimeMillis() / 1000);
        // Firma de Cloudinary: los parámetros a firmar, ordenados alfabéticamente, unidos con & y seguidos del secreto (SHA-1)
        String firma = sha1("folder=" + carpetaRemota + "&timestamp=" + marcaDeTiempo + apiSecret);
        String frontera = "----sga" + UUID.randomUUID();
        try {
            byte[] cuerpo = armarCuerpo(frontera, contenido, carpetaRemota, marcaDeTiempo, firma);
            HttpRequest peticion = HttpRequest.newBuilder(URI.create(urlApi + "/v1_1/" + cloudName + "/image/upload"))
                    .timeout(tiempoEspera)
                    .header("Content-Type", "multipart/form-data; boundary=" + frontera)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(cuerpo))
                    .build();
            HttpResponse<String> respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (respuesta.statusCode() != 200) {
                log.warn("Cloudinary rechazó la subida (HTTP {}): {}", respuesta.statusCode(), recortar(respuesta.body()));
                throw new AlmacenamientoImagenesException("El servicio de imágenes rechazó la subida (HTTP " + respuesta.statusCode() + ")");
            }
            return leerRespuesta(respuesta.body());
        } catch (AlmacenamientoImagenesException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AlmacenamientoImagenesException("La subida de la imagen fue interrumpida", e);
        } catch (IOException | RuntimeException e) {
            // Tiempo agotado, sin conexión, URL inválida... Se registra el tipo de fallo (nunca las credenciales)
            log.warn("No se pudo subir la imagen a Cloudinary: {} {}", e.getClass().getSimpleName(), e.getMessage());
            throw new AlmacenamientoImagenesException("No se pudo subir la imagen al servicio de imágenes", e);
        }
    }

    private ReferenciaImagen leerRespuesta(String json) {
        Matcher url = URL_SEGURA.matcher(json);
        Matcher id = ID_PUBLICO.matcher(json);
        if (!url.find() || !id.find()) {
            log.warn("Respuesta inesperada de Cloudinary: {}", recortar(json));
            throw new AlmacenamientoImagenesException("El servicio de imágenes respondió algo inesperado");
        }
        // En JSON las barras pueden venir escapadas (https:\/\/...)
        return new ReferenciaImagen(url.group(1).replace("\\/", "/"), id.group(1).replace("\\/", "/"));
    }

    private byte[] armarCuerpo(String frontera, ContenidoImagen contenido, String carpeta, String marcaDeTiempo, String firma)
            throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        campo(salida, frontera, "api_key", apiKey);
        campo(salida, frontera, "timestamp", marcaDeTiempo);
        campo(salida, frontera, "folder", carpeta);
        campo(salida, frontera, "signature", firma);
        salida.write(("--" + frontera + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"imagen."
                + contenido.tipo().extension() + "\"\r\nContent-Type: " + contenido.tipo().tipoMime() + "\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        salida.write(contenido.bytes());
        salida.write(("\r\n--" + frontera + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return salida.toByteArray();
    }

    private static void campo(ByteArrayOutputStream salida, String frontera, String nombre, String valor) throws IOException {
        salida.write(("--" + frontera + "\r\nContent-Disposition: form-data; name=\"" + nombre + "\"\r\n\r\n" + valor + "\r\n")
                .getBytes(StandardCharsets.UTF_8));
    }

    private static String sha1(String texto) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 no está disponible", e);
        }
    }

    private static String recortar(String texto) {
        return texto == null ? "" : texto.length() > 300 ? texto.substring(0, 300) + "…" : texto;
    }
}
