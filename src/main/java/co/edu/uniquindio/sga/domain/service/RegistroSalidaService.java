package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/**
 * 7.6: "El cierre del folio es requisito para completar la salida, salvo autorización del administrador."
 * Cerrar un folio con saldo pendiente ya exige esa autorización registrada (RN-17, Folio.cerrar), de modo que la
 * regla de la salida se reduce a: el folio debe estar cerrado. La reserva no conoce su folio (otro agregado),
 * por eso la comprobación vive en un servicio de dominio.
 */
public class RegistroSalidaService {

    public void verificarQueElFolioEstaCerrado(Reserva reserva, Folio folio) {
        if (!folio.getReserva().equals(reserva.getCodigo())) {
            throw new ReglaDominioException("El folio no pertenece a la reserva cuya salida se registra");
        }
        if (!folio.estaCerrado()) {
            throw new ReglaDominioException("No se puede registrar la salida: el folio debe cerrarse primero "
                    + "(un folio con saldo pendiente exige la autorización del administrador)");
        }
    }
}
