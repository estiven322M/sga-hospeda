package co.edu.uniquindio.sga.domain.entity;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.EstadoOperativo;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

/**
 * Raíz de Agregado que representa una unidad autónoma que se alquila.
 * Protege sus propias reglas de capacidad y estado físico.
 */

public class Apartamento {
	
private final IdentificacionApartamento identificacion; // Identidad única
    
    private String nombre;
    private int dormitorios;
    private int capacidad;
    private EstadoOperativo estadoOperativo;
    private boolean activo;

    public Apartamento(IdentificacionApartamento identificacion, String nombre, 
                       int dormitorios, int capacidad) {
        if (identificacion == null) {
            throw new ReglaDominioException("El apartamento debe tener una identificación");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDominioException("El apartamento debe tener un nombre");
        }
        if (dormitorios < 1) {
            throw new ReglaDominioException("El apartamento debe tener al menos un dormitorio");
        }
        if (capacidad < 1) {
            throw new ReglaDominioException("La capacidad debe ser al menos de 1 persona");
        }

        this.identificacion = identificacion;
        this.nombre = nombre;
        this.dormitorios = dormitorios;
        this.capacidad = capacidad;
        this.estadoOperativo = EstadoOperativo.PREPARADO; // Estado inicial por defecto
        this.activo = true;
    }

    /** RN-02: Verifica si el apartamento puede alojar a un grupo específico. */
    public boolean admite(int totalOcupantes) {
        return totalOcupantes > 0 && totalOcupantes <= this.capacidad;
    }

    /** Registro del grupo: PREPARADO → OCUPADO. */
    public void marcarOcupado() {
        verificarTransicionOperativa(EstadoOperativo.OCUPADO);
        this.estadoOperativo = EstadoOperativo.OCUPADO;
    }

    /** Salida del grupo: OCUPADO → PENDIENTE_PREPARACION. */
    public void liberar() {
        verificarTransicionOperativa(EstadoOperativo.PENDIENTE_PREPARACION);
        this.estadoOperativo = EstadoOperativo.PENDIENTE_PREPARACION;
    }

    /** Personal de servicio: PENDIENTE_PREPARACION → EN_PREPARACION. */
    public void iniciarPreparacion() {
        verificarTransicionOperativa(EstadoOperativo.EN_PREPARACION);
        this.estadoOperativo = EstadoOperativo.EN_PREPARACION;
    }

    /** Personal de servicio: EN_PREPARACION → PREPARADO. */
    public void marcarPreparado() {
        verificarTransicionOperativa(EstadoOperativo.PREPARADO);
        this.estadoOperativo = EstadoOperativo.PREPARADO;
    }

    /** Decisión administrativa. */
    public void declararFueraDeServicio() {
        verificarTransicionOperativa(EstadoOperativo.FUERA_DE_SERVICIO);
        this.estadoOperativo = EstadoOperativo.FUERA_DE_SERVICIO;
    }

    /** 
     * 7.3: cambiar la capacidad NO afecta las reservas ya creadas. 
     */
    public void cambiarCapacidad(int nuevaCapacidad) {
        if (nuevaCapacidad < 1) {
            throw new ReglaDominioException("La capacidad debe ser al menos 1");
        }
        this.capacidad = nuevaCapacidad;
    }

    private void verificarTransicionOperativa(EstadoOperativo siguiente) {
        if (!this.estadoOperativo.puedeTransicionarA(siguiente)) {
            throw new ReglaDominioException(
                "No se puede pasar de " + this.estadoOperativo + " a " + siguiente);
        }
    }

    // Getters
    public IdentificacionApartamento getIdentificacion() { return identificacion; }
    public String getNombre() { return nombre; }
    public int getDormitorios() { return dormitorios; }
    public int getCapacidad() { return capacidad; }
    public EstadoOperativo getEstadoOperativo() { return estadoOperativo; }
    public boolean isActivo() { return activo; }

    // Igualdad basada EXCLUSIVAMENTE en la identidad
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Apartamento otro)) return false;
        return this.identificacion.equals(otro.identificacion);
    }

    @Override
    public int hashCode() {
        return identificacion.hashCode();
    }

}
