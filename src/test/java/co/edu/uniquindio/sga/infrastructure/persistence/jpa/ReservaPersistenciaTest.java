package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.repository.FiltroReservas;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.DocumentoIdentidad;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.EstanciaPersistenceMapper;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.OcupantePersistenceMapper;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.ReservaPersistenceMapper;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.IntStream;

import static co.edu.uniquindio.sga.infrastructure.persistence.jpa.ReservasDePrueba.AHORA;
import static co.edu.uniquindio.sga.infrastructure.persistence.jpa.ReservasDePrueba.RELOJ;
import static co.edu.uniquindio.sga.infrastructure.persistence.jpa.ReservasDePrueba.reserva;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import({ReservaJpaRepository.class, ReservaPersistenceMapper.class, EstanciaPersistenceMapper.class,
        OcupantePersistenceMapper.class, ReservaPersistenciaTest.RelojFijo.class})
class ReservaPersistenciaTest {

    @TestConfiguration
    static class RelojFijo {
        @Bean
        Clock reloj() {
            return RELOJ;
        }
    }

    private static final IdentificacionApartamento APT = new IdentificacionApartamento("APT-101");
    private static final LocalDate ENTRADA = LocalDate.of(2027, 3, 10);

    @Autowired
    private ReservaJpaRepository repositorio;
    @Autowired
    private EntityManager em;

    private void sincronizarYLimpiar() {
        em.flush();
        em.clear();
    }

    private static CodigoReserva codigo(int n) {
        return new CodigoReserva(String.format("RES-2026-%05d", n));
    }

    private Reserva guardar(int n, String apartamento, LocalDate entrada, String titular, CanalOrigen canal,
                            int minutosDespues) {
        return repositorio.guardar(reserva(codigo(n).valor(), apartamento, entrada, 2, titular, canal,
                AHORA.plusMinutes(minutosDespues)));
    }

    @Test
    void guardar_y_buscar_por_codigo_devuelve_la_reserva_completa() {
        Reserva original = repositorio.guardar(reserva(codigo(1).valor(), "APT-101", ENTRADA));
        sincronizarYLimpiar();

        Reserva leida = repositorio.buscarPorCodigo(codigo(1)).orElseThrow();

        assertEquals(original.getHistorial(), leida.getHistorial());
        assertEquals(original.getValor(), leida.getValor());
        assertEquals(original.getOcupantes(), leida.getOcupantes());
        assertEquals(original.getEstancia(), leida.getEstancia());
    }

    @Test
    void buscar_por_codigo_inexistente_devuelve_vacio() {
        assertTrue(repositorio.buscarPorCodigo(codigo(77)).isEmpty());
    }

    @Test
    void guardar_una_reserva_existente_la_actualiza_sin_duplicarla_y_agrega_al_historial() {
        Reserva reserva = repositorio.guardar(reserva(codigo(1).valor(), "APT-101", ENTRADA));
        reserva.indicarHoraEstimadaLlegada(LocalTime.of(15, 0), "Titular 100", AHORA.plusHours(1));
        reserva.confirmar("Recepción", AHORA.plusHours(2));

        repositorio.guardar(reserva);
        sincronizarYLimpiar();

        Reserva leida = repositorio.buscarPorCodigo(codigo(1)).orElseThrow();
        assertEquals(EstadoReserva.CONFIRMADA, leida.getEstado());
        assertEquals(3, leida.getHistorial().size());
        assertEquals(1, repositorio.buscar(new FiltroReservas(null, null, null, null, null, null),
                SolicitudPagina.primera()).totalElementos());
    }

    @Test
    void buscar_activas_incluye_pendiente_confirmada_y_en_curso_y_excluye_las_terminales() {
        Reserva pendiente = guardar(1, "APT-101", ENTRADA, "100", CanalOrigen.PORTAL, 0);
        Reserva cancelada = guardar(2, "APT-101", ENTRADA.plusDays(10), "101", CanalOrigen.PORTAL, 1);
        cancelada.cancelar("Cambio de planes", "Titular 101", AHORA.plusHours(1));
        repositorio.guardar(cancelada);
        guardar(3, "APT-102", ENTRADA, "102", CanalOrigen.PORTAL, 2);
        sincronizarYLimpiar();

        List<Reserva> activas = repositorio.buscarActivasPorApartamento(APT);

        assertEquals(List.of(pendiente.getCodigo()), activas.stream().map(Reserva::getCodigo).toList());
        assertEquals(2, activas.get(0).getOcupantes().size());
    }

