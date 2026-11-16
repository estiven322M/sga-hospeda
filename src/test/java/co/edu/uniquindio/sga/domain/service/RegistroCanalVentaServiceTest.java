package co.edu.uniquindio.sga.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.entity.CanalVenta;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.CanalVentaRepository;
import co.edu.uniquindio.sga.domain.valueobject.CredencialCanal;
import co.edu.uniquindio.sga.domain.valueobject.TipoCanalVenta;

/** 7.8: el código de un canal es único; el canal se crea con el HASH de su credencial, nunca con su texto. */
class RegistroCanalVentaServiceTest {

    private final CanalVentaRepository canales = mock(CanalVentaRepository.class);
    private final RegistroCanalVentaService servicio = new RegistroCanalVentaService(canales);

    @Test
    void creaElCanalConElHashDeLaCredencial() {
        CredencialCanal credencial = CredencialCanal.generar();
        when(canales.buscarPorCodigo("DEMO")).thenReturn(Optional.empty());

        CanalVenta canal = servicio.crear("demo", "Mi canal", TipoCanalVenta.AGENCIA_DE_VIAJES, credencial);

        assertEquals("DEMO", canal.getCodigo());
        assertEquals(credencial.hash(), canal.getCredencialHash());
        assertNotEquals(credencial.valor(), canal.getCredencialHash(), "el texto de la credencial no se guarda");
    }

    @Test
    void rechazaUnCodigoRepetidoSinDistinguirMayusculas() {
        when(canales.buscarPorCodigo("DEMO")).thenReturn(Optional.of(
                new CanalVenta("DEMO", "Ya existe", TipoCanalVenta.OTRO, CredencialCanal.de("x").hash())));

        ReglaDominioException e = assertThrows(ReglaDominioException.class,
                () -> servicio.crear("Demo", "Otro", TipoCanalVenta.OTRO, CredencialCanal.generar()));

        assertEquals("Ya existe un canal con el código DEMO", e.getMessage());
    }

    @Test
    void unCodigoInvalidoSeRechazaAntesDeConsultar() {
        assertThrows(ReglaDominioException.class, () -> servicio.crear("x", "Nombre", TipoCanalVenta.OTRO, CredencialCanal.generar()));
    }
}
