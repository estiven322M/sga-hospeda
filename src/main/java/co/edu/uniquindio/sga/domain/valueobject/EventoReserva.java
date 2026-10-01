package co.edu.uniquindio.sga.domain.valueobject;

import java.time.LocalDateTime;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

public record EventoReserva(
		LocalDateTime momento,
        String accion,
        String autor,
        EstadoReserva estadoAnterior,
        EstadoReserva estadoNuevo,
        String observacion) {
	
	public EventoReserva {
        if (momento == null) {
            throw new ReglaDominioException("El evento debe tener fecha y hora");
        }
        if (accion == null || accion.isBlank()) {
            throw new ReglaDominioException("El evento debe indicar la acción realizada");
        }
        if (autor == null || autor.isBlank()) {
            throw new ReglaDominioException("El evento debe indicar quién ejecutó la acción");
        }
    }

}
