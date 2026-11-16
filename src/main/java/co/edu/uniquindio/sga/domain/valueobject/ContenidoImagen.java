package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/**
 * Los bytes de una imagen que se va a guardar, con su nombre original y su formato ya verificado (JPG, PNG o WEBP).
 * Se crea con {@link #de}, que rechaza (409) todo lo que no sea una de esas tres imágenes. El tamaño máximo no se decide
 * aquí: depende de para qué sea la imagen (apartamento o perfil) y lo revisa ValidadorImagenes con el parámetro configurado.
 */
public record ContenidoImagen(String nombre, TipoImagen tipo, byte[] bytes) {

    public ContenidoImagen {
        if (nombre == null || nombre.isBlank()) {
            nombre = "imagen";
        }
        if (tipo == null) {
            throw new ReglaDominioException("Solo se admiten imágenes JPG, PNG o WEBP");
        }
        if (bytes == null || bytes.length == 0) {
            throw new ReglaDominioException("La imagen " + nombre + " está vacía");
        }
        bytes = bytes.clone();
    }

    /** Reconoce el formato por la firma del contenido; si no es JPG, PNG ni WEBP, 409. */
    public static ContenidoImagen de(String nombre, byte[] bytes) {
        String nombreVisible = nombre == null || nombre.isBlank() ? "imagen" : nombre;
        if (bytes == null || bytes.length == 0) {
            throw new ReglaDominioException("La imagen " + nombreVisible + " está vacía");
        }
        TipoImagen tipo = TipoImagen.detectar(bytes).orElseThrow(() ->
                new ReglaDominioException("Solo se admiten imágenes JPG, PNG o WEBP: " + nombreVisible + " no lo es"));
        return new ContenidoImagen(nombreVisible, tipo, bytes);
    }

    public long tamanoBytes() {
        return bytes.length;
    }

    /** Copia defensiva: el contenido es inmutable. */
    @Override
    public byte[] bytes() {
        return bytes.clone();
    }
}
