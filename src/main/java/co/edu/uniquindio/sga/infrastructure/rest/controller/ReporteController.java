package co.edu.uniquindio.sga.infrastructure.rest.controller;

import java.time.LocalDate;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uniquindio.sga.application.dto.response.ReporteIngresosResponse;
import co.edu.uniquindio.sga.application.dto.response.ReporteOcupacionResponse;
import co.edu.uniquindio.sga.application.dto.response.ReporteReservasPorCanalResponse;
import co.edu.uniquindio.sga.application.usecase.ConsultarReporteIngresosUseCase;
import co.edu.uniquindio.sga.application.usecase.ConsultarReporteOcupacionUseCase;
import co.edu.uniquindio.sga.application.usecase.ConsultarReporteReservasPorCanalUseCase;
import co.edu.uniquindio.sga.domain.valueobject.Periodo;
import co.edu.uniquindio.sga.infrastructure.reportes.ExportadorReportesExcel;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.ReporteMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;

/**
 * Reportes del alojamiento (7.9, L-19) y su exportación a Excel (L-23). Solo lectura y solo del administrador. El
 * rango de fechas es obligatorio (400 si falta) y lo valida el dominio: un rango invertido responde 409.
 */
@RestController
@RequestMapping("/api/reportes")
@Tag(name = "Reportes", description = "Ocupación, ingresos y reservas por canal de un periodo")
public class ReporteController {

    private static final String TIPOS = "ocupacion|ingresos|reservas-por-canal";

    private final ConsultarReporteOcupacionUseCase ocupacion;
    private final ConsultarReporteIngresosUseCase ingresos;
    private final ConsultarReporteReservasPorCanalUseCase reservasPorCanal;
    private final ReporteMapper mapper;
    private final ExportadorReportesExcel excel;

    public ReporteController(ConsultarReporteOcupacionUseCase ocupacion, ConsultarReporteIngresosUseCase ingresos,
                             ConsultarReporteReservasPorCanalUseCase reservasPorCanal, ReporteMapper mapper,
                             ExportadorReportesExcel excel) {
        this.ocupacion = ocupacion;
        this.ingresos = ingresos;
        this.reservasPorCanal = reservasPorCanal;
        this.mapper = mapper;
        this.excel = excel;
    }

    @GetMapping("/ocupacion")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Reporte de ocupación: noches vendidas y porcentaje del periodo")
    public ResponseEntity<ReporteOcupacionResponse> ocupacion(@RequestParam LocalDate desde,
                                                              @RequestParam LocalDate hasta) {
        return ResponseEntity.ok(mapper.aResponse(ocupacion.ejecutar(new Periodo(desde, hasta))));
    }

    @GetMapping("/ingresos")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Reporte de ingresos: pagos registrados en el periodo por medio de pago")
    public ResponseEntity<ReporteIngresosResponse> ingresos(@RequestParam LocalDate desde,
                                                            @RequestParam LocalDate hasta) {
        return ResponseEntity.ok(mapper.aResponse(ingresos.ejecutar(new Periodo(desde, hasta))));
    }

    @GetMapping("/reservas-por-canal")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Reporte de reservas por canal de origen, incluidas canceladas y no-show")
    public ResponseEntity<ReporteReservasPorCanalResponse> reservasPorCanal(@RequestParam LocalDate desde,
                                                                           @RequestParam LocalDate hasta) {
        return ResponseEntity.ok(mapper.aResponse(reservasPorCanal.ejecutar(new Periodo(desde, hasta))));
    }

    /** L-23: el mismo reporte, como archivo de Excel con encabezados y totales. */
    @GetMapping("/{tipo}/exportar")
    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @Operation(summary = "Exportar un reporte a Excel (xlsx)")
    public ResponseEntity<byte[]> exportar(
            @PathVariable @Pattern(regexp = TIPOS,
                    message = "El tipo de reporte debe ser ocupacion, ingresos o reservas-por-canal") String tipo,
            @RequestParam LocalDate desde, @RequestParam LocalDate hasta,
            @RequestParam(defaultValue = "xlsx") @Pattern(regexp = "xlsx",
                    message = "El único formato de exportación es xlsx") String formato) {
        Periodo periodo = new Periodo(desde, hasta);
        byte[] archivo = switch (tipo) {
            case "ocupacion" -> excel.ocupacion(mapper.aResponse(ocupacion.ejecutar(periodo)));
            case "ingresos" -> excel.ingresos(mapper.aResponse(ingresos.ejecutar(periodo)));
            default -> excel.reservasPorCanal(mapper.aResponse(reservasPorCanal.ejecutar(periodo)));
        };
        String nombre = "reporte-" + tipo + "-" + desde + "-a-" + hasta + "." + formato;
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ExportadorReportesExcel.TIPO_CONTENIDO))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nombre).build().toString())
                .body(archivo);
    }
}
