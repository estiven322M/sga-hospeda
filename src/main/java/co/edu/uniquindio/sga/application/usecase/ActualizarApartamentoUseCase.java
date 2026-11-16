package co.edu.uniquindio.sga.application.usecase;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.service.ControlCapacidadService;
import co.edu.uniquindio.sga.domain.valueobject.Caracteristica;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

/**
 * Editar los datos de un apartamento (nombre, descripción, dormitorios, capacidad, dotación y características).
 * La identificación es estable y no cambia (L-04). Cambiar la capacidad no afecta las reservas ya creadas (7.3): cada
 * reserva conserva su grupo. Si se BAJA la capacidad, el resultado incluye las reservas activas que quedan por encima de
 * la nueva capacidad, para advertir al administrador; la actualización nunca se bloquea.
 */
@Service
public class ActualizarApartamentoUseCase {

    /** El apartamento actualizado y, si se bajó la capacidad, las reservas que quedaron por encima de ella. */
    public record Resultado(Apartamento apartamento, List<Reserva> reservasPorEncimaDeLaCapacidad) {
    }

    private final ApartamentoRepository apartamentoRepository;
    private final ControlCapacidadService controlCapacidad;
    private final Clock reloj;

    public ActualizarApartamentoUseCase(ApartamentoRepository apartamentoRepository,
                                        ControlCapacidadService controlCapacidad, Clock reloj) {
        this.apartamentoRepository = apartamentoRepository;
        this.controlCapacidad = controlCapacidad;
        this.reloj = reloj;
    }

    @Transactional
    public Resultado ejecutar(IdentificacionApartamento identificacion, String nombre, String descripcion,
                              int dormitorios, int capacidad, Collection<String> dotacion,
                              Collection<Caracteristica> caracteristicas) {
        Apartamento apartamento = apartamentoRepository.buscarPorIdentificacion(identificacion)
                .orElseThrow(() -> new ApartamentoNoEncontradoException(identificacion));
        int capacidadAnterior = apartamento.getCapacidad();
        apartamento.actualizarDatos(nombre, descripcion, dormitorios, capacidad, dotacion, caracteristicas);
        Apartamento guardado = apartamentoRepository.guardar(apartamento);
        List<Reserva> excedidas = capacidad < capacidadAnterior
                ? controlCapacidad.reservasPorEncimaDeLaCapacidad(guardado, LocalDate.now(reloj))
                : List.of();
        return new Resultado(guardado, excedidas);
    }
}
