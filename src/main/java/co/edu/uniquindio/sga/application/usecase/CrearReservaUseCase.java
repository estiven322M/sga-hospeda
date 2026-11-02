package co.edu.uniquindio.sga.application.usecase;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.application.exception.PoliticaCancelacionNoEncontradaException;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.entity.Ocupante;
import co.edu.uniquindio.sga.domain.entity.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.repository.GeneradorCodigoReserva;
import co.edu.uniquindio.sga.domain.repository.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.service.CotizadorEstancia;
import co.edu.uniquindio.sga.domain.service.DisponibilidadApartamentoService;
import co.edu.uniquindio.sga.domain.valueobject.Cargo;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.ValorCongelado;

/**
 * CU-03 Crear reserva. Orquesta, no decide:
 *  - la disponibilidad (RN-01, RN-02, RN-07, RN-20) la decide DisponibilidadApartamentoService;
 *  - el valor (RN-05, RN-06) lo calcula CotizadorEstancia;
 *  - las invariantes de la reserva (RN-04, titular entre los ocupantes, valor y política congelados, RN-22)
 *    las protege Reserva.crear.
 * Al crearse la reserva se abre su folio con el cargo de alojamiento (F-08), para poder registrar pagos
 * antes de la llegada. La hora estimada de llegada es opcional aquí y obligatoria para confirmar (RN-09).
 */
@Service
public class CrearReservaUseCase {

    private static final String CONCEPTO_ALOJAMIENTO = "Alojamiento";

    private final ApartamentoRepository apartamentoRepository;
    private final PoliticaCancelacionRepository politicaRepository;
    private final ReservaRepository reservaRepository;
    private final FolioRepository folioRepository;
    private final GeneradorCodigoReserva generadorCodigo;
    private final DisponibilidadApartamentoService disponibilidadService;
    private final CotizadorEstancia cotizador;

    public CrearReservaUseCase(ApartamentoRepository apartamentoRepository,
                               PoliticaCancelacionRepository politicaRepository,
                               ReservaRepository reservaRepository,
                               FolioRepository folioRepository,
                               GeneradorCodigoReserva generadorCodigo,
                               DisponibilidadApartamentoService disponibilidadService,
                               CotizadorEstancia cotizador) {
        this.apartamentoRepository = apartamentoRepository;
        this.politicaRepository = politicaRepository;
        this.reservaRepository = reservaRepository;
        this.folioRepository = folioRepository;
        this.generadorCodigo = generadorCodigo;
        this.disponibilidadService = disponibilidadService;
        this.cotizador = cotizador;
    }

    /**
     * @param horaEstimadaLlegada opcional (null si aún no se conoce)
     * @param ahora               momento actual en la zona del alojamiento; lo pone el servidor, no el cliente
     */
    @Transactional
    public Reserva ejecutar(IdentificacionApartamento identificacion, Estancia estancia, Ocupante titular,
                            List<Ocupante> ocupantes, CanalOrigen canalOrigen, LocalTime horaEstimadaLlegada,
                            LocalDateTime ahora) {
        Apartamento apartamento = apartamentoRepository.buscarPorIdentificacion(identificacion)
                .orElseThrow(() -> new ApartamentoNoEncontradoException(identificacion));

        disponibilidadService.verificarDisponible(apartamento, estancia, ocupantes.size(), null);
        ValorCongelado valor = cotizador.cotizar(identificacion, estancia, ocupantes);

        PoliticaCancelacion politica = politicaRepository.vigenteEn(ahora.toLocalDate())
                .orElseThrow(() -> new PoliticaCancelacionNoEncontradaException(
                        "vigente el " + ahora.toLocalDate()));
        CodigoReserva codigo = generadorCodigo.generar(ahora.toLocalDate());

        Reserva reserva = Reserva.crear(codigo, identificacion, estancia, titular, ocupantes, canalOrigen,
                valor, politica.getVersion(), ahora);
        if (horaEstimadaLlegada != null) {
            reserva.indicarHoraEstimadaLlegada(horaEstimadaLlegada, titular.getNombre(), ahora);
        }

        Reserva guardada = reservaRepository.guardar(reserva);
        folioRepository.guardar(new Folio(codigo, new Cargo(CONCEPTO_ALOJAMIENTO, valor.total())));
        return guardada;
    }
}
