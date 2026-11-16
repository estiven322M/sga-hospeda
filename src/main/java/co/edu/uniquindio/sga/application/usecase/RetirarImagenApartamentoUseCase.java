package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.application.exception.ImagenNoEncontradaException;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

/**
 * 7.3: retirar una imagen de la galería de un apartamento. Es LÓGICO (regla 11): el registro se conserva y el archivo del almacén
 * no se borra. El dominio impide dejar sin imágenes a un apartamento activo y reasigna la principal si se retira esa.
 */
@Service
public class RetirarImagenApartamentoUseCase {

    private final ApartamentoRepository apartamentoRepository;

    public RetirarImagenApartamentoUseCase(ApartamentoRepository apartamentoRepository) {
        this.apartamentoRepository = apartamentoRepository;
    }

    @Transactional
    public Apartamento ejecutar(IdentificacionApartamento identificacion, String idImagen) {
        Apartamento apartamento = apartamentoRepository.buscarPorIdentificacion(identificacion)
                .orElseThrow(() -> new ApartamentoNoEncontradoException(identificacion));
        apartamento.buscarImagen(idImagen).orElseThrow(() -> new ImagenNoEncontradaException(identificacion, idImagen));
        apartamento.retirarImagen(idImagen);
        return apartamentoRepository.guardar(apartamento);
    }
}
