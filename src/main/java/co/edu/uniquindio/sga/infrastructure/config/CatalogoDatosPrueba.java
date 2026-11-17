package co.edu.uniquindio.sga.infrastructure.config;

import co.edu.uniquindio.sga.domain.valueobject.Caracteristica;
import co.edu.uniquindio.sga.domain.valueobject.TramoRetencion;

import java.time.MonthDay;
import java.util.List;
import java.util.Set;

/**
 * Datos de prueba que se cargan al iniciar. Son un conjunto razonable mientras no se tenga la Ficha real:
 * para ajustar apartamentos, temporadas, tarifas o la política basta editar esta clase.
 */
final class CatalogoDatosPrueba {

    /** Datos de un apartamento sembrado: tarifa por ocupante y noche en la temporada base (las demás temporadas la
     * recargan en porcentaje) y los datos descriptivos de L-06. */
    record ApartamentoSemilla(String identificacion, String nombre, String descripcion, int dormitorios,
                              int capacidad, List<String> dotacion, Set<Caracteristica> caracteristicas,
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

    /** Un servicio adicional sembrado (L-17). Sin cargo (cortesía) lleva valor 0. */
    record ServicioSemilla(String codigo, String nombre, String descripcion, boolean generaCargo, long valor) {
    }

    /** L-17: al menos un servicio; aquí cuatro, uno de ellos sin cargo para poder demostrar esa regla. */
    static final List<ServicioSemilla> SERVICIOS_ADICIONALES = List.of(
            new ServicioSemilla("DESAYUNO", "Desayuno típico", "Desayuno cafetero servido en el apartamento, por persona.", true, 25_000),
            new ServicioSemilla("LAVANDERIA", "Lavandería", "Lavado y secado de una carga de ropa.", true, 18_000),
            new ServicioSemilla("TRASLADO", "Traslado a Armenia", "Traslado en vehículo particular hasta el aeropuerto o la terminal de Armenia.", true, 90_000),
            new ServicioSemilla("BIENVENIDA", "Detalle de bienvenida", "Cortesía del alojamiento: café de la región y una nota de bienvenida.", false, 0));

    /** Cada apartamento se diferencia de los demás por al menos una característica (L-06). */
    static final List<ApartamentoSemilla> APARTAMENTOS = List.of(
            new ApartamentoSemilla("APT-101", "Apartamento 101 - Vista al parque",
                    "Apartamento luminoso en el primer piso con vista directa al parque y balcón para el desayuno.",
                    2, 4, List.of("Cocina equipada", "Nevera", "Smart TV", "Wi-Fi", "Lavadora"),
                    Set.of(Caracteristica.VISTA_AL_PARQUE, Caracteristica.BALCON), 120_000, false),
            new ApartamentoSemilla("APT-102", "Apartamento 102 - Terraza",
                    "Apartamento con terraza privada amoblada y parqueadero cubierto incluido.",
                    3, 6, List.of("Cocina equipada", "Nevera", "Smart TV", "Wi-Fi", "Lavadora", "Asador"),
                    Set.of(Caracteristica.TERRAZA, Caracteristica.PARQUEADERO), 110_000, false),
            new ApartamentoSemilla("APT-201", "Apartamento 201",
                    "Apartamento práctico en el segundo piso con balcón hacia la calle interior.",
                    1, 3, List.of("Kitchenette", "Nevera", "Smart TV", "Wi-Fi"),
                    Set.of(Caracteristica.BALCON), 95_000, false),
            new ApartamentoSemilla("APT-202", "Apartamento 202 - Familiar",
                    "Apartamento amplio pensado para familias, con acceso sin escalones y parqueadero.",
                    3, 8, List.of("Cocina equipada", "Nevera", "Smart TV", "Wi-Fi", "Lavadora", "Cuna disponible"),
                    Set.of(Caracteristica.PARQUEADERO, Caracteristica.ACCESIBLE), 100_000, false),
            new ApartamentoSemilla("APT-203", "Apartamento 203",
                    "Apartamento accesible con baño adaptado y entrada sin escalones.",
                    2, 4, List.of("Cocina equipada", "Nevera", "Smart TV", "Wi-Fi", "Baño adaptado"),
                    Set.of(Caracteristica.ACCESIBLE), 105_000, false),
            new ApartamentoSemilla("APT-301", "Apartamento 301 - Penthouse",
                    "Penthouse en el último piso con terraza, jacuzzi y vista panorámica de la ciudad.",
                    4, 10, List.of("Cocina equipada", "Nevera", "Smart TV", "Wi-Fi", "Lavadora", "Aire acondicionado"),
                    Set.of(Caracteristica.VISTA_PANORAMICA, Caracteristica.JACUZZI, Caracteristica.TERRAZA),
                    140_000, false),
            new ApartamentoSemilla("APT-302", "Apartamento 302",
                    "Apartamento compacto para parejas, con balcón pequeño.",
                    1, 2, List.of("Kitchenette", "Nevera", "Smart TV", "Wi-Fi"),
                    Set.of(Caracteristica.BALCON), 90_000, true));

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

    /** Novedad de ejemplo (7.6): se registra a las 9:00 de hace {@code haceDias} días. */
    record NovedadSemilla(String apartamento, int haceDias, String autor, String descripcion,
                          co.edu.uniquindio.sga.domain.valueobject.GravedadNovedad gravedad) {
    }

    static final int HORA_NOVEDADES = 9;
    static final List<NovedadSemilla> NOVEDADES = List.of(
            new NovedadSemilla("APT-302", 3, "Personal de Servicio",
                    "Fuga de agua bajo el lavaplatos de la kitchenette; se cerró la llave de paso.",
                    co.edu.uniquindio.sga.domain.valueobject.GravedadNovedad.CRITICA),
            new NovedadSemilla("APT-302", 2, "Administrador SGA",
                    "El plomero revisó la fuga: hay que cambiar el sifón. Queda fuera de servicio hasta la reparación.",
                    co.edu.uniquindio.sga.domain.valueobject.GravedadNovedad.ALTA),
            new NovedadSemilla("APT-101", 1, "Personal de Servicio",
                    "Falta una de las dos llaves del balcón.",
                    co.edu.uniquindio.sga.domain.valueobject.GravedadNovedad.BAJA));

    private CatalogoDatosPrueba() {
    }
}
