package co.edu.uniquindio.sga.infrastructure.imagenes;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sun.net.httpserver.HttpServer;

import co.edu.uniquindio.sga.domain.exception.AlmacenamientoImagenesException;
import co.edu.uniquindio.sga.domain.valueobject.ContenidoImagen;
import co.edu.uniquindio.sga.domain.valueobject.ReferenciaImagen;
import co.edu.uniquindio.sga.testsupport.ImagenesDePrueba;

/**
 * Adaptadores del puerto AlmacenImagenes (I-01 a I-08). Ninguna prueba sale a internet: el local escribe en una carpeta temporal y el
 * de Cloudinary habla con un servidor simulado que levanta esta misma prueba en 127.0.0.1 (I-08).
 */
class AlmacenesDeImagenesTest {

    @TempDir
    Path carpeta;

    // ---------- almacén local ----------

    private AlmacenImagenesLocal local() {
        return new AlmacenImagenesLocal(carpeta.toString(), "http://localhost:8080/imagenes/");
    }

    @Test
    void elAlmacenLocalGuardaElArchivoYDevuelveSuUrlPublica() throws IOException {
        ContenidoImagen imagen = ContenidoImagen.de("fachada.png", ImagenesDePrueba.png());

        ReferenciaImagen guardada = local().guardar(imagen, "apartamentos/APT-101");

        assertTrue(guardada.url().startsWith("http://localhost:8080/imagenes/apartamentos/APT-101/"));
        assertTrue(guardada.url().endsWith(".png"));
        assertTrue(guardada.referencia().startsWith("apartamentos/APT-101/"));
        Path archivo = carpeta.resolve(guardada.referencia());
        assertTrue(Files.exists(archivo));
        assertArrayEquals(ImagenesDePrueba.png(), Files.readAllBytes(archivo));
    }

    @Test
    void cadaSubidaTieneUnNombreDistintoYLaExtensionDelFormatoReal() {
        AlmacenImagenesLocal almacen = local();

        ReferenciaImagen primera = almacen.guardar(ContenidoImagen.de("a.png", ImagenesDePrueba.png()), "perfiles");
        ReferenciaImagen segunda = almacen.guardar(ContenidoImagen.de("a.png", ImagenesDePrueba.png()), "perfiles");
        ReferenciaImagen jpg = almacen.guardar(ContenidoImagen.de("a.png", ImagenesDePrueba.jpg()), "perfiles");

        assertFalse(primera.url().equals(segunda.url()));
        assertTrue(jpg.url().endsWith(".jpg"), "la extensión sale del contenido, no del nombre que dio el cliente");
    }

    @Test
    void laCarpetaSeSaneaYNuncaSaleDeLaRaiz() {
        ReferenciaImagen guardada = local().guardar(ContenidoImagen.de("a.png", ImagenesDePrueba.png()), "../../etc/passwd");

        assertTrue(Path.of(carpeta.toString()).resolve(guardada.referencia()).normalize().startsWith(carpeta));
        assertFalse(guardada.referencia().contains(".."));
    }

    @Test
    void unNombreFijoNoSeVuelveAEscribirAlReiniciar() throws IOException {
        AlmacenImagenesLocal almacen = local();
        ReferenciaImagen primera = almacen.guardarConNombre("apartamentos/APT-101", "semilla-1.png", ImagenesDePrueba.png());
        Path archivo = carpeta.resolve(primera.referencia());
        Files.write(archivo, new byte[] {1, 2, 3});   // si se volviera a escribir, esto se perdería

        ReferenciaImagen segunda = almacen.guardarConNombre("apartamentos/APT-101", "semilla-1.png", ImagenesDePrueba.png());

        assertEquals(primera, segunda);
        assertArrayEquals(new byte[] {1, 2, 3}, Files.readAllBytes(archivo));
    }

