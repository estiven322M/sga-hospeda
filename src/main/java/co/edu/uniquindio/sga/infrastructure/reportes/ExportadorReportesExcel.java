package co.edu.uniquindio.sga.infrastructure.reportes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import co.edu.uniquindio.sga.application.dto.response.ReporteIngresosResponse;
import co.edu.uniquindio.sga.application.dto.response.ReporteOcupacionResponse;
import co.edu.uniquindio.sga.application.dto.response.ReporteReservasPorCanalResponse;

/**
 * Exportación de los reportes a Excel (L-23) con Apache POI. No calcula nada: escribe en una hoja lo mismo que
 * responde la API (título con el periodo, fila de encabezados en negrita, las filas y la fila de totales).
 */
@Component
public class ExportadorReportesExcel {

    public static final String TIPO_CONTENIDO = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    public byte[] ocupacion(ReporteOcupacionResponse reporte) {
        double porcentaje = reporte.porcentajeOcupacion().doubleValue();
        return libro("Ocupación", "Reporte de ocupación del " + reporte.desde() + " al " + reporte.hasta(),
                List.of("Apartamentos activos", "Noches del periodo", "Noches disponibles", "Noches vendidas",
                        "Ocupación (%)"),
                List.of(List.<Object>of(reporte.apartamentosActivos(), reporte.nochesPeriodo(),
                        reporte.nochesDisponibles(), reporte.nochesVendidas(), porcentaje)),
                List.<Object>of("Total", "", reporte.nochesDisponibles(), reporte.nochesVendidas(), porcentaje));
    }

    public byte[] ingresos(ReporteIngresosResponse reporte) {
        return libro("Ingresos", "Reporte de ingresos del " + reporte.desde() + " al " + reporte.hasta(),
                List.of("Medio de pago", "Total (" + reporte.moneda() + ")"),
                reporte.porMedio().stream().map(fila -> List.<Object>of(fila.medio(), fila.total())).toList(),
                List.<Object>of("Total", reporte.total()));
    }

    public byte[] reservasPorCanal(ReporteReservasPorCanalResponse reporte) {
        return libro("Reservas por canal",
                "Reservas por canal de origen del " + reporte.desde() + " al " + reporte.hasta(),
                List.of("Canal de origen", "Reservas", "Canceladas", "No-show"),
                reporte.canales().stream()
                        .map(fila -> List.<Object>of(fila.canal().name(), fila.total(), fila.canceladas(), fila.noShow()))
                        .toList(),
                List.<Object>of("Total", reporte.total(), reporte.canceladas(), reporte.noShow()));
    }

    private static byte[] libro(String hoja, String titulo, List<String> encabezados, List<List<Object>> filas,
                                List<Object> totales) {
        try (Workbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Font negrita = libro.createFont();
            negrita.setBold(true);
            CellStyle resaltado = libro.createCellStyle();
            resaltado.setFont(negrita);

            Sheet pagina = libro.createSheet(hoja);
            escribir(pagina.createRow(0), List.of(titulo), resaltado);
            escribir(pagina.createRow(2), encabezados, resaltado);
            int numero = 3;
            for (List<Object> fila : filas) {
                escribir(pagina.createRow(numero++), fila, null);
            }
            escribir(pagina.createRow(numero), totales, resaltado);
            for (int columna = 0; columna < encabezados.size(); columna++) {
                pagina.setColumnWidth(columna, 24 * 256);
            }
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el archivo de Excel", e);
        }
    }

    private static void escribir(Row fila, List<?> valores, CellStyle estilo) {
        for (int columna = 0; columna < valores.size(); columna++) {
            Cell celda = fila.createCell(columna);
            Object valor = valores.get(columna);
            if (valor instanceof Number numero) {
                celda.setCellValue(numero.doubleValue());
            } else {
                celda.setCellValue(String.valueOf(valor));
            }
            if (estilo != null) {
                celda.setCellStyle(estilo);
            }
        }
    }
}
