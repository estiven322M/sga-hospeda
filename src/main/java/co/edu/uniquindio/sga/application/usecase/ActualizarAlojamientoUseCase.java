package co.edu.uniquindio.sga.application.usecase;

import java.time.LocalTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.AlojamientoNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.Alojamiento;
import co.edu.uniquindio.sga.domain.repository.AlojamientoRepository;
import co.edu.uniquindio.sga.domain.valueobject.Ubicacion;

/**
 * Editar los datos del alojamiento (7.3). Orquesta: busca el alojamiento (404) y delega en el dominio, que valida todos
 * los datos (horas de entrada y salida, ubicación, normas, política de mascotas) y no modifica nada si alguno es inválido.
 */
@Service
public class ActualizarAlojamientoUseCase {

    private final AlojamientoRepository alojamientoRepository;

    public ActualizarAlojamientoUseCase(AlojamientoRepository alojamientoRepository) {
        this.alojamientoRepository = alojamientoRepository;
    }

    @Transactional
    public Alojamiento ejecutar(String nombre, String descripcion, String ciudad, String direccion, Ubicacion ubicacion,
                                LocalTime horaEntrada, LocalTime horaSalida, String normasConvivencia,
                                String politicaMascotas) {
        Alojamiento alojamiento = alojamientoRepository.obtener().orElseThrow(AlojamientoNoEncontradoException::new);
        alojamiento.actualizarDatos(nombre, descripcion, ciudad, direccion, ubicacion, horaEntrada, horaSalida,
                normasConvivencia, politicaMascotas);
        return alojamientoRepository.guardar(alojamiento);
    }
}
