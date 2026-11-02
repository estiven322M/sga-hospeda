package co.edu.uniquindio.sga.domain.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.*;

/** RN-16 (movimientos inversos) y 7.4 (retención como cargo, devolución como saldo a favor). */
class FolioAjustesTest {

    private Folio folioDe(long alojamiento) {
        return new Folio(new CodigoReserva("RES-2026-00001"), new Cargo("Alojamiento", Dinero.de(alojamiento)));
    }

    @Test
    void unAjusteNegativoReduceElSaldoSinEditarElCargo() {
        Folio folio = folioDe(200_000);
        folio.registrarAjuste(new Ajuste("Menos noches", Dinero.de(-50_000)));

        assertEquals(Dinero.de(150_000), folio.saldo());
        assertEquals(Dinero.de(200_000), folio.obtenerCargos().get(0).valor()); // el cargo original no cambia
        assertEquals(1, folio.obtenerAjustes().size());
    }

    @Test
    void unAjustePositivoAumentaElSaldo() {
        Folio folio = folioDe(200_000);
        folio.registrarAjuste(new Ajuste("Más noches", Dinero.de(80_000)));
        assertEquals(Dinero.de(280_000), folio.saldo());
    }

    @Test
    void cancelarSinPagosDejaSoloLaRetencionComoSaldo() {
        Folio folio = folioDe(200_000);

        folio.liquidarCancelacion(Dinero.de(100_000));

        assertEquals(Dinero.de(100_000), folio.saldo());
        assertEquals("Penalidad por cancelación", folio.obtenerCargos().get(1).concepto());
    }

    @Test
    void cancelarConAnticipoMayorQueLaRetencionDejaSaldoAFavor() {
        Folio folio = folioDe(200_000);
        folio.registrarPago(new Pago("TRANSFERENCIA", LocalDate.of(2026, 11, 3), Dinero.de(150_000)));

        folio.liquidarCancelacion(Dinero.de(50_000));

        assertEquals(Dinero.de(-100_000), folio.saldo()); // 100.000 a devolver al huésped
    }

    @Test
    void sinRetencionNoSeRegistraPenalidadYTodoLoPagadoQuedaAFavor() {
        Folio folio = folioDe(200_000);
        folio.registrarPago(new Pago("EFECTIVO", LocalDate.of(2026, 11, 3), Dinero.de(60_000)));

        folio.liquidarCancelacion(Dinero.CERO);

        assertEquals(1, folio.obtenerCargos().size());
        assertEquals(Dinero.de(-60_000), folio.saldo());
    }

    @Test
    void laRetencionNoPuedeSuperarLoCargado() {
        Folio folio = folioDe(200_000);
        assertThrows(ReglaDominioException.class, () -> folio.liquidarCancelacion(Dinero.de(200_001)));
        assertEquals(Dinero.de(200_000), folio.saldo()); // sin efectos parciales
    }

    @Test
    void laRetencionNoPuedeSerNegativaNiNula() {
        Folio folio = folioDe(200_000);
        assertThrows(ReglaDominioException.class, () -> folio.liquidarCancelacion(Dinero.de(-1)));
        assertThrows(ReglaDominioException.class, () -> folio.liquidarCancelacion(null));
    }

    @Test
    void unFolioCerradoNoAdmiteAjustesNiCancelacion() {
        Folio folio = folioDe(200_000);
        folio.cerrar("Autorizado por la administración");

        assertThrows(ReglaDominioException.class, () -> folio.registrarAjuste(new Ajuste("x", Dinero.de(1))));
        assertThrows(ReglaDominioException.class, () -> folio.liquidarCancelacion(Dinero.CERO));
    }

    @Test
    void elAjusteExigeConceptoYValor() {
        assertThrows(ReglaDominioException.class, () -> new Ajuste(" ", Dinero.de(1)));
        assertThrows(ReglaDominioException.class, () -> new Ajuste("x", null));
        assertThrows(ReglaDominioException.class, () -> folioDe(1).registrarAjuste(null));
    }
}