    @Test
    void buscar_ordena_de_la_reserva_mas_reciente_a_la_mas_antigua() {
        guardar(1, "APT-101", ENTRADA, "100", CanalOrigen.PORTAL, 0);
        guardar(2, "APT-101", ENTRADA.plusDays(5), "100", CanalOrigen.PORTAL, 30);
        guardar(3, "APT-101", ENTRADA.plusDays(9), "100", CanalOrigen.PORTAL, 15);
        sincronizarYLimpiar();

        Pagina<Reserva> pagina = repositorio.buscar(new FiltroReservas(null, null, null, null, null, null),
                SolicitudPagina.primera());

        assertEquals(List.of(codigo(2), codigo(3), codigo(1)),
                pagina.contenido().stream().map(Reserva::getCodigo).toList());
    }

    @Test
    void buscar_pagina_de_diez_con_total_sin_paginar() {
        IntStream.rangeClosed(1, 13).forEach(i ->
                guardar(i, "APT-101", ENTRADA.plusDays(i * 3L), "100", CanalOrigen.PORTAL, i));
        sincronizarYLimpiar();
        FiltroReservas sinFiltro = new FiltroReservas(null, null, null, null, null, null);

        Pagina<Reserva> primera = repositorio.buscar(sinFiltro, new SolicitudPagina(0));
        Pagina<Reserva> segunda = repositorio.buscar(sinFiltro, new SolicitudPagina(1));

        assertEquals(10, primera.contenido().size());
        assertEquals(3, segunda.contenido().size());
        assertEquals(13, primera.totalElementos());
        assertEquals(1, segunda.numero());
        assertEquals(codigo(13), primera.contenido().get(0).getCodigo());
        assertEquals(codigo(1), segunda.contenido().get(2).getCodigo());
    }

    @Test
    void buscar_filtra_por_rango_de_fecha_de_entrada_con_ambos_extremos_inclusive() {
        guardar(1, "APT-101", ENTRADA, "100", CanalOrigen.PORTAL, 1);
        guardar(2, "APT-101", ENTRADA.plusDays(5), "100", CanalOrigen.PORTAL, 2);
        guardar(3, "APT-101", ENTRADA.plusDays(10), "100", CanalOrigen.PORTAL, 3);
        sincronizarYLimpiar();

        Pagina<Reserva> pagina = repositorio.buscar(new FiltroReservas(
                ENTRADA, ENTRADA.plusDays(5), null, null, null, null), SolicitudPagina.primera());

        assertEquals(List.of(codigo(2), codigo(1)), pagina.contenido().stream().map(Reserva::getCodigo).toList());
        assertEquals(2, pagina.totalElementos());
    }

    @Test
    void buscar_combina_estado_apartamento_canal_y_titular() {
        guardar(1, "APT-101", ENTRADA, "100", CanalOrigen.PORTAL, 1);
        guardar(2, "APT-101", ENTRADA.plusDays(5), "100", CanalOrigen.EXTERNO, 2);
        guardar(3, "APT-102", ENTRADA, "100", CanalOrigen.EXTERNO, 3);
        guardar(4, "APT-101", ENTRADA.plusDays(9), "200", CanalOrigen.EXTERNO, 4);
        sincronizarYLimpiar();

        Pagina<Reserva> pagina = repositorio.buscar(new FiltroReservas(null, null, EstadoReserva.PENDIENTE, APT,
                CanalOrigen.EXTERNO, new DocumentoIdentidad("100")), SolicitudPagina.primera());

        assertEquals(List.of(codigo(2)), pagina.contenido().stream().map(Reserva::getCodigo).toList());
        assertTrue(repositorio.buscar(new FiltroReservas(null, null, EstadoReserva.CANCELADA, null, null, null),
                SolicitudPagina.primera()).contenido().isEmpty());
    }

    @Test
    void listar_una_pagina_de_diez_reservas_no_dispara_una_consulta_por_reserva() {
        IntStream.rangeClosed(1, 10).forEach(i ->
                guardar(i, "APT-101", ENTRADA.plusDays(i * 3L), "100", CanalOrigen.PORTAL, i));
        sincronizarYLimpiar();
        var estadisticas = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        estadisticas.clear();

        Pagina<Reserva> pagina = repositorio.buscar(new FiltroReservas(null, null, null, null, null, null),
                SolicitudPagina.primera());

        assertEquals(10, pagina.contenido().size());
        // Página + conteo + una carga por lote para cada una de las tres colecciones
        assertTrue(estadisticas.getPrepareStatementCount() <= 5,
                "Consultas ejecutadas: " + estadisticas.getPrepareStatementCount());
    }
}
