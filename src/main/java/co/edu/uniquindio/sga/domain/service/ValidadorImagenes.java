package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.ContenidoImagen;

/**
 * 7.3 y 7.2: el tamaño máximo de una imagen. Las de los apartamentos admiten hasta 5 MB y la foto de perfil hasta 2 MB; los dos
 * límites son parámetros de application.properties (regla 10), no constantes del código. El formato (JPG, PNG o WEBP) ya lo
 * garantiza ContenidoImagen al crearse.
 */
public class ValidadorImagenes {

    private static final long MEGABYTE = 1024L * 1024L;

    private final long maximoApartamentoBytes;
    private final long maximoPerfilBytes;

    public ValidadorImagenes(long maximoApartamentoBytes, long maximoPerfilBytes) {
        if (maximoApartamentoBytes < 1 || maximoPerfilBytes < 1) {
            throw new ReglaDominioException("Los tamaños máximos de imagen deben ser mayores que cero");
        }
        this.maximoApartamentoBytes = maximoApartamentoBytes;
        this.maximoPerfilBytes = maximoPerfilBytes;
    }

    public void validarParaApartamento(ContenidoImagen imagen) {
        validar(imagen, maximoApartamentoBytes);
    }

    public void validarParaPerfil(ContenidoImagen imagen) {
        validar(imagen, maximoPerfilBytes);
    }

    private static void validar(ContenidoImagen imagen, long maximoBytes) {
        if (imagen.tamanoBytes() > maximoBytes) {
            throw new ReglaDominioException("La imagen " + imagen.nombre() + " pesa más de " + aTexto(maximoBytes)
                    + " (el máximo permitido)");
        }
    }

    private static String aTexto(long bytes) {
        return bytes % MEGABYTE == 0 ? (bytes / MEGABYTE) + " MB" : (bytes / 1024) + " KB";
    }
}
