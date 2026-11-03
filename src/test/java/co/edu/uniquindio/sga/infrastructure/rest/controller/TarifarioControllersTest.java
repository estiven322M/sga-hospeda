package co.edu.uniquindio.sga.infrastructure.rest.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import co.edu.uniquindio.sga.application.exception.PoliticaCancelacionNoEncontradaException;
import co.edu.uniquindio.sga.application.usecase.*;
import co.edu.uniquindio.sga.domain.entity.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.entity.Temporada;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.valueobject.*;
import co.edu.uniquindio.sga.infrastructure.rest.exception.GlobalExceptionHandler;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.SolicitudMapperImpl;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.TarifarioMapperImpl;

/** Contrato HTTP de temporadas, política de cancelación y tarifas. */
@ExtendWith(MockitoExtension.class)
class TarifarioControllersTest {

    @Mock private RegistrarTemporadaUseCase registrarTemporada;
    @Mock private ListarTemporadasUseCase listarTemporadas;
    @Mock private RegistrarPoliticaCancelacionUseCase registrarPolitica;
    @Mock private ConsultarPoliticaVigenteUseCase politicaVigente;
    @Mock private ConsultarPoliticaPorVersionUseCase politicaPorVersion;
    @Mock private RegistrarTarifaUseCase registrarTarifa;
    @Mock private ListarTarifasUseCase listarTarifas;
    private MockMvc mvc;

    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-02T15:00:00Z"), ZoneId.of("America/Bogota"));
    private final IdentificacionApartamento apt = new IdentificacionApartamento("APT-101");

