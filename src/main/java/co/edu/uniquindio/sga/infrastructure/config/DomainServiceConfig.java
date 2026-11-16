package co.edu.uniquindio.sga.infrastructure.config;

import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.BloqueoRepository;
import co.edu.uniquindio.sga.domain.repository.ConfiguracionAlojamiento;
import co.edu.uniquindio.sga.domain.repository.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.repository.TarifarioRepository;
import co.edu.uniquindio.sga.domain.repository.TemporadaRepository;
import co.edu.uniquindio.sga.domain.service.ActivacionApartamentoService;
import co.edu.uniquindio.sga.domain.service.ConfirmacionReservaService;
import co.edu.uniquindio.sga.domain.service.CotizadorEstancia;
import co.edu.uniquindio.sga.domain.service.RecepcionReservaExternaService;
import co.edu.uniquindio.sga.domain.service.DisponibilidadApartamentoService;
import co.edu.uniquindio.sga.domain.service.RegistroApartamentoService;
import co.edu.uniquindio.sga.domain.service.RegistroPoliticaCancelacionService;
import co.edu.uniquindio.sga.domain.service.RegistroSalidaService;
import co.edu.uniquindio.sga.domain.service.RegistroTarifaService;
import co.edu.uniquindio.sga.domain.service.RegistroBloqueoService;
import co.edu.uniquindio.sga.domain.service.RetencionCancelacionService;
import co.edu.uniquindio.sga.domain.valueobject.EdadAdultoResponsable;
import co.edu.uniquindio.sga.domain.valueobject.EstanciaMinimaPorTemporada;
import co.edu.uniquindio.sga.domain.valueobject.PlazoConfirmacion;
import co.edu.uniquindio.sga.domain.valueobject.PoliticaDescuentoEstanciaLarga;
import co.edu.uniquindio.sga.domain.valueobject.TiempoPreparacion;
import co.edu.uniquindio.sga.domain.valueobject.UmbralEdadFacturable;

/**
 * Registra como beans los servicios de dominio y los parámetros de la Ficha (regla 9).
 * El dominio es Java puro: es esta clase, en infraestructura, la que lo conecta con Spring.
 * Todos los valores salen de application.properties (regla 10), nunca del código.
 */
@Configuration
public class DomainServiceConfig {

