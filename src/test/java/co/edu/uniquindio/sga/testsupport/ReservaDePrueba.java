package co.edu.uniquindio.sga.testsupport;

import co.edu.uniquindio.sga.domain.valueobject.EdadAdultoResponsable;

/**
 * Valores neutros para las pruebas que crean o modifican reservas. Desde RP-03 (L-20) {@code Reserva.crear} y
 * {@code modificarEstancia} reciben la edad del adulto responsable; casi ninguna prueba se ocupa de ella, así que usan
 * la de la Ficha (18 años) desde aquí y las pruebas de RP-03 declaran la suya explícitamente.
 */
public final class ReservaDePrueba {

    /** Los 18 años de application.properties. */
    public static final EdadAdultoResponsable EDAD_ADULTO = new EdadAdultoResponsable(18);

    private ReservaDePrueba() {
    }
}
