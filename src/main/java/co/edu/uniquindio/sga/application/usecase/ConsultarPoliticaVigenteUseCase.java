package co.edu.uniquindio.sga.application.usecase;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.PoliticaCancelacionNoEncontradaException;
import co.edu.uniquindio.sga.domain.entity.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.repository.PoliticaCancelacionRepository;

/** Consulta de la política de cancelación vigente en una fecha. Solo lectura; 404 si no hay ninguna. */
@Service
public class ConsultarPoliticaVigenteUseCase {

    private final PoliticaCancelacionRepository politicaRepository;

    public ConsultarPoliticaVigenteUseCase(PoliticaCancelacionRepository politicaRepository) {
        this.politicaRepository = politicaRepository;
    }

    @Transactional(readOnly = true)
    public PoliticaCancelacion ejecutar(LocalDate fecha) {
        return politicaRepository.vigenteEn(fecha)
                .orElseThrow(() -> new PoliticaCancelacionNoEncontradaException("vigente el " + fecha));
    }
}
