package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.valueobject.EstadoOperativo;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.repository.FiltroApartamentos;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.valueobject.Caracteristica;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.ApartamentoPersistenceMapper;
import co.edu.uniquindio.sga.testsupport.ApartamentoDePrueba;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L-06: persistencia de la descripción, la dotación y las características del apartamento, y su filtro de listado.
 * Cada lectura se hace después de vaciar la sesión de Hibernate para comprobar lo que de verdad quedó en la base
 * de datos y no una copia en memoria.
 */
@DataJpaTest
@Import({ApartamentoJpaRepository.class, ApartamentoPersistenceMapper.class})
class ApartamentoCaracteristicasJpaTest {

    @Autowired
    private ApartamentoJpaRepository repositorio;

    @Autowired
    private EntityManager entityManager;

    private void vaciarSesion() {
        entityManager.flush();
        entityManager.clear();
    }

    private static IdentificacionApartamento id(String valor) {
        return new IdentificacionApartamento(valor);
    }

    private static Apartamento con(String identificacion, Caracteristica... caracteristicas) {
        Apartamento apartamento = new Apartamento(id(identificacion), "Apartamento " + identificacion,
                "Descripción de " + identificacion, 2, 4, List.of("Nevera", "Wi-Fi"), Set.of(caracteristicas));
        apartamento.activar();
        return apartamento;
    }

    private static List<String> ids(Pagina<Apartamento> pagina) {
        return pagina.contenido().stream().map(a -> a.getIdentificacion().valor()).toList();
    }

    @Test
    void ida_y_vuelta_conserva_descripcion_dotacion_en_su_orden_y_caracteristicas() {
        Apartamento original = new Apartamento(id("APT-101"), "Vista", "Luminoso y con vista al parque", 2, 4,
                List.of("Smart TV", "Cocina equipada", "Lavadora"),
                Set.of(Caracteristica.VISTA_AL_PARQUE, Caracteristica.BALCON));
        repositorio.guardar(original);
        vaciarSesion();

        Apartamento leido = repositorio.buscarPorIdentificacion(id("APT-101")).orElseThrow();

        assertEquals("Luminoso y con vista al parque", leido.getDescripcion());
        assertEquals(List.of("Smart TV", "Cocina equipada", "Lavadora"), leido.getDotacion());
        assertEquals(Set.of(Caracteristica.VISTA_AL_PARQUE, Caracteristica.BALCON), leido.getCaracteristicas());
    }

    @Test
    void actualizar_reemplaza_la_dotacion_y_las_caracteristicas_sin_dejar_restos() {
        Apartamento apartamento = repositorio.guardar(con("APT-101", Caracteristica.TERRAZA, Caracteristica.JACUZZI));
        vaciarSesion();

        apartamento.actualizarDatos("Nuevo", "Otra descripción", 2, 4, List.of("Lavadora"),
                Set.of(Caracteristica.ACCESIBLE));
        repositorio.guardar(apartamento);
        vaciarSesion();

        Apartamento leido = repositorio.buscarPorIdentificacion(id("APT-101")).orElseThrow();
        assertEquals(List.of("Lavadora"), leido.getDotacion());
        assertEquals(Set.of(Caracteristica.ACCESIBLE), leido.getCaracteristicas());
        assertEquals("Otra descripción", leido.getDescripcion());
    }

    @Test
    void un_apartamento_sin_dotacion_ni_caracteristicas_se_lee_con_colecciones_vacias() {
        repositorio.guardar(ApartamentoDePrueba.nuevo(id("APT-101"), "Simple", 1, 2));
        vaciarSesion();

        Apartamento leido = repositorio.buscarPorIdentificacion(id("APT-101")).orElseThrow();

        assertTrue(leido.getDotacion().isEmpty());
        assertTrue(leido.getCaracteristicas().isEmpty());
    }

    @Test
    void el_filtro_por_una_caracteristica_devuelve_solo_los_apartamentos_que_la_tienen() {
        repositorio.guardar(con("APT-101", Caracteristica.TERRAZA, Caracteristica.PARQUEADERO));
        repositorio.guardar(con("APT-102", Caracteristica.BALCON));
        repositorio.guardar(con("APT-103", Caracteristica.TERRAZA));
        vaciarSesion();

        Pagina<Apartamento> pagina = repositorio.listarTodos(FiltroApartamentos.de(Set.of(Caracteristica.TERRAZA)),
                SolicitudPagina.primera());

        assertEquals(List.of("APT-101", "APT-103"), ids(pagina));
        assertEquals(2, pagina.totalElementos());
    }

    @Test
    void el_filtro_con_varias_caracteristicas_exige_todas_y_no_alguna() {
        repositorio.guardar(con("APT-101", Caracteristica.TERRAZA, Caracteristica.PARQUEADERO));
        repositorio.guardar(con("APT-102", Caracteristica.TERRAZA));
        repositorio.guardar(con("APT-103", Caracteristica.PARQUEADERO));
        vaciarSesion();

        Pagina<Apartamento> pagina = repositorio.listarTodos(
                FiltroApartamentos.de(Set.of(Caracteristica.TERRAZA, Caracteristica.PARQUEADERO)),
                SolicitudPagina.primera());

        assertEquals(List.of("APT-101"), ids(pagina));
        assertEquals(1, pagina.totalElementos());
    }

