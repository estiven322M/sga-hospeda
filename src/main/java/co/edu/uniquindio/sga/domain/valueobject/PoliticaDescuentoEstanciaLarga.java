package co.edu.uniquindio.sga.domain.valueobject;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/**
 * RP-02 (L-20): descuento por estancia larga. Desde {@code nochesDesde} noches se descuenta {@code porcentaje} % sobre el
 * total de la estancia, una sola vez. Un porcentaje de 0 significa "sin descuento". Los valores salen de
 * application.properties (regla 10).
 *
 * Esta política solo decide QUÉ porcentaje corresponde; el valor congelado ({@link ValorCongelado}) es quien lo
 * aplica y lo guarda, para que la reserva conserve el descuento con el que se creó.
 */
public record PoliticaDescuentoEstanciaLarga(int nochesDesde, int porcentaje) {

    public static final PoliticaDescuentoEstanciaLarga SIN_DESCUENTO = new PoliticaDescuentoEstanciaLarga(1, 0);

    public PoliticaDescuentoEstanciaLarga {
        if (porcentaje < 0 || porcentaje > 100) {
            throw new ReglaDominioException("El porcentaje de descuento por estancia larga debe estar entre 0 y 100");
        }
        if (nochesDesde < 0) {
            throw new ReglaDominioException("Las noches desde las que aplica el descuento no pueden ser negativas");
        }
    }

    /** Porcentaje que corresponde a una estancia de {@code noches} noches (0 si no alcanza el mínimo). */
    public int porcentajePara(int noches) {
        return porcentaje > 0 && noches >= Math.max(nochesDesde, 1) ? porcentaje : 0;
    }
}
