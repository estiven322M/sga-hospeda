package co.edu.uniquindio.sga.infrastructure.config;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.entity.Temporada;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.BloqueoRepository;
import co.edu.uniquindio.sga.domain.repository.FiltroReservas;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.repository.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.repository.TarifarioRepository;
import co.edu.uniquindio.sga.domain.repository.TemporadaRepository;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.EstadoOperativo;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Pago;
import co.edu.uniquindio.sga.domain.valueobject.Tarifa;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import co.edu.uniquindio.sga.domain.repository.ConfiguracionAlojamiento;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class CargadorDatosPruebaTest {

    private static final FiltroReservas SIN_FILTRO = new FiltroReservas(null, null, null, null, null, null);

    @Autowired private CargadorDatosPrueba cargador;
    @Autowired private ApartamentoRepository apartamentos;
    @Autowired private TemporadaRepository temporadas;
    @Autowired private TarifarioRepository tarifario;
    @Autowired private PoliticaCancelacionRepository politicas;
    @Autowired private ReservaRepository reservas;
    @Autowired private FolioRepository folios;
    @Autowired private BloqueoRepository bloqueos;
    @Autowired private Clock reloj;
    @Autowired private ConfiguracionAlojamiento configuracion;

    private List<Reserva> todasLasReservas() {
        return reservas.buscar(SIN_FILTRO, SolicitudPagina.primera()).contenido();
    }

    private Reserva enEstado(EstadoReserva estado) {
        return todasLasReservas().stream().filter(r -> r.getEstado() == estado).findFirst().orElseThrow();
    }

    private Folio folioDe(Reserva reserva) {
        return folios.buscarPorReserva(reserva.getCodigo()).orElseThrow();
    }

    @Test
    void hay_una_temporada_base_y_al_menos_otras_dos_sin_solapes() {
        List<Temporada> todas = temporadas.listarTodas();

        assertEquals(1, todas.stream().filter(Temporada::esBase).count());
        assertTrue(todas.stream().filter(t -> !t.esBase()).count() >= 2);
        for (int i = 0; i < todas.size(); i++) {
            for (int j = i + 1; j < todas.size(); j++) {
                assertFalse(todas.get(i).seSolapaCon(todas.get(j)));
            }
        }
    }

    @Test
    void todos_los_apartamentos_estan_activos_con_tarifa_en_todas_las_temporadas() {
        Set<String> nombresTemporadas = temporadas.listarTodas().stream()
                .map(Temporada::getNombre).collect(Collectors.toSet());

        List<Apartamento> todos = apartamentos.listarActivos();

        assertEquals(CatalogoDatosPrueba.APARTAMENTOS.size(), todos.size());
        for (Apartamento apartamento : todos) {
            Set<String> conTarifa = tarifario.listarTarifas(apartamento.getIdentificacion()).stream()
                    .map(Tarifa::temporada).collect(Collectors.toSet());
            assertEquals(nombresTemporadas, conTarifa, apartamento.getIdentificacion().valor());
        }
    }

    @Test
    void las_tarifas_de_cada_apartamento_cumplen_base_menor_que_media_menor_que_alta() {
        for (Apartamento apartamento : apartamentos.listarActivos()) {
            var porTemporada = tarifario.listarTarifas(apartamento.getIdentificacion()).stream()
                    .collect(Collectors.toMap(Tarifa::temporada, t -> t.valorPorOcupanteNoche().valor()));

            assertTrue(porTemporada.get("BASE").compareTo(porTemporada.get("MEDIA")) < 0,
                    apartamento.getIdentificacion().valor());
            assertTrue(porTemporada.get("MEDIA").compareTo(porTemporada.get("ALTA")) < 0,
                    apartamento.getIdentificacion().valor());
        }
    }

    @Test
    void las_temporadas_son_base_media_y_alta() {
        assertEquals(Set.of("BASE", "MEDIA", "ALTA"),
                temporadas.listarTodas().stream().map(Temporada::getNombre).collect(Collectors.toSet()));
    }

    @Test
    void apt_101_y_apt_102_existen_con_capacidad_entre_dos_y_diez() {
        for (String id : List.of("APT-101", "APT-102")) {
            Apartamento apartamento = apartamentos.buscarPorIdentificacion(new IdentificacionApartamento(id))
                    .orElseThrow();
            assertTrue(apartamento.isActivo());
            assertTrue(apartamento.getCapacidad() >= 2 && apartamento.getCapacidad() <= 10);
        }
    }

    @Test
    void hay_un_apartamento_fuera_de_servicio_y_uno_ocupado_por_la_reserva_en_curso() {
        List<Apartamento> todos = apartamentos.listarActivos();

        assertEquals(1, todos.stream()
                .filter(a -> a.getEstadoOperativo() == EstadoOperativo.FUERA_DE_SERVICIO).count());
        Reserva enCurso = enEstado(EstadoReserva.EN_CURSO);
        Apartamento ocupado = todos.stream().filter(a -> a.getIdentificacion().equals(enCurso.getApartamento()))
                .findFirst().orElseThrow();
        assertEquals(EstadoOperativo.OCUPADO, ocupado.getEstadoOperativo());
    }

    @Test
    void la_politica_tiene_al_menos_dos_tramos_y_rige_desde_antes_de_hoy() {
        LocalDate hoy = LocalDate.now(reloj);

        var politica = politicas.vigenteEn(hoy).orElseThrow();

        assertTrue(politica.getTramos().size() >= 2);
        assertTrue(politica.getVigenteDesde().isBefore(hoy));
    }

    @Test
    void hay_reservas_en_todos_los_estados_y_una_por_cada_canal() {
        List<Reserva> todas = todasLasReservas();

        assertEquals(Set.of(EstadoReserva.values()),
                todas.stream().map(Reserva::getEstado).collect(Collectors.toSet()));
        assertEquals(Set.of(CanalOrigen.values()),
                todas.stream().map(Reserva::getCanalOrigen).collect(Collectors.toSet()));
    }

    @Test
    void ninguna_reserva_sembrada_ocupa_fechas_de_2028() {
        assertTrue(todasLasReservas().stream().noneMatch(r ->
                r.getEstancia().fechaEntrada().getYear() == 2028 || r.getEstancia().fechaSalida().getYear() == 2028));
    }

    @Test
    void cada_reserva_tiene_su_folio_con_el_cargo_de_alojamiento_igual_al_valor_congelado() {
        for (Reserva reserva : todasLasReservas()) {
            Folio folio = folioDe(reserva);
            assertEquals(reserva.getValor().total(), folio.obtenerCargos().get(0).valor());
        }
    }

    @Test
    void la_reserva_cancelada_tiene_el_reverso_y_la_retencion_de_la_politica_congelada() {
        Folio folio = folioDe(enEstado(EstadoReserva.CANCELADA));
        Dinero valor = folio.obtenerCargos().get(0).valor();
        Dinero pagado = folio.obtenerPagos().stream().map(Pago::valor).reduce(Dinero.CERO, Dinero::mas);

        assertEquals(1, folio.obtenerAjustes().size());
        assertEquals(Dinero.CERO.menos(valor), folio.obtenerAjustes().get(0).valor());
        Dinero penalidad = folio.obtenerCargos().get(1).valor();
        assertEquals(penalidad.menos(pagado), folio.saldo());
        assertTrue(penalidad.valor().signum() > 0);
    }

    @Test
    void la_reserva_no_show_tiene_el_reverso_y_la_penalidad_del_tramo_tardio() {
        Reserva reserva = enEstado(EstadoReserva.NO_SHOW);
        Folio folio = folioDe(reserva);

        assertEquals(1, folio.obtenerAjustes().size());
        assertEquals(reserva.getValor().total(), folio.obtenerCargos().get(1).valor());
    }

    @Test
    void la_reserva_finalizada_tiene_el_folio_cerrado_y_sin_saldo() {
        Folio folio = folioDe(enEstado(EstadoReserva.FINALIZADA));

        assertTrue(folio.estaCerrado());
        assertTrue(folio.saldo().esCero());
    }

    @Test
    void la_reserva_en_curso_tiene_el_folio_abierto_con_saldo_pendiente() {
        Folio folio = folioDe(enEstado(EstadoReserva.EN_CURSO));

        assertFalse(folio.estaCerrado());
        assertFalse(folio.saldo().esCero());
    }

    @Test
    void la_reserva_pendiente_se_siembra_dentro_del_plazo_y_el_vencimiento_automatico_no_la_cancela() {
        Reserva pendiente = enEstado(EstadoReserva.PENDIENTE);
        LocalDateTime ahora = LocalDateTime.now(reloj);

        assertFalse(configuracion.plazoConfirmacion().venceAntesDe(pendiente.getCreadaEn(), ahora));
        assertTrue(reservas.buscarPendientesCreadasAntesDe(
                ahora.minusHours(configuracion.plazoConfirmacion().horas())).isEmpty());
    }

    @Test
    void hay_al_menos_un_bloqueo_vigente() {
        assertFalse(bloqueos.buscarVigentesPorApartamento(
                new IdentificacionApartamento(CatalogoDatosPrueba.APARTAMENTO_CON_BLOQUEO)).isEmpty());
    }

    @Test
    void cargar_otra_vez_no_duplica_los_datos() {
        int reservasAntes = todasLasReservas().size();
        long apartamentosAntes = apartamentos.listarTodos(SolicitudPagina.primera()).totalElementos();

        cargador.run(new DefaultApplicationArguments());

        assertEquals(reservasAntes, todasLasReservas().size());
        assertEquals(apartamentosAntes, apartamentos.listarTodos(SolicitudPagina.primera()).totalElementos());
    }
}
