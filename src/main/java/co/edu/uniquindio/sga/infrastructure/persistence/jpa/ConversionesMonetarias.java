package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.valueobject.Dinero;

/** El dinero se guarda como entero de pesos COP (regla 14), nunca como decimal. */
public final class ConversionesMonetarias {

    private ConversionesMonetarias() {
    }

    public static long aPesos(Dinero dinero) {
        return dinero.valor().longValueExact();
    }

    public static Dinero aDinero(long pesos) {
        return Dinero.de(pesos);
    }
}
