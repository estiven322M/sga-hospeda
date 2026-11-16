package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import static co.edu.uniquindio.sga.infrastructure.persistence.jpa.ReservasDePrueba.reservaConDescuento;
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

    // ---------- RP-02 (L-20): el descuento queda congelado en la base de datos ----------

    @Test
    void el_porcentaje_de_descuento_se_guarda_y_se_lee_de_la_base_de_datos_rp02() {
        Reserva original = repositorio.guardar(reservaConDescuento(codigo(1).valor(), "APT-101", ENTRADA, 7, 10));
        sincronizarYLimpiar();

        Reserva leida = repositorio.buscarPorCodigo(codigo(1)).orElseThrow();

        assertEquals(10, leida.getValor().porcentajeDescuento());
        assertEquals(original.getValor().bruto(), leida.getValor().bruto());
        assertEquals(Dinero.de(140_000), leida.getValor().descuento()); // bruto: 7 noches x 100.000 x 2 facturables = 1.400.000; el 10 % es 140.000
        assertEquals(original.getValor().total(), leida.getValor().total());
    }

    @Test
    void una_reserva_sin_descuento_se_lee_con_cero_por_ciento_rp02() {
        repositorio.guardar(reserva(codigo(1).valor(), "APT-101", ENTRADA));
        sincronizarYLimpiar();

        Reserva leida = repositorio.buscarPorCodigo(codigo(1)).orElseThrow();

        assertEquals(0, leida.getValor().porcentajeDescuento());
        assertEquals(Dinero.CERO, leida.getValor().descuento());
    }

    // ---------- 7.5, 7.6: salidas previstas y nombre del titular ----------

    private static FiltroReservas filtroNombre(String texto) {
        return new FiltroReservas(null, null, null, null, null, null, null, null, texto);
    }

    private List<String> codigos(FiltroReservas filtro) {
        return repositorio.buscar(filtro, SolicitudPagina.primera()).contenido().stream()
                .map(r -> r.getCodigo().valor()).toList();
    }

    @Test
    void el_nombre_del_titular_se_busca_por_texto_contenido_sin_importar_mayusculas() {
        guardar(1, "APT-101", ENTRADA, "100", CanalOrigen.PORTAL, 0);            // Titular 100
        guardar(2, "APT-102", ENTRADA, "200", CanalOrigen.PORTAL, 1);            // Titular 200
        sincronizarYLimpiar();

        assertEquals(List.of(codigo(1).valor()), codigos(filtroNombre("TITULAR 1")));
        assertEquals(List.of(codigo(2).valor()), codigos(filtroNombre("itular 20")));
        assertEquals(2, codigos(filtroNombre("titular")).size());
        assertTrue(codigos(filtroNombre("nadie")).isEmpty());
    }

    @Test
    void el_filtro_por_nombre_busca_solo_el_nombre_del_titular_y_no_el_de_los_acompanantes() {
        guardar(1, "APT-101", ENTRADA, "100", CanalOrigen.PORTAL, 0); // acompañante: "Acompañante"
        sincronizarYLimpiar();

        assertTrue(codigos(filtroNombre("acompa")).isEmpty());
    }

    @Test
    void los_comodines_de_like_escritos_por_el_usuario_se_toman_literalmente() {
        guardar(1, "APT-101", ENTRADA, "100", CanalOrigen.PORTAL, 0);
        sincronizarYLimpiar();

        assertTrue(codigos(filtroNombre("%")).isEmpty());
        assertTrue(codigos(filtroNombre("_")).isEmpty());
        assertTrue(codigos(filtroNombre("Titular _00")).isEmpty()); // _ no es "cualquier carácter"
    }

    @Test
    void el_filtro_por_nombre_no_duplica_reservas_con_varios_ocupantes_ni_rompe_la_paginacion() {
        guardar(1, "APT-101", ENTRADA, "100", CanalOrigen.PORTAL, 0);
        sincronizarYLimpiar();

        var pagina = repositorio.buscar(filtroNombre("titular"), SolicitudPagina.primera());

        assertEquals(1, pagina.totalElementos());
        assertEquals(1, pagina.contenido().size());
    }

    @Test
    void las_salidas_previstas_se_filtran_por_la_fecha_de_salida_con_extremos_inclusivos() {
        guardar(1, "APT-101", ENTRADA, "100", CanalOrigen.PORTAL, 0);                    // sale ENTRADA + 2
        guardar(2, "APT-102", ENTRADA.plusDays(5), "200", CanalOrigen.PORTAL, 1);        // sale ENTRADA + 7
        sincronizarYLimpiar();
        LocalDate salida1 = ENTRADA.plusDays(2);
        LocalDate salida2 = ENTRADA.plusDays(7);

        // el día exacto de salida (desde = hasta)
        assertEquals(List.of(codigo(1).valor()), codigos(new FiltroReservas(null, null, salida1, salida1,
                null, null, null, null, null)));
        // un día antes o después no la incluye
        assertTrue(codigos(new FiltroReservas(null, null, salida1.plusDays(1), salida1.plusDays(1),
                null, null, null, null, null)).isEmpty());
        // solo desde, solo hasta y un rango que las contiene
        assertEquals(List.of(codigo(2).valor()), codigos(new FiltroReservas(null, null, salida1.plusDays(1), null,
                null, null, null, null, null)));
        assertEquals(List.of(codigo(1).valor()), codigos(new FiltroReservas(null, null, null, salida1.plusDays(1),
                null, null, null, null, null)));
        assertEquals(2, codigos(new FiltroReservas(null, null, salida1, salida2, null, null, null, null, null)).size());
    }

    @Test
    void la_salida_no_se_confunde_con_la_entrada() {
        guardar(1, "APT-101", ENTRADA, "100", CanalOrigen.PORTAL, 0);
        sincronizarYLimpiar();

        // el día de ENTRADA no es día de salida
        assertTrue(codigos(new FiltroReservas(null, null, ENTRADA, ENTRADA, null, null, null, null, null)).isEmpty());
        assertEquals(List.of(codigo(1).valor()),
                codigos(new FiltroReservas(ENTRADA, ENTRADA, null, null, null, null, null, null, null)));
    }

    @Test
    void el_nombre_se_combina_con_el_estado_y_con_el_documento_exacto_del_titular() {
        Reserva cancelada = guardar(1, "APT-101", ENTRADA, "100", CanalOrigen.PORTAL, 0);
        cancelada.cancelar("Cambio de planes", "Titular 100", AHORA.plusHours(1));
        repositorio.guardar(cancelada);
        guardar(2, "APT-102", ENTRADA, "200", CanalOrigen.PORTAL, 1);
        sincronizarYLimpiar();

        assertEquals(List.of(codigo(1).valor()), codigos(new FiltroReservas(null, null, null, null,
                EstadoReserva.CANCELADA, null, null, null, "titular")));
        assertEquals(List.of(codigo(2).valor()), codigos(new FiltroReservas(null, null, null, null,
                null, null, null, new DocumentoIdentidad("200"), "TITULAR")));
        // el documento sigue siendo exacto: un fragmento no encuentra nada
        assertTrue(codigos(new FiltroReservas(null, null, null, null, null, null, null,
                new DocumentoIdentidad("20"), null)).isEmpty());
    }
}
