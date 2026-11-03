package co.edu.uniquindio.sga.application.usecase;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.application.exception.FolioNoEncontradoException;
import co.edu.uniquindio.sga.application.exception.ReservaNoEncontradaException;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.entity.Ocupante;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.service.CotizadorEstancia;
import co.edu.uniquindio.sga.domain.service.DisponibilidadApartamentoService;
import co.edu.uniquindio.sga.domain.valueobject.Ajuste;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.ValorCongelado;

/**
 * CU-04 Modificar reserva (fechas, ocupantes y, opcionalmente, el apartamento). Orquesta, no decide:
 *  - la disponibilidad se revalida con DisponibilidadApartamentoService excluyendo la propia reserva,
 *    para que no compita consigo misma (RN-01, RN-02, RN-07, RN-20; RN-14 repite las condiciones de creación);
 *  - el nuevo valor se recalcula con las tarifas vigentes (RN-05, RN-06);
 *  - Reserva.modificarEstancia valida el estado (solo antes de iniciar), RN-04 y el titular, conserva la
 *    política congelada y devuelve la diferencia (RN-14, RN-22);
 *  - la diferencia, positiva o negativa, queda como ajuste en el folio sin tocar el cargo original (RN-16).
 */
@Service
public class ModificarReservaUseCase {

    private static final String CONCEPTO_AJUSTE = "Ajuste por modificación de la reserva";

    private final ReservaRepository reservaRepository;
    private final FolioRepository folioRepository;
    private final ApartamentoRepository apartamentoRepository;
    private final DisponibilidadApartamentoService disponibilidadService;
    private final CotizadorEstancia cotizador;

    public ModificarReservaUseCase(ReservaRepository reservaRepository, FolioRepository folioRepository,
                                   ApartamentoRepository apartamentoRepository,
                                   DisponibilidadApartamentoService disponibilidadService,
                                   CotizadorEstancia cotizador) {
        this.reservaRepository = reservaRepository;
        this.folioRepository = folioRepository;
        this.apartamentoRepository = apartamentoRepository;
        this.disponibilidadService = disponibilidadService;
        this.cotizador = cotizador;
    }

    /** Modificación sin cambio de apartamento. */
    public Reserva ejecutar(CodigoReserva codigo, Estancia nuevaEstancia, List<Ocupante> nuevosOcupantes,
                            String autor, LocalDateTime ahora) {
        return ejecutar(codigo, null, nuevaEstancia, nuevosOcupantes, autor, ahora);
    }

    /** {@code nuevoApartamento} nulo significa que la reserva conserva su apartamento (RN-14). */
    @Transactional
    public Reserva ejecutar(CodigoReserva codigo, IdentificacionApartamento nuevoApartamento, Estancia nuevaEstancia,
                            List<Ocupante> nuevosOcupantes, String autor, LocalDateTime ahora) {
        Reserva reserva = reservaRepository.buscarPorCodigo(codigo)
                .orElseThrow(() -> new ReservaNoEncontradaException(codigo));
        Folio folio = folioRepository.buscarPorReserva(codigo)
                .orElseThrow(() -> new FolioNoEncontradoException(codigo));
        IdentificacionApartamento destino = nuevoApartamento != null ? nuevoApartamento : reserva.getApartamento();
        Apartamento apartamento = apartamentoRepository.buscarPorIdentificacion(destino)
                .orElseThrow(() -> new ApartamentoNoEncontradoException(destino));

        disponibilidadService.verificarDisponible(apartamento, nuevaEstancia, nuevosOcupantes.size(), codigo);
        ValorCongelado nuevoValor = cotizador.cotizar(destino, nuevaEstancia, nuevosOcupantes);

        Dinero diferencia = reserva.modificarEstancia(destino, nuevaEstancia, nuevoValor, nuevosOcupantes, autor, ahora);
        if (!diferencia.esCero()) {
            folio.registrarAjuste(new Ajuste(CONCEPTO_AJUSTE, diferencia));
        }

        Reserva guardada = reservaRepository.guardar(reserva);
        folioRepository.guardar(folio);
        return guardada;
    }
}
