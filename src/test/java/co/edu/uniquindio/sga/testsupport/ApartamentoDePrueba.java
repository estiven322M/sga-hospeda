package co.edu.uniquindio.sga.testsupport;

import java.util.List;
import java.util.Set;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.valueobject.Caracteristica;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

/**
 * Fábrica de apartamentos para las pruebas. Desde L-06 el constructor exige descripción, y casi ninguna prueba se
 * ocupa de ella; este helper pone los valores neutros para que cada prueba declare solo lo que le importa.
 */
public final class ApartamentoDePrueba {

    public static final String DESCRIPCION = "Apartamento de prueba";

    private ApartamentoDePrueba() {
    }

    /** Sin dotación ni características. */
    public static Apartamento nuevo(IdentificacionApartamento id, String nombre, int dormitorios, int capacidad) {
        return new Apartamento(id, nombre, DESCRIPCION, dormitorios, capacidad, List.of(), Set.of());
    }

    /** Con las características indicadas y sin dotación. */
    public static Apartamento conCaracteristicas(IdentificacionApartamento id, String nombre, int capacidad,
                                                 Caracteristica... caracteristicas) {
        return new Apartamento(id, nombre, DESCRIPCION, 1, capacidad, List.of(), Set.of(caracteristicas));
    }
}
