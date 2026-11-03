package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

/** CU-10 Preparar apartamento, segundo paso: EN_PREPARACION → PREPARADO (queda listo para recibir un grupo, RN-11). */
@Service
public class MarcarPreparadoUseCase {

    private final ApartamentoRepository apartamentoRepository;

    public MarcarPreparadoUseCase(ApartamentoRepository apartamentoRepository) {
        this.apartamentoRepository = apartamentoRepository;
    }

    @Transactional
    public Apartamento ejecutar(IdentificacionApartamento identificacion) {
        Apartamento apartamento = apartamentoRepository.buscarPorIdentificacion(identificacion)
                .orElseThrow(() -> new ApartamentoNoEncontradoException(identificacion));
        apartamento.marcarPreparado();
        return apartamentoRepository.guardar(apartamento);
    }
}
