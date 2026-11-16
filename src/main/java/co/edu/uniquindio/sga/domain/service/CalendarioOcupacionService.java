package co.edu.uniquindio.sga.domain.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import co.edu.uniquindio.sga.domain.entity.Bloqueo;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.BloqueoRepository;
import co.edu.uniquindio.sga.domain.repository.ConfiguracionAlojamiento;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.NocheCalendario;

/**
 * Calendario de ocupación de un apartamento (7.1, 7.5): el estado de cada noche del rango [desde, hasta). Una noche está
 * RESERVADA si la retiene una reserva activa (RN-12: solo las activas retienen noches), BLOQUEADA si la cubre un bloqueo
 * y LIBRE en cualquier otro caso. El rango no puede superar el máximo de noches configurado (regla 10), para que la
 * consulta siga siendo liviana. Un apartamento no puede tener a la vez una reserva activa y un bloqueo sobre la misma
 * noche (RN-07), así que no hay conflicto entre los dos estados.
 */
public class CalendarioOcupacionService {

    private final ReservaRepository reservas;
    private final BloqueoRepository bloqueos;
    private final ConfiguracionAlojamiento configuracion;

    public CalendarioOcupacionService(ReservaRepository reservas, BloqueoRepository bloqueos,
                                      ConfiguracionAlojamiento configuracion) {
        this.reservas = reservas;
        this.bloqueos = bloqueos;
        this.configuracion = configuracion;
    }

    public List<NocheCalendario> construir(IdentificacionApartamento apartamento, Estancia rango) {
        int maximo = configuracion.calendarioMaximoNoches();
        if (rango.noches() > maximo) {
            throw new ReglaDominioException("El calendario admite como máximo " + maximo + " noches; se pidieron "
                    + rango.noches());
        }
        List<Reserva> activas = reservas.buscarActivasPorApartamento(apartamento);
        List<Bloqueo> vigentes = bloqueos.buscarVigentesPorApartamento(apartamento);

        List<NocheCalendario> calendario = new ArrayList<>();
        for (LocalDate noche : rango.nochesOcupadas()) {
            calendario.add(estadoDe(noche, activas, vigentes));
        }
        return List.copyOf(calendario);
    }

    private static NocheCalendario estadoDe(LocalDate noche, List<Reserva> activas, List<Bloqueo> vigentes) {
        for (Reserva reserva : activas) {
            if (reserva.getEstancia().incluye(noche)) {
                return NocheCalendario.reservada(noche, reserva.getCodigo(), reserva.getEstado());
            }
        }
        for (Bloqueo bloqueo : vigentes) {
            if (bloqueo.getRango().incluye(noche)) {
                return NocheCalendario.bloqueada(noche);
            }
        }
        return NocheCalendario.libre(noche);
    }
}
