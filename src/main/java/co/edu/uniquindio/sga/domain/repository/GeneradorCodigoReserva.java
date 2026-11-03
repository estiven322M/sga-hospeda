package co.edu.uniquindio.sga.domain.repository;

import java.time.LocalDate;

import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;

/** Puerto que entrega el siguiente código único de reserva (formato RES-YYYY-NNNNN). */
public interface GeneradorCodigoReserva {

    CodigoReserva generar(LocalDate fecha);
}
