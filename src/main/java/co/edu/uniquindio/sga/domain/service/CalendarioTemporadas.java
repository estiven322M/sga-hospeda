package co.edu.uniquindio.sga.domain.service;

import java.time.LocalDate;
import java.util.List;

import co.edu.uniquindio.sga.domain.entity.Temporada;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/**
 * Regla que ninguna temporada puede evaluar sola (7.4): las temporadas no se solapan entre sí,
 * existe exactamente una temporada base y por tanto ninguna fecha queda sin temporada.
 */
public class CalendarioTemporadas {

    private final List<Temporada> temporadas;
    private final Temporada base;

    public CalendarioTemporadas(List<Temporada> temporadas) {
        if (temporadas == null || temporadas.isEmpty()) {
            throw new ReglaDominioException("El calendario requiere al menos la temporada base");
        }
        List<Temporada> bases = temporadas.stream().filter(Temporada::esBase).toList();
        if (bases.size() != 1) {
            throw new ReglaDominioException("Debe existir exactamente una temporada base");
        }
        if (temporadas.stream().distinct().count() != temporadas.size()) {
            throw new ReglaDominioException("No puede haber dos temporadas con el mismo nombre");
        }
        for (int i = 0; i < temporadas.size(); i++) {
            for (int j = i + 1; j < temporadas.size(); j++) {
                if (temporadas.get(i).seSolapaCon(temporadas.get(j))) {
                    throw new ReglaDominioException("Las temporadas " + temporadas.get(i).getNombre()
                            + " y " + temporadas.get(j).getNombre() + " se solapan");
                }
            }
        }
        this.temporadas = List.copyOf(temporadas);
        this.base = bases.get(0);
    }

    /** Temporada de una noche: la que la reclama o, si ninguna, la base. */
    public Temporada temporadaDe(LocalDate noche) {
        return temporadas.stream().filter(t -> t.reclama(noche)).findFirst().orElse(base);
    }

    public List<Temporada> temporadas() {
        return temporadas;
    }
}
