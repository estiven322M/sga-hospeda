package co.edu.uniquindio.sga.ValueObject;

import java.math.BigDecimal;
import java.math.RoundingMode;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/**
 *  Definición Operativa 3.4
 * Se admiten valores negativos: los ajustes por modificación y los saldos
 * a favor del huésped lo son.
 */

public record Dinero(BigDecimal valor) {
	
	public static final Dinero CERO= new Dinero(BigDecimal.ZERO);
	
	public Dinero{
		if(valor==null) {
			throw new ReglaDominioException("El valor monetario es obligatorio");
		}
		
		// Aplica redondeo al peso más cercano 
		valor=valor.setScale(0,RoundingMode.HALF_UP);
	}
	
	/**
     * Crea una instancia de Dinero a partir de un valor long.
     */
    public static Dinero de(long pesos) {
        return new Dinero(BigDecimal.valueOf(pesos));
    }

    /**
     * Suma dos valores de dinero devolviendo una nueva instancia (Inmutabilidad).
     */
    public Dinero mas(Dinero otro) {
        return new Dinero(this.valor.add(otro.valor));
    }

    /**
     * Resta dos valores de dinero devolviendo una nueva instancia.
     */
    public Dinero menos(Dinero otro) {
        return new Dinero(this.valor.subtract(otro.valor));
    }

    /**
     * Multiplica el valor por una cantidad (calcular tarifas por ocupantes).
     */
    public Dinero por(int cantidad) {
        return new Dinero(this.valor.multiply(BigDecimal.valueOf(cantidad)));
    }

    public boolean esCero() {
        return this.valor.signum() == 0;
    }

    public boolean esNegativo() {
        return this.valor.signum() < 0;
    }

}
