package co.edu.uniquindio.sga.application.usecase;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.Novedad;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.NovedadRepository;
import co.edu.uniquindio.sga.domain.valueobject.GravedadNovedad;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

/**
 * Registra una novedad sobre un apartamento (7.6). Solo orquesta: comprueba que el apartamento exista (404), toma la fecha del
 * reloj del sistema y deja que {@link Novedad} valide sus reglas (409). No cambia el estado operativo del apartamento.
 */
@Service
public class RegistrarNovedadUseCase {

    private final ApartamentoRepository apartamentoRepository;
    private final NovedadRepository novedadRepository;
    private final Clock reloj;

    public RegistrarNovedadUseCase(ApartamentoRepository apartamentoRepository, NovedadRepository novedadRepository,
                                   Clock reloj) {
        this.apartamentoRepository = apartamentoRepository;
        this.novedadRepository = novedadRepository;
        this.reloj = reloj;
    }

    @Transactional
    public Novedad ejecutar(IdentificacionApartamento apartamento, String autor, String descripcion,
                            GravedadNovedad gravedad) {
        apartamentoRepository.buscarPorIdentificacion(apartamento)
                .orElseThrow(() -> new ApartamentoNoEncontradoException(apartamento));
        return novedadRepository.registrar(
                new Novedad(apartamento, LocalDateTime.now(reloj), autor, descripcion, gravedad));
    }
}
