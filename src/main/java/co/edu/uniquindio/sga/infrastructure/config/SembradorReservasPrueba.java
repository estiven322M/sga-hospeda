package co.edu.uniquindio.sga.infrastructure.config;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.entity.Ocupante;
import co.edu.uniquindio.sga.domain.entity.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.entity.RechazoReservaExterna;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.ConfiguracionAlojamiento;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.repository.GeneradorCodigoReserva;
import co.edu.uniquindio.sga.domain.repository.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga.domain.repository.RechazoReservaExternaRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.service.ConfirmacionReservaService;
import co.edu.uniquindio.sga.domain.service.CotizadorEstancia;
import co.edu.uniquindio.sga.domain.service.DisponibilidadApartamentoService;
import co.edu.uniquindio.sga.domain.service.RetencionCancelacionService;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.Cargo;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.DocumentoIdentidad;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.IdentificadorExterno;
import co.edu.uniquindio.sga.domain.valueobject.Pago;
import co.edu.uniquindio.sga.domain.valueobject.ValorCongelado;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Siembra una reserva en cada estado y por cada canal usando los métodos reales del dominio. Cada reserva
 * "viaja en el tiempo": se crea y se transita con el momento en que habría ocurrido cada hecho, calculado
 * a partir de la fecha de hoy del reloj del alojamiento. Por eso ninguna cae en fechas fijas (2028 queda libre).
 */
@Component
@ConditionalOnProperty(name = "sga.datos-prueba.habilitado", havingValue = "true")
class SembradorReservasPrueba {

    private static final String AUTOR = "Datos de prueba";
    private static final String CONCEPTO_ALOJAMIENTO = "Alojamiento";
    private static final String CONCEPTO_CONSUMO = "Consumo de minibar";
    private static final String MEDIO_PAGO = "TRANSFERENCIA";
    private static final LocalTime HORA_CREACION = LocalTime.of(10, 0);
    private static final LocalTime HORA_CANCELACION = LocalTime.of(12, 0);
    private static final long VALOR_CONSUMO = 25_000;
    private static final long ABONO_DURANTE_LA_ESTANCIA = 100_000;
    private static final int EDAD_DEL_MENOR = 8;
    private static final int EDAD_DE_LOS_ADULTOS = 35;

    /** Un escenario: dónde, cuándo (en días desde hoy), por qué canal y hace cuántos días se creó. */
    private record Escenario(String apartamento, int entradaEnDias, int noches, CanalOrigen canal,
                             String documentoTitular, int creadaHaceDias, boolean conHoraDeLlegada) {
    }

    private record ReservaConFolio(Reserva reserva, Folio folio, LocalDateTime creadaEn) {
    }

    private final LocalDate hoy;
    private final ApartamentoRepository apartamentos;
    private final ReservaRepository reservas;
    private final RechazoReservaExternaRepository rechazos;
    private final FolioRepository folios;
    private final PoliticaCancelacionRepository politicas;
    private final GeneradorCodigoReserva generador;
    private final DisponibilidadApartamentoService disponibilidad;
    private final CotizadorEstancia cotizador;
    private final ConfirmacionReservaService confirmacion;
    private final RetencionCancelacionService retencion;
    private final ConfiguracionAlojamiento configuracion;

    SembradorReservasPrueba(Clock reloj, ApartamentoRepository apartamentos, ReservaRepository reservas,
                            FolioRepository folios, RechazoReservaExternaRepository rechazos, PoliticaCancelacionRepository politicas,
                            GeneradorCodigoReserva generador, DisponibilidadApartamentoService disponibilidad,
                            CotizadorEstancia cotizador, ConfirmacionReservaService confirmacion,
                            RetencionCancelacionService retencion, ConfiguracionAlojamiento configuracion) {
        this.hoy = LocalDate.now(reloj);
        this.apartamentos = apartamentos;
        this.reservas = reservas;
        this.rechazos = rechazos;
        this.folios = folios;
        this.politicas = politicas;
        this.generador = generador;
        this.disponibilidad = disponibilidad;
        this.cotizador = cotizador;
        this.confirmacion = confirmacion;
        this.retencion = retencion;
        this.configuracion = configuracion;
    }

