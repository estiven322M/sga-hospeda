package co.edu.uniquindio.sga.application.usecase;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import co.edu.uniquindio.sga.application.exception.ReservaExternaNoEncontradaException;
import co.edu.uniquindio.sga.domain.entity.CanalVenta;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.domain.valueobject.OperacionCanal;
import co.edu.uniquindio.sga.domain.valueobject.ResultadoCanal;

/**
 * 10.3.1, operación 3: un canal cancela una reserva que ÉL creó. Solo ve las suyas: la reserva se busca por el identificador con el
 * código del canal por delante, así un canal no puede cancelar la de otro (404). Reutiliza CancelarReservaUseCase (política de
 * cancelación congelada, retención en el folio, liberación inmediata). Es idempotente: repetir la cancelación de una reserva ya
 * cancelada responde 200 con el mismo resultado y no vuelve a cobrar ni a avisar. Avisa al titular después de confirmar (J7).
 */
@Service
public class CancelarReservaExternaUseCase {

    private static final String MOTIVO_POR_OMISION = "Cancelada por el canal";

    private final ReservaRepository reservas;
    private final CancelarReservaUseCase cancelar;
    private final NotificarReservaUseCase notificar;
    private final RegistrarEventoCanalUseCase bitacora;

    public CancelarReservaExternaUseCase(ReservaRepository reservas, CancelarReservaUseCase cancelar,
                                         NotificarReservaUseCase notificar, RegistrarEventoCanalUseCase bitacora) {
        this.reservas = reservas;
        this.cancelar = cancelar;
        this.notificar = notificar;
        this.bitacora = bitacora;
    }

    public Reserva ejecutar(CanalVenta canal, String identificadorPropio, String motivo, LocalDateTime ahora) {
        String codigoCanal = canal.getCodigo();
        Reserva reserva;
        try {
            reserva = reservas.buscarPorCanalEIdentificadorExterno(CanalOrigen.EXTERNO, canal.identificadorPara(identificadorPropio))
                    .orElseThrow(() -> new ReservaExternaNoEncontradaException(identificadorPropio));
        } catch (ReservaExternaNoEncontradaException noExiste) {
            bitacora.ejecutar(codigoCanal, OperacionCanal.CANCELAR_RESERVA, identificadorPropio, ResultadoCanal.NO_ENCONTRADA, 404, noExiste.getMessage());
            throw noExiste;
        } catch (ReglaDominioException regla) {
            bitacora.ejecutar(codigoCanal, OperacionCanal.CANCELAR_RESERVA, identificadorPropio, ResultadoCanal.RECHAZADA, 409, regla.getMessage());
            throw regla;
        }
        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            bitacora.ejecutar(codigoCanal, OperacionCanal.CANCELAR_RESERVA, identificadorPropio, ResultadoCanal.ACEPTADA, 200,
                    "La reserva " + reserva.getCodigo().valor() + " ya estaba cancelada");
            return reserva;
        }
        try {
            Reserva cancelada = cancelar.ejecutar(reserva.getCodigo(), motivo == null || motivo.isBlank() ? MOTIVO_POR_OMISION : motivo,
                    "Canal " + codigoCanal, ahora);
            bitacora.ejecutar(codigoCanal, OperacionCanal.CANCELAR_RESERVA, identificadorPropio, ResultadoCanal.ACEPTADA, 200,
                    "Reserva " + cancelada.getCodigo().valor() + " cancelada");
            notificar.reservaCancelada(cancelada, ahora);
            return cancelada;
        } catch (ReglaDominioException regla) {
            bitacora.ejecutar(codigoCanal, OperacionCanal.CANCELAR_RESERVA, identificadorPropio, ResultadoCanal.RECHAZADA, 409, regla.getMessage());
            throw regla;
        }
    }
}
