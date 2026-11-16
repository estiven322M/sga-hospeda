package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/**
 * Una imagen de un apartamento (7.3): su identificador, su URL pública, la referencia en el almacén externo y si es la
 * principal. Retirarla es lógico: {@code retirada} la saca de la galería pero el registro se conserva. Una imagen retirada
 * nunca es la principal. Es inmutable: cambiar la principal o retirarla crea otra instancia (lo decide Apartamento).
 */
public record ImagenApartamento(String id, String url, String referencia, boolean principal, boolean retirada) {

    public ImagenApartamento {
        if (id == null || id.isBlank()) {
            throw new ReglaDominioException("La imagen debe tener un identificador");
        }
        if (url == null || url.isBlank()) {
            throw new ReglaDominioException("La imagen debe tener una URL");
        }
        if (referencia == null || referencia.isBlank()) {
            throw new ReglaDominioException("La imagen debe tener una referencia en el almacén");
        }
        if (principal && retirada) {
            throw new ReglaDominioException("Una imagen retirada no puede ser la principal");
        }
    }

    public ImagenApartamento comoPrincipal() {
        return new ImagenApartamento(id, url, referencia, true, retirada);
    }

    public ImagenApartamento sinSerPrincipal() {
        return new ImagenApartamento(id, url, referencia, false, retirada);
    }

    /** Retirarla la saca de la galería y le quita el carácter de principal. */
    public ImagenApartamento retirar() {
        return new ImagenApartamento(id, url, referencia, false, true);
    }
}
