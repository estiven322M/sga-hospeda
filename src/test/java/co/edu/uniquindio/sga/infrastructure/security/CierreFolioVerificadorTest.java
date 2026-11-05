package co.edu.uniquindio.sga.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.application.dto.request.CerrarFolioRequest;

/** CU-11: solo el administrador envía una autorización de cierre con saldo pendiente. */
class CierreFolioVerificadorTest {

    private final CierreFolioVerificador verificador = new CierreFolioVerificador();

    @Test
    void unCierreSinAutorizacionLoPuedeHacerRecepcion() {
        assertTrue(verificador.sinAutorizacion(new CerrarFolioRequest(null, null)));
        assertTrue(verificador.sinAutorizacion(new CerrarFolioRequest(" ", "")));
        assertTrue(verificador.sinAutorizacion(null));
    }

    @Test
    void unCierreConAutorizacionExigeAlAdministrador() {
        assertFalse(verificador.sinAutorizacion(new CerrarFolioRequest("Gerencia", "Acuerdo de pago")));
        assertFalse(verificador.sinAutorizacion(new CerrarFolioRequest(null, "Acuerdo de pago")));
    }
}
