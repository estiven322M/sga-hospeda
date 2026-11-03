package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

/** Baja lógica de un apartamento (regla 11): deja de venderse, pero el registro nunca se elimina físicamente. */
@Service
public class DarDeBajaApartamentoUseCase {

    private final ApartamentoRepository apartamentoRepository;

    public DarDeBajaApartamentoUseCase(ApartamentoRepository apartamentoRepository) {
        this.apartamentoRepository = apartamentoRepository;
    }

    @Transactional
    public Apartamento ejecutar(IdentificacionApartamento identificacion) {
        Apartamento apartamento = apartamentoRepository.buscarPorIdentificacion(identificacion)
                .orElseThrow(() -> new ApartamentoNoEncontradoException(identificacion));
        apartamento.darDeBaja();
        return apartamentoRepository.guardar(apartamento);
    }
}
