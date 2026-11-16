package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import co.edu.uniquindio.sga.domain.entity.Alojamiento;
import co.edu.uniquindio.sga.domain.entity.ServicioAdicional;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.Ubicacion;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.AlojamientoPersistenceMapper;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.ServicioAdicionalPersistenceMapper;

/** Persistir no cambia el dominio: el alojamiento (único) y los servicios adicionales viajan a la base y vuelven iguales. */
@DataJpaTest
@Import({AlojamientoJpaRepository.class, AlojamientoPersistenceMapper.class,
        ServicioAdicionalJpaRepository.class, ServicioAdicionalPersistenceMapper.class})
class AlojamientoYServiciosPersistenciaTest {

    @Autowired private AlojamientoJpaRepository alojamientos;
    @Autowired private ServicioAdicionalJpaRepository servicios;

    private static Alojamiento alojamiento(String nombre) {
        return new Alojamiento(nombre, "Descripción", "Salento", "Carrera 6 # 3-45", new Ubicacion(4.6368, -75.57),
                LocalTime.of(15, 0), LocalTime.of(11, 0), "Silencio desde las 10 p. m.", "No se aceptan mascotas.");
    }

    // ---------- alojamiento ----------

    @Test
    void sinSembrarNoHayAlojamiento() {
        assertTrue(alojamientos.obtener().isEmpty());
    }

    @Test
    void guardaYRecuperaTodosLosDatosDelAlojamiento() {
        alojamientos.guardar(alojamiento("Hospeda Salento"));

        Alojamiento leido = alojamientos.obtener().orElseThrow();

        assertEquals("Hospeda Salento", leido.getNombre());
        assertEquals("Salento", leido.getCiudad());
        assertEquals(new Ubicacion(4.6368, -75.57), leido.getUbicacion());
        assertEquals(LocalTime.of(15, 0), leido.getHoraEntrada());
        assertEquals(LocalTime.of(11, 0), leido.getHoraSalida());
        assertEquals("No se aceptan mascotas.", leido.getPoliticaMascotas());
    }

    @Test
    void guardarDeNuevoReemplazaElUnicoAlojamientoSinDuplicarlo() {
        alojamientos.guardar(alojamiento("Primero"));
        alojamientos.guardar(alojamiento("Segundo"));

        assertEquals("Segundo", alojamientos.obtener().orElseThrow().getNombre());
    }

    // ---------- servicios adicionales ----------

    private static ServicioAdicional servicio(String codigo, boolean genera) {
        return new ServicioAdicional(codigo, "Servicio " + codigo, "Descripción", genera, genera ? Dinero.de(20_000) : Dinero.CERO);
    }

    @Test
    void guardaYRecuperaUnServicioConSuValorEnPesosYSuEstado() {
        ServicioAdicional original = servicio("DESAYUNO", true);
        original.darDeBaja();
        servicios.guardar(original);

        ServicioAdicional leido = servicios.buscarPorCodigo("DESAYUNO").orElseThrow();

        assertEquals("Servicio DESAYUNO", leido.getNombre());
        assertEquals(Dinero.de(20_000), leido.getValor());
        assertTrue(leido.generaCargo());
        assertFalse(leido.isActivo()); // la baja lógica se conserva
    }

    @Test
    void buscarUnCodigoQueNoExisteDevuelveVacio() {
        assertTrue(servicios.buscarPorCodigo("NO-EXISTE").isEmpty());
    }

    @Test
    void listaDe10EnDiezOrdenadaPorCodigoYPuedeFiltrarSoloLosActivos() {
        IntStream.rangeClosed(1, 12).forEach(i -> servicios.guardar(servicio(String.format("S%02d", i), true)));
        ServicioAdicional dadoDeBaja = servicios.buscarPorCodigo("S03").orElseThrow();
        dadoDeBaja.darDeBaja();
        servicios.guardar(dadoDeBaja);

        Pagina<ServicioAdicional> primera = servicios.listarTodos(new SolicitudPagina(0));
        Pagina<ServicioAdicional> segunda = servicios.listarTodos(new SolicitudPagina(1));
        Pagina<ServicioAdicional> activos = servicios.listarActivos(new SolicitudPagina(0));

        assertEquals(12, primera.totalElementos());
        assertEquals(2, primera.totalPaginas());
        assertEquals(10, primera.contenido().size());
        assertEquals("S01", primera.contenido().get(0).getCodigo());
        assertEquals(List.of("S11", "S12"), segunda.contenido().stream().map(ServicioAdicional::getCodigo).toList());
        assertEquals(11, activos.totalElementos());
        assertFalse(activos.contenido().stream().anyMatch(s -> s.getCodigo().equals("S03")));
    }
}
