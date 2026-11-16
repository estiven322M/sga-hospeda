package co.edu.uniquindio.sga.application.dto.response;

import java.time.LocalDate;

import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;

/** La reserva tal como la ve un canal externo (10.3.1): su código en el SGA, su identificador propio, su estado y lo que cuesta. */
public record ReservaExternaResponse(
        String codigoReserva,
        String identificadorExterno,
        String identificacionApartamento,
        LocalDate fechaEntrada,
        LocalDate fechaSalida,
        EstadoReserva estado,
        long valorTotal,
        String moneda) {
}
