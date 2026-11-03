package co.edu.uniquindio.sga.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ConfiguracionAlojamiento;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Pago;

/**
 * 8 (transiciones): PENDIENTE → CONFIRMADA es "confirmación, con anticipo si el equipo lo exige" (L-11).
 * La reserva no conoce el folio y el folio no conoce el porcentaje de anticipo, por eso la regla vive en un
 * servicio de dominio: no se confirma mientras lo pagado en el folio no cubra el anticipo configurado.
 * Un porcentaje de 0 significa que el alojamiento no exige anticipo.
 * El resto de las condiciones (hora estimada de llegada, RN-09, y la transición, RN-08) las sigue protegiendo Reserva.
 */
public class ConfirmacionReservaService {

    private final ConfiguracionAlojamiento configuracion;

    public ConfirmacionReservaService(ConfiguracionAlojamiento configuracion) {
        this.configuracion = configuracion;
    }

    /** Anticipo = porcentaje configurado del valor congelado de la reserva, redondeado al peso (regla 14). */
    public Dinero anticipoRequerido(Reserva reserva) {
        int porcentaje = configuracion.anticipoPorcentaje();
        if (porcentaje < 0 || porcentaje > 100) {
            throw new ReglaDominioException("El porcentaje de anticipo configurado debe estar entre 0 y 100");
        }
        BigDecimal valor = reserva.getValor().total().valor().multiply(BigDecimal.valueOf(porcentaje))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        return new Dinero(valor);
    }

    public void confirmar(Reserva reserva, Folio folio, String autor, LocalDateTime ahora) {
        if (!folio.getReserva().equals(reserva.getCodigo())) {
            throw new ReglaDominioException("El folio no pertenece a la reserva que se quiere confirmar");
        }
        // RN-08: se revisa primero la transición para no pedir un anticipo a una reserva que no puede confirmarse
        if (!reserva.getEstado().puedeTransicionarA(EstadoReserva.CONFIRMADA)) {
            throw new ReglaDominioException("No se puede pasar de " + reserva.getEstado() + " a "
                    + EstadoReserva.CONFIRMADA);
        }
        Dinero requerido = anticipoRequerido(reserva);
        Dinero pagado = folio.obtenerPagos().stream().map(Pago::valor).reduce(Dinero.CERO, Dinero::mas);
        if (pagado.valor().compareTo(requerido.valor()) < 0) {
            throw new ReglaDominioException("Para confirmar la reserva debe registrarse un anticipo de "
                    + requerido.valor() + " COP (" + configuracion.anticipoPorcentaje() + " % del valor); "
                    + "hasta ahora se han pagado " + pagado.valor() + " COP");
        }
        reserva.confirmar(autor, ahora);
    }
}
