package co.edu.uniquindio.sga.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.entity.ServicioAdicional;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ServicioAdicionalRepository;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;

/** L-17: el código de un servicio adicional es único. */
class RegistroServicioAdicionalServiceTest {

    private final ServicioAdicionalRepository repositorio = mock(ServicioAdicionalRepository.class);
    private final RegistroServicioAdicionalService servicio = new RegistroServicioAdicionalService(repositorio);

    @Test
    void creaUnServicioConUnCodigoNuevo() {
        when(repositorio.buscarPorCodigo("DESAYUNO")).thenReturn(Optional.empty());

        ServicioAdicional creado = servicio.crear("desayuno", "Desayuno", "", true, Dinero.de(25_000));

        assertEquals("DESAYUNO", creado.getCodigo());
    }

    @Test
    void rechazaUnCodigoRepetidoSinDistinguirMayusculas() {
        when(repositorio.buscarPorCodigo("DESAYUNO")).thenReturn(Optional.of(
                new ServicioAdicional("DESAYUNO", "Desayuno", "", true, Dinero.de(25_000))));

        ReglaDominioException e = assertThrows(ReglaDominioException.class,
                () -> servicio.crear("Desayuno", "Otro", "", true, Dinero.de(1)));

        assertEquals("Ya existe un servicio adicional con el código DESAYUNO", e.getMessage());
    }
}
