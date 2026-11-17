package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.DatosReportesRepository;
import co.edu.uniquindio.sga.domain.service.ReportesService;
import co.edu.uniquindio.sga.domain.valueobject.Periodo;
import co.edu.uniquindio.sga.domain.valueobject.ReporteOcupacion;

/**
 * 7.9, L-19: ocupación del periodo. Reúne las reservas y la cantidad de apartamentos activos y delega la fórmula en
 * el servicio de dominio; aquí no se calcula nada.
 */
@Service
public class ConsultarReporteOcupacionUseCase {

    private final DatosReportesRepository datos;
    private final ApartamentoRepository apartamentos;
    private final ReportesService reportes;

    public ConsultarReporteOcupacionUseCase(DatosReportesRepository datos, ApartamentoRepository apartamentos,
                                            ReportesService reportes) {
        this.datos = datos;
        this.apartamentos = apartamentos;
        this.reportes = reportes;
    }

    @Transactional(readOnly = true)
    public ReporteOcupacion ejecutar(Periodo periodo) {
        return reportes.ocupacion(periodo, datos.listarReservas(), apartamentos.listarActivos().size());
    }
}