    /** Zona horaria del alojamiento: de aquí sale "ahora" para todas las reglas (regla 15). */
    @Bean
    public Clock reloj(@Value("${sga.alojamiento.zona-horaria}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }

    /** Implementación del puerto ConfiguracionAlojamiento respaldada por application.properties. */
    @Bean
    public ConfiguracionAlojamiento configuracionAlojamiento(
            @Value("${sga.alojamiento.zona-horaria}") String zona,
            @Value("${sga.alojamiento.hora-entrada}") String horaEntrada,
            @Value("${sga.alojamiento.hora-salida}") String horaSalida,
            @Value("${sga.alojamiento.tiempo-preparacion-horas}") int tiempoPreparacionHoras,
            @Value("${sga.alojamiento.plazo-confirmacion-horas}") int plazoConfirmacionHoras,
            @Value("${sga.alojamiento.umbral-edad-facturable-anios}") int umbralEdadAnios,
            @Value("${sga.alojamiento.hora-limite-no-show}") String horaLimiteNoShow,
            @Value("${sga.alojamiento.anticipo-porcentaje}") int anticipoPorcentaje,
            // L-20: reglas propias
            @Value("${sga.alojamiento.descuento-estancia-larga-noches}") int nochesDescuentoEstanciaLarga,
            @Value("${sga.alojamiento.descuento-estancia-larga-porcentaje}") int porcentajeDescuentoEstanciaLarga,
            @Value("${sga.alojamiento.edad-adulto-responsable-anios}") int edadAdultoResponsableAnios,
            Environment entorno) {
        // RP-01: sga.alojamiento.estancia-minima.<TEMPORADA>=<noches>; el nombre de la temporada es un dato (L-08)
        Map<String, Integer> estanciaMinima = Binder.get(entorno)
                .bind("sga.alojamiento.estancia-minima", Bindable.mapOf(String.class, Integer.class))
                .orElse(Map.of());
        // falla al arrancar, y no en la primera cotización, si el descuento está mal configurado
        new PoliticaDescuentoEstanciaLarga(nochesDescuentoEstanciaLarga, porcentajeDescuentoEstanciaLarga);
        return new ConfiguracionPropiedades(
                ZoneId.of(zona), LocalTime.parse(horaEntrada), LocalTime.parse(horaSalida),
                new TiempoPreparacion(tiempoPreparacionHoras), new PlazoConfirmacion(plazoConfirmacionHoras),
                new UmbralEdadFacturable(umbralEdadAnios), LocalTime.parse(horaLimiteNoShow), anticipoPorcentaje,
                new EstanciaMinimaPorTemporada(estanciaMinima).minimos(), nochesDescuentoEstanciaLarga,
                porcentajeDescuentoEstanciaLarga, new EdadAdultoResponsable(edadAdultoResponsableAnios).anios());
    }

    @Bean
    public DisponibilidadApartamentoService disponibilidadApartamentoService(
            ReservaRepository reservaRepository, BloqueoRepository bloqueoRepository,
            ConfiguracionAlojamiento configuracion) {
        return new DisponibilidadApartamentoService(reservaRepository, bloqueoRepository, configuracion);
    }

    @Bean
    public CotizadorEstancia cotizadorEstancia(TarifarioRepository tarifarioRepository,
                                               ConfiguracionAlojamiento configuracion) {
        return new CotizadorEstancia(tarifarioRepository, configuracion);
    }

    @Bean
    public RegistroPoliticaCancelacionService registroPoliticaCancelacionService(
            PoliticaCancelacionRepository politicaRepository) {
        return new RegistroPoliticaCancelacionService(politicaRepository);
    }

    @Bean
    public RegistroTarifaService registroTarifaService(TemporadaRepository temporadaRepository) {
        return new RegistroTarifaService(temporadaRepository);
    }

    @Bean
    public RegistroSalidaService registroSalidaService() {
        return new RegistroSalidaService();
    }

    @Bean
    public RegistroApartamentoService registroApartamentoService(ApartamentoRepository apartamentoRepository) {
        return new RegistroApartamentoService(apartamentoRepository);
    }

    @Bean
    public ConfirmacionReservaService confirmacionReservaService(ConfiguracionAlojamiento configuracion) {
        return new ConfirmacionReservaService(configuracion);
    }

    @Bean
    public RecepcionReservaExternaService recepcionReservaExternaService(ReservaRepository reservaRepository) {
        return new RecepcionReservaExternaService(reservaRepository);
    }

    @Bean
    public RegistroBloqueoService registroBloqueoService(ReservaRepository reservaRepository) {
        return new RegistroBloqueoService(reservaRepository);
    }

    @Bean
    public RetencionCancelacionService retencionCancelacionService(ConfiguracionAlojamiento configuracion) {
        return new RetencionCancelacionService(configuracion);
    }

    @Bean
    public ActivacionApartamentoService activacionApartamentoService(
            TarifarioRepository tarifarioRepository, TemporadaRepository temporadaRepository) {
        return new ActivacionApartamentoService(tarifarioRepository, temporadaRepository);
    }

    /** Valores de la Ficha leídos una vez; el puerto del dominio solo ve los objetos de valor. */
    record ConfiguracionPropiedades(
            ZoneId zonaHoraria, LocalTime horaEntrada, LocalTime horaSalida,
            TiempoPreparacion tiempoPreparacion, PlazoConfirmacion plazoConfirmacion,
            UmbralEdadFacturable umbralEdadFacturable, LocalTime horaLimiteNoShow,
            int anticipoPorcentaje, Map<String, Integer> estanciaMinimaPorTemporada,
            int nochesDescuentoEstanciaLarga, int porcentajeDescuentoEstanciaLarga,
            int edadAdultoResponsableAnios) implements ConfiguracionAlojamiento {
    }
}
