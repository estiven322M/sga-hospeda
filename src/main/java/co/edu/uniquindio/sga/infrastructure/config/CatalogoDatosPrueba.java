package co.edu.uniquindio.sga.infrastructure.config;

import co.edu.uniquindio.sga.domain.valueobject.TramoRetencion;

import java.time.MonthDay;
import java.util.List;

/**
 * Datos de prueba que se cargan al iniciar. Son un conjunto razonable mientras no se tenga la Ficha real:
 * para ajustar apartamentos, temporadas, tarifas o la política basta editar esta clase.
 */
final class CatalogoDatosPrueba {

    /** Tarifa por ocupante y noche en la temporada base; las demás temporadas la recargan en porcentaje. */
    record ApartamentoSemilla(String identificacion, String nombre, int dormitorios, int capacidad,
                              long tarifaBase, boolean fueraDeServicio) {
    }

    /** Rango que se repite cada año. Un fin anterior al inicio cruza el cambio de año (15 dic - 15 ene). */
    record PeriodoAnual(MonthDay desde, MonthDay hasta) {
        boolean cruzaDeAnio() {
            return hasta.isBefore(desde);
        }
    }

    /** Sin periodos es la temporada base, que cubre todo lo que las demás no reclaman. */
    record TemporadaSemilla(String nombre, int porcentajeSobreTarifaBase, List<PeriodoAnual> periodos) {
        boolean esBase() {
            return periodos.isEmpty();
        }
    }

    static final List<ApartamentoSemilla> APARTAMENTOS = List.of(
            new ApartamentoSemilla("APT-101", "Apartamento 101 - Vista al parque", 2, 4, 120_000, false),
            new ApartamentoSemilla("APT-102", "Apartamento 102 - Terraza", 3, 6, 110_000, false),
            new ApartamentoSemilla("APT-201", "Apartamento 201", 1, 3, 95_000, false),
            new ApartamentoSemilla("APT-202", "Apartamento 202 - Familiar", 3, 8, 100_000, false),
            new ApartamentoSemilla("APT-203", "Apartamento 203", 2, 4, 105_000, false),
            new ApartamentoSemilla("APT-301", "Apartamento 301 - Penthouse", 4, 10, 140_000, false),
            new ApartamentoSemilla("APT-302", "Apartamento 302", 1, 2, 90_000, true));

    /**
     * BASE < MEDIA < ALTA. Las fechas son fijas y aproximadas (Semana Santa y los puentes festivos se mueven
     * cada año); ninguna temporada se solapa con otra.
     */
    static final List<TemporadaSemilla> TEMPORADAS = List.of(
            // Resto del año: demanda normal.
            new TemporadaSemilla("BASE", 100, List.of()),
            // Puentes festivos: Semana Santa, 1 de mayo, agosto, octubre y los dos puentes de noviembre.
            new TemporadaSemilla("MEDIA", 125, List.of(
                    new PeriodoAnual(MonthDay.of(3, 28), MonthDay.of(4, 6)),
                    new PeriodoAnual(MonthDay.of(4, 30), MonthDay.of(5, 4)),
                    new PeriodoAnual(MonthDay.of(8, 7), MonthDay.of(8, 10)),
                    new PeriodoAnual(MonthDay.of(10, 12), MonthDay.of(10, 15)),
                    new PeriodoAnual(MonthDay.of(11, 1), MonthDay.of(11, 4)),
                    new PeriodoAnual(MonthDay.of(11, 11), MonthDay.of(11, 14)))),
            // Vacaciones escolares: mitad de año y fin de año (Navidad, Año Nuevo y Reyes).
            new TemporadaSemilla("ALTA", 160, List.of(
                    new PeriodoAnual(MonthDay.of(6, 15), MonthDay.of(7, 15)),
                    new PeriodoAnual(MonthDay.of(12, 15), MonthDay.of(1, 15)))));

    /** Los periodos de las temporadas se generan para este rango de años alrededor de la fecha de carga. */
    static final int ANIOS_ANTERIORES_CUBIERTOS = 1;
    static final int ANIOS_POSTERIORES_CUBIERTOS = 2;

    /** Antelación mínima en horas y porcentaje retenido; el último tramo (antelación 0) cubre lo tardío. */
    static final List<TramoRetencion> TRAMOS_POLITICA = List.of(
            new TramoRetencion(72, 0), new TramoRetencion(24, 50), new TramoRetencion(0, 100));
    static final int DIAS_DE_VIGENCIA_PREVIA_POLITICA = 365;

    static final String APARTAMENTO_CON_BLOQUEO = "APT-301";
    static final int BLOQUEO_INICIA_EN_DIAS = 30;
    static final int BLOQUEO_NOCHES = 4;
    static final String BLOQUEO_MOTIVO = "Mantenimiento de tuberías";

    private CatalogoDatosPrueba() {
    }
}
