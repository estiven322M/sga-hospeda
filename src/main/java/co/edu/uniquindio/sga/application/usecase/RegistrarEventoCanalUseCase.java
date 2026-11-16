package co.edu.uniquindio.sga.application.usecase;

import java.time.Clock;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import co.edu.uniquindio.sga.domain.entity.EventoCanal;
import co.edu.uniquindio.sga.domain.repository.BitacoraCanalRepository;
import co.edu.uniquindio.sga.domain.valueobject.OperacionCanal;
import co.edu.uniquindio.sga.domain.valueobject.ResultadoCanal;

/**
 * Deja constancia en la bitácora de un mensaje intercambiado con un canal (7.8). No es transaccional y NO lanza nunca: un fallo al
 * registrar no puede cambiar la respuesta que recibe el canal ni deshacer una reserva ya creada. Cada registro se guarda en su propia
 * transacción, así que queda aunque la operación de negocio haya revertido la suya.
 */
@Service
public class RegistrarEventoCanalUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegistrarEventoCanalUseCase.class);
    /**
     * ¿Se registró algo en esta petición? Lo usa el filtro del canal para no dejar sin constancia un mensaje que falló ANTES de llegar
     * al caso de uso (un cuerpo mal armado, una fecha imposible al convertirla) sin duplicar el que sí se registró. Una petición se
     * atiende en un solo hilo, así que un ThreadLocal es suficiente; el filtro lo limpia siempre.
     */
    private static final ThreadLocal<Boolean> REGISTRADO = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /** El filtro lo llama al empezar y al terminar cada petición del canal. */
    public static void reiniciarConstancia() {
        REGISTRADO.remove();
    }

    public static boolean huboRegistro() {
        return REGISTRADO.get();
    }

    private final BitacoraCanalRepository bitacora;
    private final Clock reloj;

    public RegistrarEventoCanalUseCase(BitacoraCanalRepository bitacora, Clock reloj) {
        this.bitacora = bitacora;
        this.reloj = reloj;
    }

    public void ejecutar(String canal, OperacionCanal operacion, String identificadorExterno, ResultadoCanal resultado,
                         int codigoHttp, String detalle) {
        REGISTRADO.set(Boolean.TRUE);
        try {
            bitacora.registrar(new EventoCanal(LocalDateTime.now(reloj), canal, operacion, identificadorExterno, resultado,
                    codigoHttp, detalle));
        } catch (RuntimeException e) {
            log.warn("No se pudo registrar en la bitácora el mensaje {} del canal {}: {}", operacion, canal, e.getMessage());
        }
    }
}
