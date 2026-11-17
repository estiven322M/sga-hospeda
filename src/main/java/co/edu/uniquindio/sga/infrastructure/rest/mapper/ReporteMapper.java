package co.edu.uniquindio.sga.infrastructure.rest.mapper;

import org.springframework.stereotype.Component;

import co.edu.uniquindio.sga.application.dto.response.ReporteIngresosResponse;
import co.edu.uniquindio.sga.application.dto.response.ReporteIngresosResponse.IngresoPorMedioResponse;
import co.edu.uniquindio.sga.application.dto.response.ReporteOcupacionResponse;
import co.edu.uniquindio.sga.application.dto.response.ReporteReservasPorCanalResponse;
import co.edu.uniquindio.sga.application.dto.response.ReporteReservasPorCanalResponse.ReservasDeCanalResponse;
import co.edu.uniquindio.sga.domain.valueobject.ReporteIngresos;
import co.edu.uniquindio.sga.domain.valueobject.ReporteOcupacion;
import co.edu.uniquindio.sga.domain.valueobject.ReporteReservasPorCanal;
import co.edu.uniquindio.sga.domain.valueobject.ReservasDeCanal;

/** Dominio → respuesta de los reportes (7.9). Solo traduce: el dinero viaja como entero de pesos COP. */
@Component
public class ReporteMapper implements ConversionesDominio {

    public ReporteOcupacionResponse aResponse(ReporteOcupacion reporte) {
        return new ReporteOcupacionResponse(reporte.periodo().desde(), reporte.periodo().hasta(),
                reporte.apartamentosActivos(), reporte.nochesPeriodo(), reporte.nochesDisponibles(),
                reporte.nochesVendidas(), reporte.porcentaje());
    }

    public ReporteIngresosResponse aResponse(ReporteIngresos reporte) {
        return new ReporteIngresosResponse(reporte.periodo().desde(), reporte.periodo().hasta(),
                reporte.porMedio().stream().map(fila -> new IngresoPorMedioResponse(fila.medio(), aPesos(fila.total())))
                        .toList(),
                aPesos(reporte.total()), MONEDA);
    }

    public ReporteReservasPorCanalResponse aResponse(ReporteReservasPorCanal reporte) {
        return new ReporteReservasPorCanalResponse(reporte.periodo().desde(), reporte.periodo().hasta(),
                reporte.canales().stream()
                        .map(fila -> new ReservasDeCanalResponse(fila.canal(), fila.total(), fila.canceladas(), fila.noShow()))
                        .toList(),
                reporte.total(), reporte.canceladas(), reporte.noShow());
    }
}
