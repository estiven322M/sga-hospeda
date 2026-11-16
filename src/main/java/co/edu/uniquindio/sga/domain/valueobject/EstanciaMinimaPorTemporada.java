package co.edu.uniquindio.sga.domain.valueobject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/**
 * RP-01 (L-20): número mínimo de noches que exige cada temporada. Si alguna noche de la estancia cae en una temporada
 * con mínimo, la estancia completa debe tener al menos ese número de noches. Las temporadas que no figuran no exigen
 * nada (mínimo 1, que ya garantiza RN-03). Los valores salen de application.properties (regla 10).
 *
 * El nombre de la temporada se compara sin distinguir mayúsculas. Si varias temporadas de la estancia tienen mínimo, se
 * exige el mayor (y el mensaje nombra esa temporada).
 */
public record EstanciaMinimaPorTemporada(Map<String, Integer> minimos) {

    public static final EstanciaMinimaPorTemporada SIN_MINIMOS = new EstanciaMinimaPorTemporada(Map.of());

    public EstanciaMinimaPorTemporada {
        Map<String, Integer> normalizado = new HashMap<>();
        if (minimos != null) {
            minimos.forEach((temporada, noches) -> {
                if (temporada == null || temporada.isBlank()) {
                    throw new ReglaDominioException("La estancia mínima debe indicar el nombre de la temporada");
                }
                if (noches == null || noches < 1) {
                    throw new ReglaDominioException(
                            "La estancia mínima de la temporada " + temporada + " debe ser de al menos 1 noche");
                }
                if (normalizado.put(temporada.trim().toUpperCase(), noches) != null) {
                    throw new ReglaDominioException("La temporada " + temporada.trim() + " tiene dos estancias mínimas");
                }
            });
        }
        minimos = Map.copyOf(normalizado);
    }

    /** Mínimo de noches que exige la temporada (1 si no exige ninguno). */
    public int minimoDe(String temporada) {
        return temporada == null ? 1 : minimos.getOrDefault(temporada.trim().toUpperCase(), 1);
    }

    /**
     * RP-01: verifica el desglose noche por noche de una estancia. Lanza la regla violada si el número de noches es
     * menor que el mínimo de alguna de las temporadas por las que pasa la estancia.
     */
    public void verificar(List<CargoNoche> detalle) {
        int noches = detalle.size();
        String temporadaExigente = null;
        int exigido = 1;
        for (CargoNoche cargo : detalle) {
            int minimo = minimoDe(cargo.temporada());
            if (minimo > exigido) {
                exigido = minimo;
                temporadaExigente = cargo.temporada();
            }
        }
        if (noches < exigido) {
            throw new ReglaDominioException(
                    "En temporada " + temporadaExigente + " la estancia mínima es de " + exigido + " noches");
        }
    }
}
