package co.edu.uniquindio.sga.ValueObject;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

public record Estancia(LocalDate fechaEntrada, LocalDate fechaSalida) {
	
	public Estancia {
        if (fechaEntrada == null || fechaSalida == null) {
            throw new ReglaDominioException("La estancia requiere fecha de entrada y de salida");
        }
        // Regla negocio 03: toda estancia tiene al menos una noche (la salida debe ser posterior a la entrada)
        if (!fechaSalida.isAfter(fechaEntrada)) {
            throw new ReglaDominioException(
                "La fecha de salida debe ser posterior a la fecha de entrada");
        }
    }
	
	/** 
     * Calcula el número de noches de la estancia.
     * Ejemplo: Del 10 al 12 son dos noches (la del 10 y la del 11). 
     */
    public int noches() {
        return (int) ChronoUnit.DAYS.between(fechaEntrada, fechaSalida);
    }

    /** 
     * Verifica si una fecha específica está incluida en la estancia.
     * La noche de la fecha de salida no se ocupa ni se cobra. 
     */
    public boolean incluye(LocalDate noche) {
        return !noche.isBefore(fechaEntrada) && noche.isBefore(fechaSalida);
    }

    /** 
     * Regla  negocio 01: Dos estancias se solapan si comparten al menos una noche. 
     * Es decir, si la entrada de una es anterior a la salida de la otra y viceversa.
     */
    public boolean seSolapaCon(Estancia otra) {
        return this.fechaEntrada.isBefore(otra.fechaSalida)
            && otra.fechaEntrada.isBefore(this.fechaSalida);
    }

    /** 
     * Retorna la lista de fechas efectivamente ocupadas, 
     * útil para liquidar tarifas noche por noche (Regla negocio 05). 
     */
    public List<LocalDate> nochesOcupadas() {
        return fechaEntrada.datesUntil(fechaSalida).toList();
    }

}
