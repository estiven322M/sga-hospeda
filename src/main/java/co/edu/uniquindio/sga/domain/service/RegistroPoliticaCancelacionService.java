package co.edu.uniquindio.sga.domain.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import co.edu.uniquindio.sga.domain.entity.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga.domain.valueobject.TramoRetencion;
import co.edu.uniquindio.sga.domain.valueobject.VersionPolitica;

/**
 * F-07: existe una única política de cancelación por alojamiento, versionada en el tiempo. Actualizarla
 * crea una versión nueva (las reservas ya creadas conservan la suya, RN-13) con el número siguiente.
 * Una política no puede saber cuál es la anterior, por eso este servicio asigna la versión y exige que la
 * nueva rija desde una fecha posterior a la de la anterior, para que "la vigente en una fecha" sea inequívoca.
 * Los tramos (mínimo dos, con uno de antelación 0) los valida el constructor de PoliticaCancelacion.
 */
public class RegistroPoliticaCancelacionService {

    private final PoliticaCancelacionRepository politicaRepository;

    public RegistroPoliticaCancelacionService(PoliticaCancelacionRepository politicaRepository) {
        this.politicaRepository = politicaRepository;
    }

    public PoliticaCancelacion crearNuevaVersion(LocalDate vigenteDesde, List<TramoRetencion> tramos) {
        Optional<PoliticaCancelacion> ultima = politicaRepository.ultimaVersion();
        int numero = ultima.map(p -> p.getVersion().numero() + 1).orElse(1);
        PoliticaCancelacion nueva = new PoliticaCancelacion(new VersionPolitica(numero), vigenteDesde, tramos);
        if (ultima.isPresent() && !vigenteDesde.isAfter(ultima.get().getVigenteDesde())) {
            throw new ReglaDominioException("La nueva versión debe regir desde una fecha posterior al "
                    + ultima.get().getVigenteDesde() + ", cuando empezó la versión "
                    + ultima.get().getVersion().numero());
        }
        return nueva;
    }
}
