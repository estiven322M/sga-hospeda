package co.edu.uniquindio.sga.infrastructure.config;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Component;

import co.edu.uniquindio.sga.domain.valueobject.ReferenciaImagen;
import co.edu.uniquindio.sga.infrastructure.imagenes.AlmacenImagenesLocal;

/**
 * Imágenes de ejemplo de los datos de prueba (regla 12 y 7.3: todo apartamento activo tiene al menos una). Dibuja dos ilustraciones por
 * apartamento (una de día y otra al atardecer) con Java2D, SIN texto: así no depende de las fuentes del sistema (un servidor sin
 * fuentes instaladas fallaría al dibujar texto). Se guardan SIEMPRE en el almacén local, sea cual sea el proveedor activo: sembrar
 * no debe subir nada a internet cada vez que arranca la aplicación. Los nombres de archivo son fijos, así que reiniciar no duplica.
 */
@Component
class SembradorImagenesPrueba {

    private static final int ANCHO = 800;
    private static final int ALTO = 500;
    /** Paleta de la fachada de cada apartamento (se reparte por el orden del catálogo). */
    private static final List<Color> FACHADAS = List.of(
            new Color(0x2E7D5B), new Color(0xC9772B), new Color(0x3F6FB5), new Color(0x9C4F7E),
            new Color(0x6A8E3A), new Color(0xB5473F), new Color(0x4A8C93));

    static {
        System.setProperty("java.awt.headless", "true");
    }

    private final AlmacenImagenesLocal almacen;

    SembradorImagenesPrueba(AlmacenImagenesLocal almacen) {
        this.almacen = almacen;
    }

    /** Las imágenes de ejemplo del apartamento número {@code indice} del catálogo: la primera (de día) quedará como principal. */
    List<ReferenciaImagen> paraApartamento(String identificacion, int indice) {
        Color fachada = FACHADAS.get(indice % FACHADAS.size());
        return List.of(
                guardar(identificacion, "semilla-1.png", dibujar(fachada, new Color(0x87CEEB), new Color(0xE8F6FF), false)),
                guardar(identificacion, "semilla-2.png", dibujar(fachada, new Color(0xF0A560), new Color(0x5B4B8A), true)));
    }

    private ReferenciaImagen guardar(String identificacion, String archivo, byte[] bytes) {
        return almacen.guardarConNombre("apartamentos/" + identificacion, archivo, bytes);
    }

    private static byte[] dibujar(Color fachada, Color cieloArriba, Color cieloAbajo, boolean atardecer) {
        BufferedImage imagen = new BufferedImage(ANCHO, ALTO, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, cieloArriba, 0, ALTO * 0.7f, cieloAbajo));
            g.fillRect(0, 0, ANCHO, ALTO);
            g.setColor(atardecer ? new Color(0xFFD27F) : new Color(0xFFF1A8));   // sol
            g.fillOval(620, 60, 90, 90);
            g.setColor(new Color(0x5E8B4F));                                       // monte
            g.fillOval(-150, 330, 650, 300);
            g.setColor(new Color(0x4C7A44));
            g.fillOval(350, 360, 700, 300);
            g.setColor(new Color(0x7A5C3A));                                       // suelo
            g.fillRect(0, 440, ANCHO, 60);
            g.setColor(fachada);                                                   // casa
            g.fillRect(230, 230, 340, 215);
            g.setColor(fachada.darker());
            g.fillPolygon(new int[] {205, 400, 595}, new int[] {235, 120, 235}, 3);
            g.setColor(new Color(0xFFF7D6));                                       // ventanas
            g.fillRect(265, 280, 80, 70);
            g.fillRect(455, 280, 80, 70);
            g.setColor(fachada.darker().darker());                                 // puerta
            g.fillRect(370, 330, 60, 115);
            g.setColor(new Color(0, 0, 0, 40));
            g.fillRect(0, 0, ANCHO, ALTO);
        } finally {
            g.dispose();
        }
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            ImageIO.write(imagen, "png", salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar la imagen de ejemplo", e);
        }
    }
}
