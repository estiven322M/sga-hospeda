package co.edu.uniquindio.sga.ValueObject;

import java.util.List;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

public record ValorCongelado(List<CargoNoche> detalle) {
	
	public ValorCongelado {
        if (detalle == null || detalle.isEmpty()) {
            throw new ReglaDominioException("El valor de la reserva debe tener desglose por noche");
        }
        detalle = List.copyOf(detalle); // Copia inmutable
    }

    public Dinero total() {
        return detalle.stream()
                .map(CargoNoche::subtotal)
                .reduce(Dinero.CERO, Dinero::mas);
    }

    public int noches() {
        return detalle.size();
    }
	
	

}