    @BeforeEach
    void setUp() {
        var mapper = new TarifarioMapperImpl();
        var solicitud = new SolicitudMapperImpl();
        mvc = MockMvcBuilders.standaloneSetup(
                        new TemporadaController(registrarTemporada, listarTemporadas, mapper, solicitud),
                        new PoliticaCancelacionController(registrarPolitica, politicaVigente, politicaPorVersion,
                                mapper, solicitud, reloj),
                        new TarifaController(registrarTarifa, listarTarifas, mapper, solicitud))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private PoliticaCancelacion politica() {
        return new PoliticaCancelacion(new VersionPolitica(2), LocalDate.of(2026, 6, 1),
                List.of(new TramoRetencion(0, 100), new TramoRetencion(48, 0)));
    }

    // ---------- Temporadas ----------

    @Test
    void registrarTemporadaResponde201() throws Exception {
        when(registrarTemporada.ejecutar(any())).thenReturn(new Temporada("ALTA", false,
                List.of(new Periodo(LocalDate.of(2026, 12, 15), LocalDate.of(2027, 1, 15)))));

        mvc.perform(post("/api/temporadas").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre":"ALTA","base":false,
                         "periodos":[{"desde":"2026-12-15","hasta":"2027-01-15"}]}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/temporadas")))
                .andExpect(jsonPath("$.nombre").value("ALTA"))
                .andExpect(jsonPath("$.base").value(false))
                .andExpect(jsonPath("$.periodos[0].desde").value("2026-12-15"));
    }

    @Test
    void registrarTemporadaSinNombreOSinIndicarSiEsBaseResponde400() throws Exception {
        mvc.perform(post("/api/temporadas").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists())
                .andExpect(jsonPath("$.errores.base").exists());
    }

    @Test
    void registrarTemporadaQueSeSolapaResponde409() throws Exception {
        when(registrarTemporada.ejecutar(any()))
                .thenThrow(new ReglaDominioException("Las temporadas ALTA y FERIADO se solapan"));

        mvc.perform(post("/api/temporadas").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre":"FERIADO","base":false,"periodos":[{"desde":"2027-01-10","hasta":"2027-01-20"}]}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void temporadaNoBaseSinPeriodosResponde409PorReglaDeDominio() throws Exception {
        mvc.perform(post("/api/temporadas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"ALTA\",\"base\":false}"))
                .andExpect(status().isConflict());
    }

    @Test
    void listarTemporadasResponde200Paginado() throws Exception {
        when(listarTemporadas.ejecutar(new SolicitudPagina(0)))
                .thenReturn(new Pagina<>(List.of(new Temporada("BASE", true, List.of())), 0, 1));

        mvc.perform(get("/api/temporadas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].base").value(true))
                .andExpect(jsonPath("$.tamanio").value(10));
    }

    // ---------- Política ----------

    @Test
    void registrarPoliticaResponde201ConLocationHaciaLaVersion() throws Exception {
        when(registrarPolitica.ejecutar(eq(LocalDate.of(2026, 6, 1)), any())).thenReturn(politica());

        mvc.perform(post("/api/politicas-cancelacion").contentType(MediaType.APPLICATION_JSON).content("""
                        {"vigenteDesde":"2026-06-01",
                         "tramos":[{"horasAntelacionMinima":48,"porcentajeRetencion":0},
                                   {"horasAntelacionMinima":0,"porcentajeRetencion":100}]}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/politicas-cancelacion/2")))
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.tramos[0].horasAntelacionMinima").value(48))
                .andExpect(jsonPath("$.tramos[1].porcentajeRetencion").value(100));
    }

    @Test
    void registrarPoliticaConPorcentajeFueraDeRangoResponde400() throws Exception {
        mvc.perform(post("/api/politicas-cancelacion").contentType(MediaType.APPLICATION_JSON).content("""
                        {"vigenteDesde":"2026-06-01",
                         "tramos":[{"horasAntelacionMinima":0,"porcentajeRetencion":150}]}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrarPoliticaConUnSoloTramoResponde409() throws Exception {
        when(registrarPolitica.ejecutar(any(), any())).thenThrow(
                new ReglaDominioException("La política de cancelación requiere al menos dos tramos"));

        mvc.perform(post("/api/politicas-cancelacion").contentType(MediaType.APPLICATION_JSON).content("""
                        {"vigenteDesde":"2026-06-01","tramos":[{"horasAntelacionMinima":0,"porcentajeRetencion":100}]}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void consultarLaVigenteUsaLaFechaDelServidor() throws Exception {
        when(politicaVigente.ejecutar(LocalDate.of(2026, 11, 2))).thenReturn(politica());

        mvc.perform(get("/api/politicas-cancelacion/vigente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    void consultarUnaVersionInexistenteResponde404() throws Exception {
        when(politicaPorVersion.ejecutar(new VersionPolitica(9)))
                .thenThrow(new PoliticaCancelacionNoEncontradaException("versión 9"));

        mvc.perform(get("/api/politicas-cancelacion/9")).andExpect(status().isNotFound());
    }

    // ---------- Tarifas ----------

    @Test
    void definirTarifaResponde200() throws Exception {
        when(registrarTarifa.ejecutar(eq(apt), eq("ALTA"), eq(Dinero.de(120_000))))
                .thenReturn(new Tarifa("ALTA", Dinero.de(120_000)));

        mvc.perform(put("/api/apartamentos/APT-101/tarifas/ALTA").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valorPorOcupanteNoche\":120000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temporada").value("ALTA"))
                .andExpect(jsonPath("$.valorPorOcupanteNoche").value(120000))
                .andExpect(jsonPath("$.moneda").value("COP"));
    }

    @Test
    void definirTarifaNoPositivaResponde400() throws Exception {
        mvc.perform(put("/api/apartamentos/APT-101/tarifas/ALTA").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valorPorOcupanteNoche\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.valorPorOcupanteNoche").exists());
    }

    @Test
    void definirTarifaDeUnaTemporadaInexistenteResponde409() throws Exception {
        when(registrarTarifa.ejecutar(any(), any(), any()))
                .thenThrow(new ReglaDominioException("La temporada FERIADOS no existe"));

        mvc.perform(put("/api/apartamentos/APT-101/tarifas/FERIADOS").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valorPorOcupanteNoche\":100000}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("La temporada FERIADOS no existe"));
    }

    @Test
    void listarTarifasResponde200() throws Exception {
        when(listarTarifas.ejecutar(apt, new SolicitudPagina(0)))
                .thenReturn(new Pagina<>(List.of(new Tarifa("BASE", Dinero.de(80_000))), 0, 1));

        mvc.perform(get("/api/apartamentos/APT-101/tarifas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].temporada").value("BASE"));
    }
}
