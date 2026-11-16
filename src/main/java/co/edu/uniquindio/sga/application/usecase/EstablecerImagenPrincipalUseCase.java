package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.application.exception.ImagenNoEncontradaException;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

/** 7.3: elegir la imagen principal de un apartamento. El dominio garantiza que siempre haya exactamente una. */
@Service
public class EstablecerImagenPrincipalUseCase {

    private final ApartamentoRepository apartamentoRepository;

    public EstablecerImagenPrincipalUseCase(ApartamentoRepository apartamentoRepository) {
        this.apartamentoRepository = apartamentoRepository;
    }

    @Transactional
    public Apartamento ejecutar(IdentificacionApartamento identificacion, String idImagen) {
        Apartamento apartamento = apartamentoRepository.buscarPorIdentificacion(identificacion)
                .orElseThrow(() -> new ApartamentoNoEncontradoException(identificacion));
        apartamento.buscarImagen(idImagen).orElseThrow(() -> new ImagenNoEncontradaException(identificacion, idImagen));
        apartamento.establecerImagenPrincipal(idImagen);
        return apartamentoRepository.guardar(apartamento);
    }
}
