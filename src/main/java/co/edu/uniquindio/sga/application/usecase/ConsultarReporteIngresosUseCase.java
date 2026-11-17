package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.domain.repository.DatosReportesRepository;
import co.edu.uniquindio.sga.domain.service.ReportesService;
import co.edu.uniquindio.sga.domain.valueobject.Periodo;
import co.edu.uniquindio.sga.domain.valueobject.ReporteIngresos;

/**
 * 7.9, L-19: ingresos del periodo por medio de pago. Reúne los pagos de los folios y delega la fórmula en el servicio
 * de dominio; aquí no se calcula nada.
 */
@Service
public class ConsultarReporteIngresosUseCase {

    private final DatosReportesRepository datos;
    private final ReportesService reportes;

    public ConsultarReporteIngresosUseCase(DatosReportesRepository datos, ReportesService reportes) {
        this.datos = datos;
        this.reportes = reportes;
    }

    @Transactional(readOnly = true)
    public ReporteIngresos ejecutar(Periodo periodo) {
        return reportes.ingresos(periodo, datos.listarPagos());
    }
}