    void sembrar() {
        sembrarCancelada();
        sembrarNoShow();
        sembrarFinalizada();
        sembrarEnCurso();
        sembrarConfirmada();
        sembrarPendiente();
        sembrarRechazoExterno();
    }

    private void sembrarPendiente() {
        ReservaConFolio r = abrir(new Escenario("APT-201", 20, 2, CanalOrigen.PORTAL, "CC-1001", 0, false));
        guardar(r);
    }

    /** RN-19: los canales externos traen el identificador con el que ese canal conoce la reserva. */
    private static IdentificadorExterno identificadorExternoDe(Escenario escenario) {
        return escenario.canal().exigeIdentificadorExterno()
                ? new IdentificadorExterno("EXT-" + escenario.documentoTitular()) : null;
    }

    /** RN-18: una reserva externa que chocó con la confirmada de APT-202 y quedó registrada sin tocarla. */
    private void sembrarRechazoExterno() {
        LocalDate entrada = hoy.plusDays(10);
        rechazos.guardar(new RechazoReservaExterna(CanalOrigen.EXTERNO, new IdentificadorExterno("EXT-CONFLICTO-001"),
                new IdentificacionApartamento("APT-202"), new Estancia(entrada, entrada.plusDays(2)),
                "El apartamento ya tiene una reserva activa que solapa esas noches",
                hoy.minusDays(1).atTime(HORA_CREACION)));
    }

    private void sembrarConfirmada() {
        ReservaConFolio r = abrir(new Escenario("APT-202", 10, 2, CanalOrigen.DIRECTO, "CC-1002", 3, true));
        pagarAnticipoYConfirmar(r);
        guardar(r);
    }

    private void sembrarEnCurso() {
        ReservaConFolio r = abrir(new Escenario("APT-203", -1, 3, CanalOrigen.EXTERNO, "CC-1003", 6, true));
        pagarAnticipoYConfirmar(r);
        LocalDateTime llegada = llegadaDe(r.reserva());
        r.reserva().registrarLlegada(AUTOR, llegada);
        r.folio().registrarCargo(new Cargo(CONCEPTO_CONSUMO, Dinero.de(VALOR_CONSUMO)));
        r.folio().registrarPago(new Pago(MEDIO_PAGO, llegada.toLocalDate(), Dinero.de(ABONO_DURANTE_LA_ESTANCIA)));
        Apartamento apartamento = apartamentoDe(r.reserva());
        apartamento.marcarOcupado();
        apartamentos.guardar(apartamento);
        guardar(r);
    }

    private void sembrarFinalizada() {
        ReservaConFolio r = abrir(new Escenario("APT-201", -30, 3, CanalOrigen.PORTAL, "CC-1004", 40, true));
        pagarAnticipoYConfirmar(r);
        LocalDateTime llegada = llegadaDe(r.reserva());
        r.reserva().registrarLlegada(AUTOR, llegada);
        r.folio().registrarPago(new Pago(MEDIO_PAGO, llegada.toLocalDate(), r.folio().saldo()));
        r.folio().cerrar(null);
        r.reserva().registrarSalida(AUTOR, r.reserva().getEstancia().fechaSalida()
                .atTime(configuracion.horaSalida()).minusHours(1));
        guardar(r);
    }

    private void sembrarCancelada() {
        ReservaConFolio r = abrir(new Escenario("APT-202", 1, 2, CanalOrigen.DIRECTO, "CC-1005", 20, true));
        pagarAnticipoYConfirmar(r);
        LocalDateTime ahora = hoy.minusDays(1).atTime(HORA_CANCELACION);
        PoliticaCancelacion politica = politicaDe(r.reserva());
        r.reserva().cancelar("Cambio de planes del titular", AUTOR, ahora);
        r.folio().liquidarCancelacion(retencion.retencionPorCancelacion(r.reserva(), politica, ahora));
        guardar(r);
    }

