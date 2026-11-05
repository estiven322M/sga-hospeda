package co.edu.uniquindio.sga.infrastructure.security;

import co.edu.uniquindio.sga.application.dto.request.CerrarFolioRequest;
import org.springframework.stereotype.Component;

/**
 * CU-11 (proyecto 7.1): el recepcionista cierra folios, pero solo el ADMINISTRADOR autoriza el cierre con saldo
 * pendiente. Regla de acceso: si la petición trae una autorización (quién autoriza o motivo), la debe enviar un
 * administrador. Si el recepcionista intenta cerrar con saldo y sin autorización, decide el dominio (409).
 * <p>
 * Se usa en {@code @PreAuthorize} como {@code @cierreFolio.sinAutorizacion(#request)}.
 */
@Component("cierreFolio")
public class CierreFolioVerificador {

    public boolean sinAutorizacion(CerrarFolioRequest request) {
        return request == null || (vacio(request.autorizadoPor()) && vacio(request.motivo()));
    }

    private static boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
