package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.valueobject.EstadoOperativo;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.ApartamentoPersistenceMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import({ApartamentoJpaRepository.class, ApartamentoPersistenceMapper.class})
class ApartamentoJpaRepositoryTest {

    @Autowired
    private ApartamentoJpaRepository repositorio;

    private static IdentificacionApartamento id(String valor) {
        return new IdentificacionApartamento(valor);
    }

    private static Apartamento activo(String identificacion) {
        Apartamento apartamento = new Apartamento(id(identificacion), "Apartamento " + identificacion, 2, 4);
        apartamento.activar();
        return apartamento;
    }

    @Test
    void guardar_y_buscar_devuelve_el_apartamento_con_su_estado_activo() {
        repositorio.guardar(activo("APT-101"));

        Apartamento encontrado = repositorio.buscarPorIdentificacion(id("apt-101")).orElseThrow();

        assertTrue(encontrado.isActivo());
        assertEquals(EstadoOperativo.PREPARADO, encontrado.getEstadoOperativo());
    }

    @Test
    void buscar_un_apartamento_inexistente_devuelve_vacio() {
        assertTrue(repositorio.buscarPorIdentificacion(id("APT-999")).isEmpty());
    }

    @Test
    void guardar_un_apartamento_existente_lo_actualiza_sin_duplicarlo() {
        Apartamento apartamento = repositorio.guardar(activo("APT-101"));
        apartamento.actualizarDatos("Renombrado", 3, 8);
        apartamento.declararFueraDeServicio();

        Apartamento guardado = repositorio.guardar(apartamento);

        assertEquals("Renombrado", guardado.getNombre());
        assertEquals(EstadoOperativo.FUERA_DE_SERVICIO, guardado.getEstadoOperativo());
        assertEquals(1, repositorio.listarTodos(SolicitudPagina.primera()).totalElementos());
    }

    @Test
    void un_apartamento_dado_de_baja_sigue_existiendo_pero_no_aparece_entre_los_activos() {
        Apartamento apartamento = repositorio.guardar(activo("APT-101"));
        apartamento.darDeBaja();
        repositorio.guardar(apartamento);

        assertFalse(repositorio.buscarPorIdentificacion(id("APT-101")).orElseThrow().isActivo());
        assertTrue(repositorio.listarActivos().isEmpty());
        assertEquals(1, repositorio.listarTodos(SolicitudPagina.primera()).totalElementos());
    }

    @Test
    void listar_activos_excluye_los_inactivos_y_ordena_por_identificacion() {
        repositorio.guardar(activo("APT-300"));
        repositorio.guardar(activo("APT-100"));
        repositorio.guardar(new Apartamento(id("APT-200"), "Inactivo", 1, 2));

        List<String> identificaciones = repositorio.listarActivos().stream()
                .map(a -> a.getIdentificacion().valor()).toList();

        assertEquals(List.of("APT-100", "APT-300"), identificaciones);
    }

    @Test
    void listar_todos_incluye_inactivos_y_pagina_de_diez_con_total_sin_paginar() {
        IntStream.rangeClosed(1, 12).forEach(i -> repositorio.guardar(
                new Apartamento(id(String.format("APT-%03d", i)), "Apartamento " + i, 1, 2)));

        Pagina<Apartamento> primera = repositorio.listarTodos(new SolicitudPagina(0));
        Pagina<Apartamento> segunda = repositorio.listarTodos(new SolicitudPagina(1));

        assertEquals(10, primera.contenido().size());
        assertEquals(2, segunda.contenido().size());
        assertEquals(12, primera.totalElementos());
        assertEquals(1, segunda.numero());
        assertEquals("APT-001", primera.contenido().get(0).getIdentificacion().valor());
        assertEquals("APT-012", segunda.contenido().get(1).getIdentificacion().valor());
    }

    @Test
    void listar_activos_paginado_solo_cuenta_los_activos() {
        repositorio.guardar(activo("APT-101"));
        repositorio.guardar(new Apartamento(id("APT-102"), "Inactivo", 1, 2));

        Pagina<Apartamento> pagina = repositorio.listarActivos(SolicitudPagina.primera());

        assertEquals(1, pagina.totalElementos());
        assertEquals(1, pagina.contenido().size());
    }

    @Test
    void una_pagina_fuera_de_rango_devuelve_contenido_vacio() {
        repositorio.guardar(activo("APT-101"));

        Pagina<Apartamento> pagina = repositorio.listarTodos(new SolicitudPagina(5));

        assertTrue(pagina.contenido().isEmpty());
        assertEquals(1, pagina.totalElementos());
    }
}
