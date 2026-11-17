package co.edu.uniquindio.sga.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.domain.valueobject.IngresoPorMedio;
import co.edu.uniquindio.sga.domain.valueobject.Pago;
import co.edu.uniquindio.sga.domain.valueobject.Periodo;
import co.edu.uniquindio.sga.domain.valueobject.ReporteIngresos;
import co.edu.uniquindio.sga.domain.valueobject.ReporteOcupacion;
import co.edu.uniquindio.sga.domain.valueobject.ReporteReservasPorCanal;
import co.edu.uniquindio.sga.domain.valueobject.ReservasDeCanal;

/**
 * Fórmulas de los reportes (7.9, L-19). Es un servicio de dominio porque cada cálculo cruza varios agregados
 * (reservas, folios y apartamentos) y ninguno puede evaluarlo solo. No consulta nada: recibe los datos y calcula.
 * El periodo es un rango de fechas inclusivo en ambos extremos; un rango invertido lo rechaza {@link Periodo}.
 * <ul>
 *   <li><b>Ocupación:</b> noches vendidas / (apartamentos activos × noches del periodo). Una noche está vendida si
 *       pertenece a una reserva CONFIRMADA, EN_CURSO o FINALIZADA; las PENDIENTES aún no están vendidas y las
 *       CANCELADAS y NO_SHOW no ocuparon. El porcentaje se redondea a dos decimales (mitad hacia arriba).</li>
 *   <li><b>Ingresos:</b> suma de los pagos cuya fecha cae en el periodo, por medio de pago. Los cargos no cuentan.</li>
 *   <li><b>Reservas por canal:</b> reservas creadas en el periodo, por canal de origen, en cualquier estado.</li>
 * </ul>
 */
public class ReportesService {

    private static final Set<EstadoReserva> ESTADOS_QUE_VENDEN =
            EnumSet.of(EstadoReserva.CONFIRMADA, EstadoReserva.EN_CURSO, EstadoReserva.FINALIZADA);
    private static final int DECIMALES_PORCENTAJE = 2;

    public ReporteOcupacion ocupacion(Periodo periodo, List<Reserva> reservas, int apartamentosActivos) {
        if (apartamentosActivos < 0) {
            throw new ReglaDominioException("La cantidad de apartamentos activos no puede ser negativa");
        }
        int nochesPeriodo = (int) ChronoUnit.DAYS.between(periodo.desde(), periodo.hasta()) + 1;
        int vendidas = 0;
        for (Reserva reserva : reservas) {
            if (ESTADOS_QUE_VENDEN.contains(reserva.getEstado())) {
                vendidas += nochesDentroDelPeriodo(reserva, periodo);
            }
        }
        int disponibles = apartamentosActivos * nochesPeriodo;
        BigDecimal porcentaje = disponibles == 0
                ? BigDecimal.ZERO.setScale(DECIMALES_PORCENTAJE)
                : BigDecimal.valueOf(vendidas).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(disponibles), DECIMALES_PORCENTAJE, RoundingMode.HALF_UP);
        return new ReporteOcupacion(periodo, apartamentosActivos, nochesPeriodo, vendidas, porcentaje);
    }

    public ReporteIngresos ingresos(Periodo periodo, List<Pago> pagos) {
        Map<String, Dinero> porMedio = new TreeMap<>();
        Dinero total = Dinero.CERO;
        for (Pago pago : pagos) {
            if (periodo.incluye(pago.fecha())) {
                porMedio.merge(pago.medio().trim().toUpperCase(Locale.ROOT), pago.valor(), Dinero::mas);
                total = total.mas(pago.valor());
            }
        }
        List<IngresoPorMedio> filas = porMedio.entrySet().stream()
                .map(fila -> new IngresoPorMedio(fila.getKey(), fila.getValue())).toList();
        return new ReporteIngresos(periodo, filas, total);
    }

    public ReporteReservasPorCanal reservasPorCanal(Periodo periodo, List<Reserva> reservas) {
        List<ReservasDeCanal> canales = new ArrayList<>();
        int total = 0;
        for (CanalOrigen canal : CanalOrigen.values()) {
            int delCanal = 0;
            int canceladas = 0;
            int noShow = 0;
            for (Reserva reserva : reservas) {
                if (reserva.getCanalOrigen() != canal || !periodo.incluye(reserva.getCreadaEn().toLocalDate())) {
                    continue;
                }
                delCanal++;
                if (reserva.getEstado() == EstadoReserva.CANCELADA) {
                    canceladas++;
                } else if (reserva.getEstado() == EstadoReserva.NO_SHOW) {
                    noShow++;
                }
            }
            canales.add(new ReservasDeCanal(canal, delCanal, canceladas, noShow));
            total += delCanal;
        }
        return new ReporteReservasPorCanal(periodo, canales, total);
    }

    /** Noches de la estancia [entrada, salida) que caen dentro del periodo [desde, hasta]. */
    private static int nochesDentroDelPeriodo(Reserva reserva, Periodo periodo) {
        LocalDate primera = max(reserva.getEstancia().fechaEntrada(), periodo.desde());
        LocalDate finExclusivo = min(reserva.getEstancia().fechaSalida(), periodo.hasta().plusDays(1));
        return (int) Math.max(0, ChronoUnit.DAYS.between(primera, finExclusivo));
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate min(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }
}
