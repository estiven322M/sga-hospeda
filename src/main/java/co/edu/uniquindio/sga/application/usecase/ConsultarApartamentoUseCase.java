package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

/** Consulta de un apartamento por su identificación. Solo lectura; 404 si no existe. */
@Service
public class ConsultarApartamentoUseCase {

    private final ApartamentoRepository apartamentoRepository;

    public ConsultarApartamentoUseCase(ApartamentoRepository apartamentoRepository) {
        this.apartamentoRepository = apartamentoRepository;
    }

    @Transactional(readOnly = true)
    public Apartamento ejecutar(IdentificacionApartamento identificacion) {
        return apartamentoRepository.buscarPorIdentificacion(identificacion)
                .orElseThrow(() -> new ApartamentoNoEncontradoException(identificacion));
    }
}
