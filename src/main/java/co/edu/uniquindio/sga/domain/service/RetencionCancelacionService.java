package co.edu.uniquindio.sga.domain.service;

import java.time.Duration;
import java.time.LocalDateTime;

import co.edu.uniquindio.sga.domain.entity.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ConfiguracionAlojamiento;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;

/**
 * Regla que ni la reserva ni la política pueden evaluar solas: la retención por cancelación se calcula
 * con la política congelada en la reserva (RN-13), no con la vigente, y la antelación se mide desde el
 * momento de la cancelación hasta la hora de entrada del alojamiento el día de la entrada.
 */
public class RetencionCancelacionService {

    private final ConfiguracionAlojamiento configuracion;

    public RetencionCancelacionService(ConfiguracionAlojamiento configuracion) {
        this.configuracion = configuracion;
    }

    /**
     * No-show: aplica la consecuencia de la política congelada en la reserva (RN-13). Como el no-show ocurre
     * pasada la hora de entrada, la antelación es nula y rige el tramo de antelación 0 de la política.
     */
    public Dinero retencionPorNoShow(Reserva reserva, PoliticaCancelacion politica) {
        verificarPoliticaCongelada(reserva, politica);
        return politica.retencion(reserva.getValor().total(), Duration.ZERO);
    }

    public Dinero retencionPorCancelacion(Reserva reserva, PoliticaCancelacion politica, LocalDateTime ahora) {
        verificarPoliticaCongelada(reserva, politica);
        LocalDateTime entrada = reserva.getEstancia().fechaEntrada().atTime(configuracion.horaEntrada());
        return politica.retencion(reserva.getValor().total(), Duration.between(ahora, entrada));
    }

    private void verificarPoliticaCongelada(Reserva reserva, PoliticaCancelacion politica) {
        if (!politica.getVersion().equals(reserva.getPolitica())) {
            throw new ReglaDominioException("La retención debe calcularse con la política congelada en la reserva (versión "
                    + reserva.getPolitica().numero() + ")");
        }
    }
}
