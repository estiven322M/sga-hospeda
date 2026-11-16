package co.edu.uniquindio.sga.application.usecase;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.uniquindio.sga.application.exception.RecursoNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.CanalVenta;
import co.edu.uniquindio.sga.domain.entity.Ocupante;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.MensajeRepetidoException;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.IdentificadorExterno;
import co.edu.uniquindio.sga.domain.valueobject.OperacionCanal;
import co.edu.uniquindio.sga.domain.valueobject.ResultadoCanal;

/**
 * 10.3.1, operación 2: un canal crea una reserva con SU identificador. Reutiliza CrearReservaUseCase tal como está hoy:
 * <ul>
 *   <li>RN-19: el mismo mensaje (canal + identificador) dos veces no duplica; el dominio responde 409 y la bitácora dice REPETIDA.</li>
 *   <li>RN-18: si choca con una reserva vigente se rechaza con 409, queda el conflicto registrado para el administrador y la reserva
 *       vigente no se toca; la bitácora dice RECHAZADA.</li>
 * </ul>
 * El identificador se guarda con el código del canal por delante ({@link CanalVenta#identificadorPara}), así "canal + identificador"
 * es único aunque dos canales usen el mismo texto. Si se crea, se avisa al titular DESPUÉS de confirmar la transacción (J7). No es
 * transaccional: la bitácora debe quedar también cuando la operación se rechaza.
 */
@Service
public class CrearReservaExternaUseCase {

    private final CrearReservaUseCase crearReserva;
    private final NotificarReservaUseCase notificar;
    private final RegistrarEventoCanalUseCase bitacora;

    public CrearReservaExternaUseCase(CrearReservaUseCase crearReserva, NotificarReservaUseCase notificar,
                                      RegistrarEventoCanalUseCase bitacora) {
        this.crearReserva = crearReserva;
        this.notificar = notificar;
        this.bitacora = bitacora;
    }

    public Reserva ejecutar(CanalVenta canal, String identificadorPropio, IdentificacionApartamento apartamento, Estancia estancia,
                            Ocupante titular, List<Ocupante> ocupantes, LocalTime horaEstimadaLlegada, String correoTitular,
                            LocalDateTime ahora) {
        String codigoCanal = canal.getCodigo();
        try {
            IdentificadorExterno identificador = canal.identificadorPara(identificadorPropio);
            Reserva reserva = crearReserva.ejecutar(apartamento, estancia, titular, ocupantes, CanalOrigen.EXTERNO, identificador,
                    horaEstimadaLlegada, ahora);
            bitacora.ejecutar(codigoCanal, OperacionCanal.CREAR_RESERVA, identificadorPropio, ResultadoCanal.ACEPTADA, 201,
                    "Reserva " + reserva.getCodigo().valor() + " creada");
            notificar.reservaCreada(reserva, correoTitular, ahora);
            return reserva;
        } catch (MensajeRepetidoException repetido) {
            bitacora.ejecutar(codigoCanal, OperacionCanal.CREAR_RESERVA, identificadorPropio, ResultadoCanal.REPETIDA, 409, repetido.getMessage());
            throw repetido;
        } catch (ReglaDominioException regla) {
            bitacora.ejecutar(codigoCanal, OperacionCanal.CREAR_RESERVA, identificadorPropio, ResultadoCanal.RECHAZADA, 409, regla.getMessage());
            throw regla;
        } catch (RecursoNoEncontradoException noExiste) {
            bitacora.ejecutar(codigoCanal, OperacionCanal.CREAR_RESERVA, identificadorPropio, ResultadoCanal.NO_ENCONTRADA, 404, noExiste.getMessage());
            throw noExiste;
        }
    }
}
