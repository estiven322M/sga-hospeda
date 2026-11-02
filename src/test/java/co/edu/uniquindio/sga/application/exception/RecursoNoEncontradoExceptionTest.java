package co.edu.uniquindio.sga.application.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

/** Todas las excepciones de "no existe" comparten la base que el manejador global traduce a 404. */
class RecursoNoEncontradoExceptionTest {

    @Test
    void cadaExcepcionIndicaQueRecursoNoExiste() {
        CodigoReserva codigo = new CodigoReserva("RES-2026-00001");

        assertTrue(new ReservaNoEncontradaException(codigo).getMessage().contains("RES-2026-00001"));
        assertTrue(new FolioNoEncontradoException(codigo).getMessage().contains("RES-2026-00001"));
        assertTrue(new ApartamentoNoEncontradoException(new IdentificacionApartamento("apt-101"))
                .getMessage().contains("APT-101"));
        assertTrue(new PoliticaCancelacionNoEncontradaException("versión 2").getMessage().contains("versión 2"));
    }

    @Test
    void todasHeredanDeLaBase() {
        assertInstanceOf(RecursoNoEncontradoException.class,
                new ReservaNoEncontradaException(new CodigoReserva("RES-2026-00001")));
    }
}
