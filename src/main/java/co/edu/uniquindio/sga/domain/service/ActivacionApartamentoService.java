package co.edu.uniquindio.sga.domain.service;

import java.util.Set;
import java.util.stream.Collectors;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Temporada;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.TarifarioRepository;
import co.edu.uniquindio.sga.domain.repository.TemporadaRepository;
import co.edu.uniquindio.sga.domain.valueobject.Tarifa;

/**
 * 7.4: todo apartamento activo debe tener tarifa en todas las temporadas; el sistema impide
 * activar un apartamento sin tarifas completas. 7.3: tampoco se activa sin al menos una imagen. Necesita conocer temporadas y tarifas (otros agregados).
 */
public class ActivacionApartamentoService {

    private final TarifarioRepository tarifarioRepository;
    private final TemporadaRepository temporadaRepository;

    public ActivacionApartamentoService(TarifarioRepository tarifarioRepository,
                                        TemporadaRepository temporadaRepository) {
        this.tarifarioRepository = tarifarioRepository;
        this.temporadaRepository = temporadaRepository;
    }

    public void activar(Apartamento apartamento) {
        Set<String> conTarifa = tarifarioRepository.listarTarifas(apartamento.getIdentificacion()).stream()
                .map(Tarifa::temporada).collect(Collectors.toSet());
        String faltantes = temporadaRepository.listarTodas().stream()
                .map(Temporada::getNombre)
                .filter(nombre -> !conTarifa.contains(nombre))
                .collect(Collectors.joining(", "));
        if (!faltantes.isEmpty()) {
            throw new ReglaDominioException(
                    "No se puede activar el apartamento: faltan tarifas para las temporadas " + faltantes);
        }
        // 7.3: un apartamento activo tiene entre 1 y 10 imágenes; sin ninguna no puede venderse
        if (apartamento.cantidadImagenes() < 1) {
            throw new ReglaDominioException("No se puede activar el apartamento: debe tener al menos una imagen");
        }
        apartamento.activar();
    }
}
