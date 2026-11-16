package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.OperacionCanal;
import co.edu.uniquindio.sga.domain.valueobject.ResultadoCanal;

import co.edu.uniquindio.sga.application.exception.ReservaExternaNoEncontradaException;
import co.edu.uniquindio.sga.domain.entity.CanalVenta;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;

/** Un canal consulta una reserva que él creó, por SU identificador. Solo ve las suyas: la de otro canal es un 404. Queda en la bitácora. */
@Service
public class ConsultarReservaExternaUseCase {

    private final ReservaRepository reservas;
    private final RegistrarEventoCanalUseCase bitacora;

    public ConsultarReservaExternaUseCase(ReservaRepository reservas, RegistrarEventoCanalUseCase bitacora) {
        this.reservas = reservas;
        this.bitacora = bitacora;
    }

    @Transactional(readOnly = true)
    public Reserva ejecutar(CanalVenta canal, String identificadorPropio) {
        try {
            Reserva reserva = reservas.buscarPorCanalEIdentificadorExterno(CanalOrigen.EXTERNO, canal.identificadorPara(identificadorPropio))
                    .orElseThrow(() -> new ReservaExternaNoEncontradaException(identificadorPropio));
            bitacora.ejecutar(canal.getCodigo(), OperacionCanal.CONSULTAR_RESERVA, identificadorPropio, ResultadoCanal.ACEPTADA, 200,
                    "Reserva " + reserva.getCodigo().valor() + " consultada");
            return reserva;
        } catch (ReservaExternaNoEncontradaException noExiste) {
            bitacora.ejecutar(canal.getCodigo(), OperacionCanal.CONSULTAR_RESERVA, identificadorPropio, ResultadoCanal.NO_ENCONTRADA, 404, noExiste.getMessage());
            throw noExiste;
        } catch (ReglaDominioException regla) {
            bitacora.ejecutar(canal.getCodigo(), OperacionCanal.CONSULTAR_RESERVA, identificadorPropio, ResultadoCanal.RECHAZADA, 409, regla.getMessage());
            throw regla;
        }
    }
}