    @Test
    void siElDiscoFallaLanzaLaExcepcionDelPuerto() throws IOException {
        Path ocupado = carpeta.resolve("ocupado");
        Files.writeString(ocupado, "soy un archivo, no una carpeta");
        AlmacenImagenesLocal almacen = new AlmacenImagenesLocal(ocupado.toString(), "http://localhost:8080/imagenes");

        assertThrows(AlmacenamientoImagenesException.class,
                () -> almacen.guardar(ContenidoImagen.de("a.png", ImagenesDePrueba.png()), "apartamentos/APT-101"));
    }

    // ---------- Cloudinary con un servidor simulado ----------

    private HttpServer servidor;
    private final AtomicReference<String> cuerpoRecibido = new AtomicReference<>();
    private final AtomicReference<String> rutaRecibida = new AtomicReference<>();

    @BeforeEach
    void levantarServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    }

    @AfterEach
    void detenerServidor() {
        servidor.stop(0);
    }

    private String urlDelServidor() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    private void responder(int estado, String cuerpo, long demoraMs) {
        servidor.createContext("/", intercambio -> {
            rutaRecibida.set(intercambio.getRequestURI().getPath());
            cuerpoRecibido.set(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1));
            if (demoraMs > 0) {
                try {
                    Thread.sleep(demoraMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
            try {
                intercambio.sendResponseHeaders(estado, bytes.length);
                intercambio.getResponseBody().write(bytes);
            } catch (IOException ignorada) {
                // el cliente ya se fue (tiempo agotado): es lo que se está probando
            } finally {
                intercambio.close();
            }
        });
        servidor.start();
    }

    private AlmacenImagenesCloudinary cloudinary(long tiempoEsperaMs) {
        return new AlmacenImagenesCloudinary("mi-nube", "clave-publica", "secreto-de-prueba", urlDelServidor(), tiempoEsperaMs);
    }

    private static final String EXITO = "{\"public_id\":\"sga/apartamentos/APT-101/abc123\","
            + "\"secure_url\":\"https:\\/\\/res.cloudinary.com\\/mi-nube\\/image\\/upload\\/v1\\/sga\\/apartamentos\\/APT-101\\/abc123.png\"}";

    @Test
    void cloudinarySubeLaImagenFirmadaYDevuelveSuUrlSegura() {
        responder(200, EXITO, 0);

        ReferenciaImagen guardada = cloudinary(5000).guardar(ContenidoImagen.de("a.png", ImagenesDePrueba.png()), "apartamentos/APT-101");

        assertEquals("https://res.cloudinary.com/mi-nube/image/upload/v1/sga/apartamentos/APT-101/abc123.png", guardada.url());
        assertEquals("sga/apartamentos/APT-101/abc123", guardada.referencia());
        assertEquals("/v1_1/mi-nube/image/upload", rutaRecibida.get());
    }

    @Test
    void laPeticionLlevaLosCamposFirmadosYElArchivo() throws Exception {
        responder(200, EXITO, 0);
        byte[] png = ImagenesDePrueba.png();

        cloudinary(5000).guardar(ContenidoImagen.de("a.png", png), "apartamentos/APT-101");

        String cuerpo = cuerpoRecibido.get();
        assertTrue(cuerpo.contains("name=\"api_key\"\r\n\r\nclave-publica"));
        assertTrue(cuerpo.contains("name=\"folder\"\r\n\r\nsga/apartamentos/APT-101"));
        assertTrue(cuerpo.contains("name=\"file\"; filename=\"imagen.png\"\r\nContent-Type: image/png"));
        assertTrue(cuerpo.contains(new String(png, StandardCharsets.ISO_8859_1)), "el archivo viaja completo");
        // la firma es el SHA-1 de "folder=...&timestamp=..." + el secreto, que NUNCA viaja
        String marca = valor(cuerpo, "timestamp");
        String firma = valor(cuerpo, "signature");
        String esperada = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(
                ("folder=sga/apartamentos/APT-101&timestamp=" + marca + "secreto-de-prueba").getBytes(StandardCharsets.UTF_8)));
        assertEquals(esperada, firma);
        assertFalse(cuerpo.contains("secreto-de-prueba"), "el secreto nunca se envía");
    }

    private static String valor(String cuerpo, String campo) {
        Matcher m = Pattern.compile("name=\"" + campo + "\"\r\n\r\n([^\r]+)").matcher(cuerpo);
        assertTrue(m.find(), "falta el campo " + campo);
        return m.group(1);
    }

    @Test
    void siCloudinaryRechazaLasCredencialesLanzaLaExcepcionDelPuerto() {
        responder(401, "{\"error\":{\"message\":\"Invalid Signature\"}}", 0);

        AlmacenamientoImagenesException e = assertThrows(AlmacenamientoImagenesException.class,
                () -> cloudinary(5000).guardar(ContenidoImagen.de("a.png", ImagenesDePrueba.png()), "apartamentos/APT-101"));

        assertTrue(e.getMessage().contains("401"));
        assertFalse(e.getMessage().contains("secreto-de-prueba"));
    }

    @Test
    void unaRespuestaInesperadaSeTrataComoFallo() {
        responder(200, "{\"algo\":\"raro\"}", 0);

        assertThrows(AlmacenamientoImagenesException.class,
                () -> cloudinary(5000).guardar(ContenidoImagen.de("a.png", ImagenesDePrueba.png()), "apartamentos/APT-101"));
    }

    @Test
    void siElServicioTardaMasQueElTiempoDeEsperaFallaRapidoSinColgarse() {
        responder(200, EXITO, 2000);
        long inicio = System.nanoTime();

        assertThrows(AlmacenamientoImagenesException.class,
                () -> cloudinary(300).guardar(ContenidoImagen.de("a.png", ImagenesDePrueba.png()), "apartamentos/APT-101"));

        long milisegundos = (System.nanoTime() - inicio) / 1_000_000;
        assertTrue(milisegundos < 1800, "I-03: debe rendirse al vencer el tiempo máximo, tardó " + milisegundos + " ms");
    }

    @Test
    void siNoHayConexionFallaConLaExcepcionDelPuerto() {
        // un puerto cerrado de esta misma máquina: nunca se llama a un servicio externo
        AlmacenImagenesCloudinary sinServidor = new AlmacenImagenesCloudinary("n", "k", "s", "http://127.0.0.1:1", 1000);

        assertThrows(AlmacenamientoImagenesException.class,
                () -> sinServidor.guardar(ContenidoImagen.de("a.png", ImagenesDePrueba.png()), "apartamentos/APT-101"));
    }

    // ---------- elección del proveedor ----------

    private ImagenesConfig config() {
        return new ImagenesConfig(local());
    }

    @Test
    void elProveedorLocalEsElPorDefecto() {
        assertEquals(local().getRaiz(), ((AlmacenImagenesLocal) config().almacenImagenes("local", 1000, "https://x", "", "", "")).getRaiz());
        assertTrue(config().almacenImagenes("LOCAL ", 1000, "https://x", "", "", "") instanceof AlmacenImagenesLocal);
    }

    @Test
    void conCloudinarySeCreaElAdaptadorReal() {
        assertTrue(config().almacenImagenes("cloudinary", 1000, "https://x", "n", "k", "s") instanceof AlmacenImagenesCloudinary);
    }

    @Test
    void cloudinarySinCredencialesImpideArrancarYDiceCualesFaltan() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> config().almacenImagenes("cloudinary", 1000, "https://x", "nube", "", ""));

        assertTrue(e.getMessage().contains("cloud-name"));
        assertTrue(e.getMessage().contains("api-secret"));
    }

    @Test
    void unProveedorDesconocidoImpideArrancar() {
        assertThrows(IllegalStateException.class, () -> config().almacenImagenes("dropbox", 1000, "https://x", "", "", ""));
    }
}
