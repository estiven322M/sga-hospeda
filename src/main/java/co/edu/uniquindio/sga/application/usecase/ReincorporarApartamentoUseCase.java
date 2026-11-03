package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

/** Reincorpora al servicio un apartamento fuera de servicio: FUERA_DE_SERVICIO → PENDIENTE_PREPARACION. */
@Service
public class ReincorporarApartamentoUseCase {

    private final ApartamentoRepository apartamentoRepository;

    public ReincorporarApartamentoUseCase(ApartamentoRepository apartamentoRepository) {
        this.apartamentoRepository = apartamentoRepository;
    }

    @Transactional
    public Apartamento ejecutar(IdentificacionApartamento identificacion) {
        Apartamento apartamento = apartamentoRepository.buscarPorIdentificacion(identificacion)
                .orElseThrow(() -> new ApartamentoNoEncontradoException(identificacion));
        apartamento.reincorporarAlServicio();
        return apartamentoRepository.guardar(apartamento);
    }
}
