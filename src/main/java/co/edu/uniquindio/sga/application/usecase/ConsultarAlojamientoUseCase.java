package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.AlojamientoNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.Alojamiento;
import co.edu.uniquindio.sga.domain.repository.AlojamientoRepository;

/** Consultar los datos del alojamiento (7.3). Es público: lo usan Inicio, el detalle de un apartamento y el mapa. */
@Service
public class ConsultarAlojamientoUseCase {

    private final AlojamientoRepository alojamientoRepository;

    public ConsultarAlojamientoUseCase(AlojamientoRepository alojamientoRepository) {
        this.alojamientoRepository = alojamientoRepository;
    }

    @Transactional(readOnly = true)
    public Alojamiento ejecutar() {
        return alojamientoRepository.obtener().orElseThrow(AlojamientoNoEncontradoException::new);
    }
}
