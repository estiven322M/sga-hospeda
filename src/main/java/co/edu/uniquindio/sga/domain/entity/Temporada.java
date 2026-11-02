package co.edu.uniquindio.sga.domain.entity;

import java.time.LocalDate;
import java.util.List;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.Periodo;

/**
 * Periodo del calendario con tarifas propias (7.4). Se identifica por su nombre.
 * La temporada base no tiene periodos: cubre toda fecha que ninguna otra temporada reclama.
 */
public class Temporada {

    private final String nombre;
    private final boolean base;
    private final List<Periodo> periodos;

    public Temporada(String nombre, boolean base, List<Periodo> periodos) {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDominioException("La temporada debe tener un nombre");
        }
        List<Periodo> lista = periodos == null ? List.of() : List.copyOf(periodos);
        if (base && !lista.isEmpty()) {
            throw new ReglaDominioException("La temporada base cubre las fechas restantes y no define periodos");
        }
        if (!base && lista.isEmpty()) {
            throw new ReglaDominioException("Una temporada que no es base debe definir al menos un periodo");
        }
        for (int i = 0; i < lista.size(); i++) {
            for (int j = i + 1; j < lista.size(); j++) {
                if (lista.get(i).seSolapaCon(lista.get(j))) {
                    throw new ReglaDominioException("Los periodos de la temporada " + nombre + " no pueden solaparse");
                }
            }
        }
        this.nombre = nombre.trim();
        this.base = base;
        this.periodos = lista;
    }

    /** Una temporada no base reclama la fecha si algún periodo la incluye; la base nunca reclama por sí misma. */
    public boolean reclama(LocalDate fecha) {
        return periodos.stream().anyMatch(p -> p.incluye(fecha));
    }

    public boolean seSolapaCon(Temporada otra) {
        return periodos.stream().anyMatch(p -> otra.periodos.stream().anyMatch(p::seSolapaCon));
    }

    public String getNombre() { return nombre; }
    public boolean esBase() { return base; }
    public List<Periodo> getPeriodos() { return periodos; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Temporada otra)) return false;
        return nombre.equals(otra.nombre);
    }

    @Override
    public int hashCode() {
        return nombre.hashCode();
    }
}
