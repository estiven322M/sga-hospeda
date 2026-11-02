package co.edu.uniquindio.sga.domain.repository;

import java.util.List;
import co.edu.uniquindio.sga.domain.entity.Temporada;

/** Puerto de salida de las temporadas. Sin eliminación física (regla 11). */
public interface TemporadaRepository {

    Temporada guardar(Temporada temporada);

    List<Temporada> listarTodas();
}