    private void sembrarNoShow() {
        ReservaConFolio r = abrir(new Escenario("APT-301", -8, 2, CanalOrigen.EXTERNO, "CC-1006", 12, true));
        pagarAnticipoYConfirmar(r);
        LocalDateTime ahora = r.reserva().getEstancia().fechaEntrada()
                .atTime(configuracion.horaLimiteNoShow()).plusHours(1);
        PoliticaCancelacion politica = politicaDe(r.reserva());
        r.reserva().declararNoShow(configuracion.horaLimiteNoShow(), AUTOR, ahora);
        r.folio().liquidarNoShow(retencion.retencionPorNoShow(r.reserva(), politica));
        guardar(r);
    }

    private ReservaConFolio abrir(Escenario escenario) {
        Apartamento apartamento = apartamentoDe(escenario.apartamento());
        LocalDate entrada = hoy.plusDays(escenario.entradaEnDias());
        Estancia estancia = new Estancia(entrada, entrada.plusDays(escenario.noches()));
        List<Ocupante> grupo = grupoDe(escenario.documentoTitular());
        disponibilidad.verificarDisponible(apartamento, estancia, grupo.size(), null);
        ValorCongelado valor = cotizador.cotizar(apartamento.getIdentificacion(), estancia, grupo);
        LocalDateTime creadaEn = hoy.minusDays(escenario.creadaHaceDias()).atTime(HORA_CREACION);
        CodigoReserva codigo = generador.generar(creadaEn.toLocalDate());
        Reserva reserva = Reserva.crear(codigo, apartamento.getIdentificacion(), estancia, grupo.get(0), grupo,
                escenario.canal(), identificadorExternoDe(escenario), valor, ultimaPolitica().getVersion(), creadaEn);
        if (escenario.conHoraDeLlegada()) {
            reserva.indicarHoraEstimadaLlegada(configuracion.horaEntrada().plusHours(1), AUTOR, creadaEn);
        }
        Folio folio = new Folio(codigo, new Cargo(CONCEPTO_ALOJAMIENTO, valor.total()));
        return new ReservaConFolio(reserva, folio, creadaEn);
    }

    private void pagarAnticipoYConfirmar(ReservaConFolio r) {
        r.folio().registrarPago(new Pago(MEDIO_PAGO, r.creadaEn().toLocalDate(),
                confirmacion.anticipoRequerido(r.reserva())));
        confirmacion.confirmar(r.reserva(), r.folio(), AUTOR, r.creadaEn().plusHours(1));
    }

    private LocalDateTime llegadaDe(Reserva reserva) {
        return reserva.getEstancia().fechaEntrada().atTime(configuracion.horaEntrada()).plusMinutes(30);
    }

    private void guardar(ReservaConFolio r) {
        reservas.guardar(r.reserva());
        folios.guardar(r.folio());
    }

    private List<Ocupante> grupoDe(String documentoTitular) {
        Ocupante titular = ocupante(documentoTitular, "Titular " + documentoTitular, EDAD_DE_LOS_ADULTOS);
        return List.of(titular,
                ocupante(documentoTitular + "-A", "Acompañante " + documentoTitular, EDAD_DE_LOS_ADULTOS),
                ocupante(documentoTitular + "-M", "Menor " + documentoTitular, EDAD_DEL_MENOR));
    }

    private Ocupante ocupante(String documento, String nombre, int edad) {
        return new Ocupante(new DocumentoIdentidad(documento), nombre, hoy.minusYears(edad), hoy);
    }

    private Apartamento apartamentoDe(Reserva reserva) {
        return apartamentoDe(reserva.getApartamento().valor());
    }

    private Apartamento apartamentoDe(String identificacion) {
        return apartamentos.buscarPorIdentificacion(new IdentificacionApartamento(identificacion))
                .orElseThrow(() -> new IllegalStateException("Falta el apartamento de prueba " + identificacion));
    }

    private PoliticaCancelacion ultimaPolitica() {
        return politicas.ultimaVersion()
                .orElseThrow(() -> new IllegalStateException("Falta la política de cancelación de prueba"));
    }

    private PoliticaCancelacion politicaDe(Reserva reserva) {
        return politicas.porVersion(reserva.getPolitica())
                .orElseThrow(() -> new IllegalStateException("Falta la política " + reserva.getPolitica().numero()));
    }
}
