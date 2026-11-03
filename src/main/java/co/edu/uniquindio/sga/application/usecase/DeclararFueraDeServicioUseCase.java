package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

/**
 * Declarar un apartamento FUERA_DE_SERVICIO: decisión administrativa, posible desde cualquier estado no ocupado.
 * No crea un bloqueo: un apartamento fuera de servicio sin bloqueo registrado sigue siendo vendible a futuro (3.3),
 * por eso el administrador debe registrar además el bloqueo (CU-09).
 */
@Service
public class DeclararFueraDeServicioUseCase {

    private final ApartamentoRepository apartamentoRepository;

    public DeclararFueraDeServicioUseCase(ApartamentoRepository apartamentoRepository) {
        this.apartamentoRepository = apartamentoRepository;
    }

    @Transactional
    public Apartamento ejecutar(IdentificacionApartamento identificacion) {
        Apartamento apartamento = apartamentoRepository.buscarPorIdentificacion(identificacion)
                .orElseThrow(() -> new ApartamentoNoEncontradoException(identificacion));
        apartamento.declararFueraDeServicio();
        return apartamentoRepository.guardar(apartamento);
    }
}
