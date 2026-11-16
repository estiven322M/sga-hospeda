package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/**
 * Lo que devuelve el almacén de imágenes al guardar una: la URL pública para mostrarla y la referencia con la que el
 * almacén la identifica (en Cloudinary, su public_id; en disco, el nombre del archivo). El dominio no interpreta ninguna
 * de las dos: solo las guarda.
 */
public record ReferenciaImagen(String url, String referencia) {

    public ReferenciaImagen {
        if (url == null || url.isBlank()) {
            throw new ReglaDominioException("La imagen guardada debe tener una URL");
        }
        if (referencia == null || referencia.isBlank()) {
            throw new ReglaDominioException("La imagen guardada debe tener una referencia");
        }
    }
}
