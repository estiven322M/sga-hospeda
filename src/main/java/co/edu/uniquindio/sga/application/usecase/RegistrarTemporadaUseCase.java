package co.edu.uniquindio.sga.application.usecase;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.domain.entity.Temporada;
import co.edu.uniquindio.sga.domain.repository.TemporadaRepository;
import co.edu.uniquindio.sga.domain.service.CalendarioTemporadas;

/**
 * Registrar una temporada (7.4). Orquesta: carga las temporadas existentes y le pide a CalendarioTemporadas
 * (dominio) que valide el calendario resultante: sin solapes, sin nombres repetidos y con exactamente una
 * temporada base. Si el calendario no es válido, no se guarda nada. La temporada base debe registrarse primero.
 */
@Service
public class RegistrarTemporadaUseCase {

    private final TemporadaRepository temporadaRepository;

    public RegistrarTemporadaUseCase(TemporadaRepository temporadaRepository) {
        this.temporadaRepository = temporadaRepository;
    }

    @Transactional
    public Temporada ejecutar(Temporada nueva) {
        List<Temporada> calendario = new ArrayList<>(temporadaRepository.listarTodas());
        calendario.add(nueva);
        new CalendarioTemporadas(calendario); // lanza ReglaDominioException si el calendario resultante es inválido
        return temporadaRepository.guardar(nueva);
    }
}
