package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

/**
 * Editar los datos de un apartamento (nombre, dormitorios, capacidad). La identificación es estable y no cambia
 * (L-04). Cambiar la capacidad no afecta las reservas ya creadas (7.3): cada reserva conserva su grupo.
 */
@Service
public class ActualizarApartamentoUseCase {

    private final ApartamentoRepository apartamentoRepository;

    public ActualizarApartamentoUseCase(ApartamentoRepository apartamentoRepository) {
        this.apartamentoRepository = apartamentoRepository;
    }

    @Transactional
    public Apartamento ejecutar(IdentificacionApartamento identificacion, String nombre, int dormitorios,
                                int capacidad) {
        Apartamento apartamento = apartamentoRepository.buscarPorIdentificacion(identificacion)
                .orElseThrow(() -> new ApartamentoNoEncontradoException(identificacion));
        apartamento.actualizarDatos(nombre, dormitorios, capacidad);
        return apartamentoRepository.guardar(apartamento);
    }
}
