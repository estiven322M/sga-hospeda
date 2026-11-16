package co.edu.uniquindio.sga.infrastructure.imagenes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import co.edu.uniquindio.sga.domain.exception.AlmacenamientoImagenesException;
import co.edu.uniquindio.sga.domain.repository.AlmacenImagenes;
import co.edu.uniquindio.sga.domain.valueobject.ContenidoImagen;
import co.edu.uniquindio.sga.domain.valueobject.ReferenciaImagen;

/**
 * Adaptador LOCAL del puerto AlmacenImagenes (I-02): guarda las imágenes en una carpeta del disco
 * ({@code sga.imagenes.local.carpeta}) que la aplicación sirve en {@code /imagenes/**}. No usa internet ni credenciales, así que el
 * sistema funciona y se prueba sin ellos. Es el adaptador activo por defecto ({@code sga.imagenes.proveedor=local}) y el que
 * guarda las imágenes de ejemplo de los datos de prueba aunque el proveedor sea otro. Como no hay servidor externo no tiene tiempo
 * de espera; si el disco falla, lanza AlmacenamientoImagenesException (la API responde 503).
 */
@Component
public class AlmacenImagenesLocal implements AlmacenImagenes {

    private static final Logger log = LoggerFactory.getLogger(AlmacenImagenesLocal.class);

    private final Path raiz;
    private final String urlBase;

    public AlmacenImagenesLocal(@Value("${sga.imagenes.local.carpeta}") String carpeta,
                                @Value("${sga.imagenes.local.url-base}") String urlBase) {
        this.raiz = Path.of(carpeta).toAbsolutePath().normalize();
        this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
        try {
            Files.createDirectories(raiz);   // la carpeta debe existir al arrancar para poder servirla en /imagenes/**
        } catch (IOException e) {
            log.warn("No se pudo crear la carpeta de imágenes {}: {}", raiz, e.getMessage());
        }
    }

    public Path getRaiz() {
        return raiz;
    }

    @Override
    public ReferenciaImagen guardar(ContenidoImagen contenido, String carpeta) {
        return escribir(carpeta, UUID.randomUUID() + "." + contenido.tipo().extension(), contenido.bytes());
    }

    /**
     * Guarda una imagen con un nombre FIJO (las imágenes de ejemplo de los datos de prueba): si ya existe no se vuelve a escribir,
     * así reiniciar la aplicación no llena el disco ni cambia las URL.
     */
    public ReferenciaImagen guardarConNombre(String carpeta, String nombreArchivo, byte[] bytes) {
        return escribir(carpeta, nombreArchivo, bytes);
    }

    private ReferenciaImagen escribir(String carpeta, String nombreArchivo, byte[] bytes) {
        String carpetaSegura = sanear(carpeta);
        Path destino = raiz.resolve(carpetaSegura).resolve(nombreArchivo).normalize();
        if (!destino.startsWith(raiz)) {
            throw new AlmacenamientoImagenesException("La ruta de la imagen no es válida");
        }
        try {
            Files.createDirectories(destino.getParent());
            if (!Files.exists(destino)) {
                Files.write(destino, bytes);
            }
        } catch (IOException e) {
            log.warn("No se pudo guardar la imagen en el disco ({}): {}", destino, e.getMessage());
            throw new AlmacenamientoImagenesException("No se pudo guardar la imagen en el disco", e);
        }
        String referencia = carpetaSegura + "/" + nombreArchivo;
        return new ReferenciaImagen(urlBase + "/" + referencia, referencia);
    }

    /** Deja solo letras, números, guion y barra: nada de "..", rutas absolutas ni caracteres raros. */
    private static String sanear(String carpeta) {
        String limpia = carpeta == null ? "" : carpeta.replaceAll("[^A-Za-z0-9_/-]", "_").replaceAll("/{2,}", "/");
        limpia = limpia.replaceAll("^/+", "").replaceAll("/+$", "");
        return limpia.isEmpty() ? "otros" : limpia;
    }
}
