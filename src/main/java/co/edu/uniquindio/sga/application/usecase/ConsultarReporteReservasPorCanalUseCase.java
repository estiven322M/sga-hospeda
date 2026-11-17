package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.domain.repository.DatosReportesRepository;
import co.edu.uniquindio.sga.domain.service.ReportesService;
import co.edu.uniquindio.sga.domain.valueobject.Periodo;
import co.edu.uniquindio.sga.domain.valueobject.ReporteReservasPorCanal;

/**
 * 7.9, L-19: reservas por canal de origen del periodo, incluidas canceladas y no-show. Reúne las reservas y delega el
 * conteo en el servicio de dominio.
 */
@Service
public class ConsultarReporteReservasPorCanalUseCase {

    private final DatosReportesRepository datos;
    private final ReportesService reportes;

    public ConsultarReporteReservasPorCanalUseCase(DatosReportesRepository datos, ReportesService reportes) {
        this.datos = datos;
        this.reportes = reportes;
    }

    @Transactional(readOnly = true)
    public ReporteReservasPorCanal ejecutar(Periodo periodo) {
        return reportes.reservasPorCanal(periodo, datos.listarReservas());
    }
}
