package co.edu.uniquindio.sga.domain.repository;

import java.util.List;

import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.valueobject.Pago;

/**
 * Puerto de salida de los reportes (7.9, L-19): entrega los datos crudos y no calcula nada; las fórmulas viven en
 * {@code ReportesService}. Solo lectura.
 */
public interface DatosReportesRepository {

    /** Todas las reservas, en cualquier estado (incluidas canceladas y no-show). */
    List<Reserva> listarReservas();

    /** Todos los pagos registrados en los folios. */
    List<Pago> listarPagos();
}