    @Test
    void el_filtro_sin_coincidencias_devuelve_una_pagina_vacia() {
        repositorio.guardar(con("APT-101", Caracteristica.TERRAZA));

        Pagina<Apartamento> pagina = repositorio.listarTodos(FiltroApartamentos.de(Set.of(Caracteristica.JACUZZI)),
                SolicitudPagina.primera());

        assertTrue(pagina.contenido().isEmpty());
        assertEquals(0, pagina.totalElementos());
    }

    @Test
    void sin_filtro_se_listan_todos_aunque_no_tengan_caracteristicas() {
        repositorio.guardar(con("APT-101", Caracteristica.TERRAZA));
        repositorio.guardar(ApartamentoDePrueba.nuevo(id("APT-102"), "Simple", 1, 2));

        assertEquals(2, repositorio.listarTodos(FiltroApartamentos.SIN_FILTRO, SolicitudPagina.primera())
                .totalElementos());
        assertEquals(2, repositorio.listarTodos(SolicitudPagina.primera()).totalElementos());
    }

    @Test
    void el_filtro_pagina_de_diez_en_diez_sin_duplicar_apartamentos_con_varias_caracteristicas() {
        IntStream.rangeClosed(1, 12).forEach(i -> repositorio.guardar(con(String.format("APT-%03d", i),
                Caracteristica.TERRAZA, Caracteristica.BALCON, Caracteristica.JACUZZI)));
        vaciarSesion();

        FiltroApartamentos filtro = FiltroApartamentos.de(Set.of(Caracteristica.TERRAZA));
        Pagina<Apartamento> primera = repositorio.listarTodos(filtro, new SolicitudPagina(0));
        Pagina<Apartamento> segunda = repositorio.listarTodos(filtro, new SolicitudPagina(1));

        assertEquals(10, primera.contenido().size());
        assertEquals(2, segunda.contenido().size());
        assertEquals(12, primera.totalElementos());
    }

    // ---------- estado operativo (preparación del personal de servicio) ----------

    @Test
    void el_filtro_por_estado_operativo_devuelve_solo_los_apartamentos_en_esa_condicion() {
        Apartamento ocupado = con("APT-101", Caracteristica.TERRAZA);
        ocupado.marcarOcupado();
        Apartamento fuera = con("APT-102", Caracteristica.TERRAZA);
        fuera.declararFueraDeServicio();
        repositorio.guardar(ocupado);
        repositorio.guardar(fuera);
        repositorio.guardar(con("APT-103", Caracteristica.BALCON));
        vaciarSesion();

        assertEquals(List.of("APT-101"), ids(repositorio.listarTodos(
                FiltroApartamentos.de(Set.of(), EstadoOperativo.OCUPADO), SolicitudPagina.primera())));
        assertEquals(List.of("APT-102"), ids(repositorio.listarTodos(
                FiltroApartamentos.de(Set.of(), EstadoOperativo.FUERA_DE_SERVICIO), SolicitudPagina.primera())));
        assertEquals(List.of("APT-103"), ids(repositorio.listarTodos(
                FiltroApartamentos.de(Set.of(), EstadoOperativo.PREPARADO), SolicitudPagina.primera())));
        assertTrue(ids(repositorio.listarTodos(
                FiltroApartamentos.de(Set.of(), EstadoOperativo.EN_PREPARACION), SolicitudPagina.primera())).isEmpty());
    }

    @Test
    void el_estado_operativo_se_combina_con_las_caracteristicas() {
        Apartamento fuera = con("APT-102", Caracteristica.TERRAZA);
        fuera.declararFueraDeServicio();
        repositorio.guardar(con("APT-101", Caracteristica.TERRAZA));
        repositorio.guardar(fuera);
        vaciarSesion();

        assertEquals(List.of("APT-101"), ids(repositorio.listarTodos(
                FiltroApartamentos.de(Set.of(Caracteristica.TERRAZA), EstadoOperativo.PREPARADO),
                SolicitudPagina.primera())));
        assertEquals(2, repositorio.listarTodos(FiltroApartamentos.de(Set.of(Caracteristica.TERRAZA)),
                SolicitudPagina.primera()).totalElementos());
    }

    @Test
    void un_filtro_sin_estado_es_igual_al_filtro_anterior_solo_por_caracteristicas() {
        org.junit.jupiter.api.Assertions.assertEquals(FiltroApartamentos.SIN_FILTRO,
                FiltroApartamentos.de(Set.of(), null));
        org.junit.jupiter.api.Assertions.assertEquals(new FiltroApartamentos(Set.of(Caracteristica.BALCON)),
                FiltroApartamentos.de(Set.of(Caracteristica.BALCON)));
    }
}
