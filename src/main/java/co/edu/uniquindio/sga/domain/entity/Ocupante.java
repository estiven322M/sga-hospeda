package co.edu.uniquindio.sga.domain.entity;

import java.time.LocalDate;
import java.time.Period;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.ValueObject.DocumentoIdentidad;
import co.edu.uniquindio.sga.ValueObject.Estancia;
import co.edu.uniquindio.sga.ValueObject.UmbralEdadFacturable;

/**
 * Entidad local que representa a una persona que se aloja.
 * La identidad la define el documento (dos personas con el mismo documento son la misma).
 */

public class Ocupante {
	
	private final DocumentoIdentidad documento;      // Identidad: no cambia nunca
    private final LocalDate fechaNacimiento;         // Inmutable (3.2)

    private String nombre;                           // Estado: puede cambiar

    public Ocupante(DocumentoIdentidad documento, String nombre,
                    LocalDate fechaNacimiento, LocalDate fechaActual) {
    	
        if (documento == null) {
            throw new ReglaDominioException("El ocupante debe tener documento de identidad");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDominioException("El ocupante debe tener nombre");
        }
        if (fechaNacimiento == null) {
            throw new ReglaDominioException("El ocupante debe tener fecha de nacimiento");
        }
        if (fechaNacimiento.isAfter(fechaActual)) {
            throw new ReglaDominioException("La fecha de nacimiento no puede ser futura");
        }
        
        this.documento = documento;
        this.nombre = nombre;
        this.fechaNacimiento = fechaNacimiento;
    }

    /** 
     * La edad se calcula respecto a una fecha, nunca se almacena (3.2). 
     */
    public int edadA(LocalDate fecha) {
        return Period.between(fechaNacimiento, fecha).getYears();
    }

    /**
     * RN-06: Es facturable si a la FECHA DE ENTRADA alcanza el umbral.
     * Quien cumple años durante la estancia no cambia de condición a mitad de camino.
     */
    public boolean esFacturableEn(Estancia estancia, UmbralEdadFacturable umbral) {
        return edadA(estancia.fechaEntrada()) >= umbral.anios();
    }

    /**
     * Comportamiento para cambiar el nombre (no es un simple setter anémico).
     */
    public void actualizarNombre(String nuevoNombre) {
        if (nuevoNombre == null || nuevoNombre.isBlank()) {
            throw new ReglaDominioException("El ocupante debe tener nombre");
        }
        this.nombre = nuevoNombre;
    }

    public DocumentoIdentidad getDocumento() {
        return documento;
    }

    public String getNombre() {
        return nombre;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    // Igualdad basada EXCLUSIVAMENTE en la identidad.

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Ocupante otro)) {
            return false;
        }
        return this.documento.equals(otro.documento);
    }

    @Override
    public int hashCode() {
        return documento.hashCode();
    }

}
