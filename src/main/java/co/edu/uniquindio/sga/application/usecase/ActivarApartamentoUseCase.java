package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.service.ActivacionApartamentoService;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

/**
 * Activar un apartamento. Orquesta: ActivacionApartamentoService impide activarlo si le falta la tarifa de
 * alguna temporada (7.4) y Apartamento rechaza activar uno que ya está activo.
 */
@Service
public class ActivarApartamentoUseCase {

    private final ApartamentoRepository apartamentoRepository;
    private final ActivacionApartamentoService activacionService;

    public ActivarApartamentoUseCase(ApartamentoRepository apartamentoRepository,
                                     ActivacionApartamentoService activacionService) {
        this.apartamentoRepository = apartamentoRepository;
        this.activacionService = activacionService;
    }

    @Transactional
    public Apartamento ejecutar(IdentificacionApartamento identificacion) {
        Apartamento apartamento = apartamentoRepository.buscarPorIdentificacion(identificacion)
                .orElseThrow(() -> new ApartamentoNoEncontradoException(identificacion));
        activacionService.activar(apartamento);
        return apartamentoRepository.guardar(apartamento);
    }
}
