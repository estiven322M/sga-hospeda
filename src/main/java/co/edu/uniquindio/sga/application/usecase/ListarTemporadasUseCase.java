package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.domain.entity.Temporada;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.repository.TemporadaRepository;

/** Listado de temporadas, de 10 en 10. Son pocas, así que se pagina en memoria. Solo lectura. */
@Service
public class ListarTemporadasUseCase {

    private final TemporadaRepository temporadaRepository;

    public ListarTemporadasUseCase(TemporadaRepository temporadaRepository) {
        this.temporadaRepository = temporadaRepository;
    }

    @Transactional(readOnly = true)
    public Pagina<Temporada> ejecutar(SolicitudPagina pagina) {
        return Pagina.de(temporadaRepository.listarTodas(), pagina);
    }
}
