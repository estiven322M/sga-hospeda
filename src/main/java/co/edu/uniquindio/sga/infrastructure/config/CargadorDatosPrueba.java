package co.edu.uniquindio.sga.infrastructure.config;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Temporada;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.BloqueoRepository;
import co.edu.uniquindio.sga.domain.repository.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.repository.TarifarioRepository;
import co.edu.uniquindio.sga.domain.repository.TemporadaRepository;
import co.edu.uniquindio.sga.domain.service.ActivacionApartamentoService;
import co.edu.uniquindio.sga.domain.service.RegistroApartamentoService;
import co.edu.uniquindio.sga.domain.service.RegistroBloqueoService;
import co.edu.uniquindio.sga.domain.service.RegistroPoliticaCancelacionService;
import co.edu.uniquindio.sga.domain.service.RegistroTarifaService;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Periodo;
import co.edu.uniquindio.sga.infrastructure.config.CatalogoDatosPrueba.ApartamentoSemilla;
import co.edu.uniquindio.sga.infrastructure.config.CatalogoDatosPrueba.TemporadaSemilla;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Carga los datos de prueba al iniciar (regla 12). Respeta el orden del dominio: primero las temporadas, luego
 * cada apartamento con sus tarifas antes de activarlo, la política, el bloqueo y, al final, las reservas.
 * Es idempotente: si ya hay apartamentos no hace nada.
 */
@Component
@ConditionalOnProperty(name = "sga.datos-prueba.habilitado", havingValue = "true")
public class CargadorDatosPrueba implements ApplicationRunner {

    private static final Logger REGISTRO = LoggerFactory.getLogger(CargadorDatosPrueba.class);
    private static final int PORCENTAJE_COMPLETO = 100;

    private final LocalDate hoy;
    private final TemporadaRepository temporadas;
    private final ApartamentoRepository apartamentos;
    private final TarifarioRepository tarifario;
    private final PoliticaCancelacionRepository politicas;
    private final BloqueoRepository bloqueos;
    private final RegistroApartamentoService registroApartamento;
    private final RegistroTarifaService registroTarifa;
    private final ActivacionApartamentoService activacion;
    private final RegistroPoliticaCancelacionService registroPolitica;
    private final RegistroBloqueoService registroBloqueo;
    private final SembradorReservasPrueba sembradorReservas;

    CargadorDatosPrueba(Clock reloj, TemporadaRepository temporadas, ApartamentoRepository apartamentos,
                        TarifarioRepository tarifario, PoliticaCancelacionRepository politicas,
                        BloqueoRepository bloqueos, RegistroApartamentoService registroApartamento,
                        RegistroTarifaService registroTarifa, ActivacionApartamentoService activacion,
                        RegistroPoliticaCancelacionService registroPolitica, RegistroBloqueoService registroBloqueo,
                        SembradorReservasPrueba sembradorReservas) {
        this.hoy = LocalDate.now(reloj);
        this.temporadas = temporadas;
        this.apartamentos = apartamentos;
        this.tarifario = tarifario;
        this.politicas = politicas;
        this.bloqueos = bloqueos;
        this.registroApartamento = registroApartamento;
        this.registroTarifa = registroTarifa;
        this.activacion = activacion;
        this.registroPolitica = registroPolitica;
        this.registroBloqueo = registroBloqueo;
        this.sembradorReservas = sembradorReservas;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments argumentos) {
        if (apartamentos.listarTodos(SolicitudPagina.primera()).totalElementos() > 0) {
            REGISTRO.info("Los datos de prueba ya están cargados; no se vuelven a sembrar.");
            return;
        }
        cargarTemporadas();
        CatalogoDatosPrueba.APARTAMENTOS.forEach(this::cargarApartamento);
        cargarPoliticaDeCancelacion();
        cargarBloqueo();
        sembradorReservas.sembrar();
        REGISTRO.info("Datos de prueba cargados.");
    }

    private void cargarTemporadas() {
        CatalogoDatosPrueba.TEMPORADAS.forEach(semilla ->
                temporadas.guardar(new Temporada(semilla.nombre(), semilla.esBase(), periodosDe(semilla))));
    }

    private List<Periodo> periodosDe(TemporadaSemilla semilla) {
        int primerAnio = hoy.getYear() - CatalogoDatosPrueba.ANIOS_ANTERIORES_CUBIERTOS;
        int ultimoAnio = hoy.getYear() + CatalogoDatosPrueba.ANIOS_POSTERIORES_CUBIERTOS;
        return IntStream.rangeClosed(primerAnio, ultimoAnio).boxed()
                .flatMap(anio -> semilla.periodos().stream().map(periodo -> new Periodo(
                        periodo.desde().atYear(anio),
                        periodo.hasta().atYear(periodo.cruzaDeAnio() ? anio + 1 : anio))))
                .toList();
    }

    /** Un apartamento nace inactivo (7.4): se crea, se le registran tarifas en todas las temporadas y se activa. */
    private void cargarApartamento(ApartamentoSemilla semilla) {
        IdentificacionApartamento identificacion = new IdentificacionApartamento(semilla.identificacion());
        Apartamento apartamento = apartamentos.guardar(registroApartamento.crear(identificacion, semilla.nombre(),
                semilla.dormitorios(), semilla.capacidad()));
        CatalogoDatosPrueba.TEMPORADAS.forEach(temporada -> tarifario.guardarTarifa(identificacion,
                registroTarifa.crear(temporada.nombre(), tarifaDe(semilla, temporada))));
        activacion.activar(apartamento);
        if (semilla.fueraDeServicio()) {
            apartamento.declararFueraDeServicio();
        }
        apartamentos.guardar(apartamento);
    }

    private static Dinero tarifaDe(ApartamentoSemilla apartamento, TemporadaSemilla temporada) {
        return Dinero.de(apartamento.tarifaBase() * temporada.porcentajeSobreTarifaBase() / PORCENTAJE_COMPLETO);
    }

    private void cargarPoliticaDeCancelacion() {
        politicas.guardar(registroPolitica.crearNuevaVersion(
                hoy.minusDays(CatalogoDatosPrueba.DIAS_DE_VIGENCIA_PREVIA_POLITICA),
                CatalogoDatosPrueba.TRAMOS_POLITICA));
    }

    private void cargarBloqueo() {
        LocalDate inicio = hoy.plusDays(CatalogoDatosPrueba.BLOQUEO_INICIA_EN_DIAS);
        bloqueos.guardar(registroBloqueo.crear(
                new IdentificacionApartamento(CatalogoDatosPrueba.APARTAMENTO_CON_BLOQUEO),
                new Estancia(inicio, inicio.plusDays(CatalogoDatosPrueba.BLOQUEO_NOCHES)),
                CatalogoDatosPrueba.BLOQUEO_MOTIVO));
    }
}
