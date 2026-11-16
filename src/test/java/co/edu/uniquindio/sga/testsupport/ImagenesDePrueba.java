package co.edu.uniquindio.sga.testsupport;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import co.edu.uniquindio.sga.domain.valueobject.ContenidoImagen;
import co.edu.uniquindio.sga.domain.valueobject.ReferenciaImagen;

/** Imágenes mínimas para las pruebas: bytes con la firma real de cada formato, sin depender de archivos ni de internet. */
public final class ImagenesDePrueba {

    private ImagenesDePrueba() {
    }

    /** Un PNG de verdad (2x2 píxeles). */
    public static byte[] png() {
        return codificar("png");
    }

    /** Un JPG de verdad (2x2 píxeles). */
    public static byte[] jpg() {
        return codificar("jpg");
    }

    /** Un contenedor WEBP mínimo: solo necesita la firma RIFF....WEBP para que el sistema lo reconozca. */
    public static byte[] webp() {
        byte[] bytes = new byte[32];
        byte[] cabecera = {'R', 'I', 'F', 'F', 24, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '};
        System.arraycopy(cabecera, 0, bytes, 0, cabecera.length);
        return bytes;
    }

    /** Un archivo que empieza como PNG y pesa exactamente {@code bytes}: para probar los tamaños máximos. */
    public static byte[] pngDe(int bytes) {
        byte[] contenido = new byte[bytes];
        byte[] firma = png();
        System.arraycopy(firma, 0, contenido, 0, Math.min(8, firma.length));
        Arrays.fill(contenido, 8, bytes, (byte) 7);
        return contenido;
    }

    /** Texto que se hace pasar por imagen (por ejemplo, un .jpg renombrado). */
    public static byte[] texto() {
        return "esto no es una imagen".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public static ContenidoImagen contenidoPng(String nombre) {
        return ContenidoImagen.de(nombre, png());
    }

    public static ReferenciaImagen referencia(int numero) {
        return new ReferenciaImagen("http://localhost:8080/imagenes/apartamentos/APT-101/prueba-" + numero + ".png",
                "apartamentos/APT-101/prueba-" + numero + ".png");
    }

    private static byte[] codificar(String formato) {
        BufferedImage imagen = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        imagen.setRGB(0, 0, 0xCC3333);
        imagen.setRGB(1, 1, 0x3366CC);
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(imagen, formato, salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
