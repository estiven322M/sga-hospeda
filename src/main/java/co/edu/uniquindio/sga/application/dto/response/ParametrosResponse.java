package co.edu.uniquindio.sga.application.dto.response;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Los parámetros de la sección 6 del proyecto, en solo lectura: son configuración del sistema (application.properties o
 * variables de entorno), no datos editables. Cambiarlos exige cambiar la configuración y reiniciar.
 */
public record ParametrosResponse(
        int umbralEdadFacturableAnios,
        int tiempoPreparacionHoras,
        int plazoConfirmacionHoras,
        LocalTime horaLimiteNoShow,
        int anticipoPorcentaje,
        Map<String, Integer> estanciaMinimaPorTemporada,
        int nochesDescuentoEstanciaLarga,
        int porcentajeDescuentoEstanciaLarga,
        int edadAdultoResponsableAnios,
        List<String> mediosPago,
        int calendarioMaximoNoches,
        String zonaHoraria,
        String nota) {
}
