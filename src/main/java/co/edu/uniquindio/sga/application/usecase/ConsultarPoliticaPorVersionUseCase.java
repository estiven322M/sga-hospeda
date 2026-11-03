package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.PoliticaCancelacionNoEncontradaException;
import co.edu.uniquindio.sga.domain.entity.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.repository.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga.domain.valueobject.VersionPolitica;

/** Consulta de una versión concreta de la política (la que una reserva tiene congelada). 404 si no existe. */
@Service
public class ConsultarPoliticaPorVersionUseCase {

    private final PoliticaCancelacionRepository politicaRepository;

    public ConsultarPoliticaPorVersionUseCase(PoliticaCancelacionRepository politicaRepository) {
        this.politicaRepository = politicaRepository;
    }

    @Transactional(readOnly = true)
    public PoliticaCancelacion ejecutar(VersionPolitica version) {
        return politicaRepository.porVersion(version)
                .orElseThrow(() -> new PoliticaCancelacionNoEncontradaException("versión " + version.numero()));
    }
}
