package co.edu.uniquindio.sga.domain.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/** Regla 13: paginación de 10 por página. */
class PaginaTest {

    private static java.util.List<Integer> numeros(int n) {
        return IntStream.rangeClosed(1, n).boxed().toList();
    }

    @Test
    void laPrimeraPaginaTieneDiezElementos() {
        Pagina<Integer> p = Pagina.de(numeros(25), new SolicitudPagina(0));
        assertEquals(10, p.contenido().size());
        assertEquals(25, p.totalElementos());
        assertEquals(3, p.totalPaginas());
    }

    @Test
    void laUltimaPaginaTieneElResto() {
        Pagina<Integer> p = Pagina.de(numeros(25), new SolicitudPagina(2));
        assertEquals(5, p.contenido().size());
        assertEquals(21, p.contenido().get(0));
    }

    @Test
    void unaPaginaFueraDeRangoVieneVacia() {
        assertTrue(Pagina.de(numeros(5), new SolicitudPagina(3)).contenido().isEmpty());
    }

    @Test
    void elNumeroDePaginaNoPuedeSerNegativo() {
        assertThrows(ReglaDominioException.class, () -> new SolicitudPagina(-1));
    }
}
