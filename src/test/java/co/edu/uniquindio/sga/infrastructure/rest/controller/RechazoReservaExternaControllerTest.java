package co.edu.uniquindio.sga.infrastructure.rest.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

import co.edu.uniquindio.sga.application.exception.RechazoNoEncontradoException;
import co.edu.uniquindio.sga.application.usecase.ListarRechazosReservasExternasUseCase;
import co.edu.uniquindio.sga.application.usecase.ResolverRechazoReservaExternaUseCase;
import co.edu.uniquindio.sga.domain.entity.RechazoReservaExterna;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.IdentificadorExterno;
import co.edu.uniquindio.sga.infrastructure.rest.exception.GlobalExceptionHandler;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.RechazoReservaExternaMapperImpl;

/** Contrato HTTP de los conflictos del canal externo: el listado (RN-18) y su resolución (7.8). */
@ExtendWith(MockitoExtension.class)
class RechazoReservaExternaControllerTest {

    @Mock private ListarRechazosReservasExternasUseCase listar;
    @Mock private ResolverRechazoReservaExternaUseCase resolver;

    private MockMvc mvc;
    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-16T15:00:00Z"), ZoneId.of("America/Bogota"));

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(
                        new RechazoReservaExternaController(listar, resolver, new RechazoReservaExternaMapperImpl(), reloj))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private static RechazoReservaExterna rechazo(String identificador) {
        return new RechazoReservaExterna(CanalOrigen.EXTERNO, new IdentificadorExterno(identificador),
                new IdentificacionApartamento("APT-202"),
                new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12)),
                "El apartamento ya tiene una reserva activa que solapa esas noches",
                LocalDateTime.of(2026, 11, 2, 10, 0));
    }

    @Test
    void listarResponde200ConLaConstanciaDelRechazo() throws Exception {
        when(listar.ejecutar(null, new SolicitudPagina(0))).thenReturn(new Pagina<>(List.of(rechazo("BKG-1")), 0, 1));

        mvc.perform(get("/api/reservas-externas/rechazos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].canalOrigen").value("EXTERNO"))
                .andExpect(jsonPath("$.contenido[0].identificadorExterno").value("BKG-1"))
                .andExpect(jsonPath("$.contenido[0].identificacionApartamento").value("APT-202"))
                .andExpect(jsonPath("$.contenido[0].fechaEntrada").value("2026-12-10"))
                .andExpect(jsonPath("$.contenido[0].fechaSalida").value("2026-12-12"))
                .andExpect(jsonPath("$.contenido[0].motivo").value(containsString("solapa")))
                .andExpect(jsonPath("$.contenido[0].revisado").value(false))
                .andExpect(jsonPath("$.contenido[0].canal").doesNotExist())
                .andExpect(jsonPath("$.totalPaginas").value(1));
    }

    @Test
    void elCanalSaleDelPrefijoDelIdentificadorGuardado() throws Exception {
        when(listar.ejecutar(null, new SolicitudPagina(0))).thenReturn(new Pagina<>(List.of(rechazo("DEMO:RES-9001")), 0, 1));

        mvc.perform(get("/api/reservas-externas/rechazos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].canal").value("DEMO"));
    }

    @Test
    void elFiltroDeRevisionLlegaAlCasoDeUso() throws Exception {
        when(listar.ejecutar(false, new SolicitudPagina(0))).thenReturn(new Pagina<>(List.of(), 0, 0));

        mvc.perform(get("/api/reservas-externas/rechazos").param("revisado", "false"))
                .andExpect(status().isOk());

        verify(listar).ejecutar(false, new SolicitudPagina(0));
    }

    @Test
    void unaPaginaNegativaResponde400() throws Exception {
        mvc.perform(get("/api/reservas-externas/rechazos").param("pagina", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resolverResponde200ConLaRevisionYElMomentoLoPoneElServidor() throws Exception {
        RechazoReservaExterna revisado = rechazo("BKG-1");
        revisado.resolver("Se habló con el canal", "Administrador SGA", LocalDateTime.of(2026, 11, 16, 10, 0));
        when(resolver.ejecutar(eq(7L), eq("Se habló con el canal"), any(), eq(LocalDateTime.of(2026, 11, 16, 10, 0)))).thenReturn(revisado);
        var admin = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("admin@sga.local", null);

        mvc.perform(put("/api/reservas-externas/rechazos/7/resolver").principal(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nota\":\"Se habló con el canal\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revisado").value(true))
                .andExpect(jsonPath("$.notaRevision").value("Se habló con el canal"))
                .andExpect(jsonPath("$.revisadoPor").value("Administrador SGA"))
                .andExpect(jsonPath("$.revisadoEn").exists());
    }

    @Test
    void resolverSinNotaResponde400PorCampo() throws Exception {
        mvc.perform(put("/api/reservas-externas/rechazos/7/resolver").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nota").exists());
    }

    @Test
    void resolverUnConflictoYaRevisadoResponde409() throws Exception {
        when(resolver.ejecutar(eq(7L), any(), any(), any())).thenThrow(new ReglaDominioException("El conflicto ya fue revisado por Ana"));

        mvc.perform(put("/api/reservas-externas/rechazos/7/resolver").principal(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("a@sga.local", null))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nota\":\"otra\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El conflicto ya fue revisado por Ana"));
    }

    @Test
    void resolverUnConflictoQueNoExisteResponde404() throws Exception {
        when(resolver.ejecutar(eq(99L), any(), any(), any())).thenThrow(new RechazoNoEncontradoException(99));

        mvc.perform(put("/api/reservas-externas/rechazos/99/resolver").principal(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("a@sga.local", null))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nota\":\"x\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
