package co.edu.uniquindio.sga.ValueObject;

public enum EstadoReserva {
	
	PENDIENTE(true),
	CONFIRMADA (true),
	EN_CURSO (true),
	FINALIZADA (false),
	CANCELADA (false),
	NO_SHOW (false);
	
	private final boolean activa;
	
	EstadoReserva(boolean activa){
		this.activa=activa;
	}
	
	// Consulta estado activo para PENDIENTE, COFIRMADA y EN_CURSO
	public boolean esActiva() {
		return activa;
	}
	
	
	
	// Consulta estado terminal para FINALIZADA, CANCELADA, NO_SHOW
	public boolean esTerminal() {
		return !activa;
	}
	
	public boolean retieneDisponibilidad() {
		return activa;
	}
	
	/**
     * RN-08: define el ciclo de vida válido de una reserva.
     * El conocimiento de las transiciones vive aquí, no disperso en if.
     */
    public boolean puedeTransicionarA(EstadoReserva siguiente) {
        return switch (this) {
            case PENDIENTE  -> siguiente == CONFIRMADA || siguiente == CANCELADA;
            case CONFIRMADA -> siguiente == EN_CURSO
                            || siguiente == CANCELADA
                            || siguiente == NO_SHOW;
            case EN_CURSO   -> siguiente == FINALIZADA;
            case FINALIZADA, CANCELADA, NO_SHOW -> false;   // estados terminales
        };
    }
	

}
