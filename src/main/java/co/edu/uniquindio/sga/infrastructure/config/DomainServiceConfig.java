package co.edu.uniquindio.sga.infrastructure.config;

import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.AlojamientoRepository;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.BloqueoRepository;
import co.edu.uniquindio.sga.domain.repository.ServicioAdicionalRepository;
import co.edu.uniquindio.sga.domain.repository.ConfiguracionAlojamiento;
import co.edu.uniquindio.sga.domain.repository.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.repository.TarifarioRepository;
import co.edu.uniquindio.sga.domain.repository.TemporadaRepository;
import co.edu.uniquindio.sga.domain.entity.Alojamiento;
import co.edu.uniquindio.sga.domain.service.ActivacionApartamentoService;
import co.edu.uniquindio.sga.domain.service.BajaUsuarioService;
import co.edu.uniquindio.sga.domain.service.ValidadorImagenes;
import co.edu.uniquindio.sga.domain.service.RedactorNotificaciones;
import co.edu.uniquindio.sga.domain.service.CalendarioOcupacionService;
import co.edu.uniquindio.sga.domain.service.CargoServicioAdicionalService;
import co.edu.uniquindio.sga.domain.service.ConfirmacionReservaService;
import co.edu.uniquindio.sga.domain.service.ControlCapacidadService;
import co.edu.uniquindio.sga.domain.service.RegistroPagoService;
import co.edu.uniquindio.sga.domain.service.RegistroServicioAdicionalService;
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

    /**
     * Implementación del puerto ConfiguracionAlojamiento: los valores salen de application.properties, salvo las horas de
     * entrada y salida, que salen del alojamiento que el administrador edita (ver {@link ConfiguracionConAlojamiento}).
     */
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
            // L-18 y calendario (7.5)
            @Value("${sga.alojamiento.medios-pago}") List<String> mediosPago,
            @Value("${sga.alojamiento.calendario-maximo-noches}") int calendarioMaximoNoches,
            Environment entorno, AlojamientoRepository alojamiento) {
        ConfiguracionAlojamiento base = configuracionPropiedades(zona, horaEntrada, horaSalida, tiempoPreparacionHoras,
                plazoConfirmacionHoras, umbralEdadAnios, horaLimiteNoShow, anticipoPorcentaje,
                nochesDescuentoEstanciaLarga, porcentajeDescuentoEstanciaLarga, edadAdultoResponsableAnios, mediosPago,
                calendarioMaximoNoches, entorno);
        return new ConfiguracionConAlojamiento(base, alojamiento);
    }

    /** Los parámetros leídos de application.properties, validados al arrancar (no en la primera operación). */
    ConfiguracionAlojamiento configuracionPropiedades(
            String zona, String horaEntrada, String horaSalida, int tiempoPreparacionHoras, int plazoConfirmacionHoras,
            int umbralEdadAnios, String horaLimiteNoShow, int anticipoPorcentaje, int nochesDescuentoEstanciaLarga,
            int porcentajeDescuentoEstanciaLarga, int edadAdultoResponsableAnios, List<String> mediosPago,
            int calendarioMaximoNoches, Environment entorno) {
        List<String> medios = mediosPago.stream().map(String::trim).filter(medio -> !medio.isEmpty())
                .map(String::toUpperCase).distinct().toList();
        if (medios.size() < 2) {
            throw new ReglaDominioException("L-18: el alojamiento debe aceptar al menos dos medios de pago (sga.alojamiento.medios-pago)");
        }
        if (calendarioMaximoNoches < 1) {
            throw new ReglaDominioException("El máximo de noches del calendario debe ser al menos 1");
        }
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
                porcentajeDescuentoEstanciaLarga, new EdadAdultoResponsable(edadAdultoResponsableAnios).anios(),
                medios, calendarioMaximoNoches);
    }

    @Bean
    public RegistroServicioAdicionalService registroServicioAdicionalService(ServicioAdicionalRepository servicios) {
        return new RegistroServicioAdicionalService(servicios);
    }

    @Bean
    public CargoServicioAdicionalService cargoServicioAdicionalService() {
        return new CargoServicioAdicionalService();
    }

    @Bean
    public RegistroPagoService registroPagoService(ConfiguracionAlojamiento configuracion) {
        return new RegistroPagoService(configuracion);
    }

    @Bean
    public CalendarioOcupacionService calendarioOcupacionService(ReservaRepository reservaRepository,
                                                                BloqueoRepository bloqueoRepository,
                                                                ConfiguracionAlojamiento configuracion) {
        return new CalendarioOcupacionService(reservaRepository, bloqueoRepository, configuracion);
    }

    @Bean
    public ControlCapacidadService controlCapacidadService(ReservaRepository reservaRepository) {
        return new ControlCapacidadService(reservaRepository);
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

    /** 7.3 y 7.2: tamaños máximos de las imágenes, en MB, desde application.properties (regla 10). */
    @Bean
    public ValidadorImagenes validadorImagenes(@Value("${sga.imagenes.tamano-maximo-mb}") int maximoApartamentoMb,
                                               @Value("${sga.imagenes.tamano-maximo-perfil-mb}") int maximoPerfilMb) {
        return new ValidadorImagenes(maximoApartamentoMb * 1024L * 1024L, maximoPerfilMb * 1024L * 1024L);
    }

    @Bean
    public BajaUsuarioService bajaUsuarioService(ReservaRepository reservaRepository) {
        return new BajaUsuarioService(reservaRepository);
    }

    @Bean
    public RedactorNotificaciones redactorNotificaciones(AlojamientoRepository alojamientoRepository) {
        return new RedactorNotificaciones(() -> alojamientoRepository.obtener().map(Alojamiento::getNombre).orElse("Hospeda"));
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
            int edadAdultoResponsableAnios, List<String> mediosPagoAceptados,
            int calendarioMaximoNoches) implements ConfiguracionAlojamiento {
    }
}
