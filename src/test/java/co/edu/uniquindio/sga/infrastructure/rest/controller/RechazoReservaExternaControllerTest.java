package co.edu.uniquindio.sga.infrastructure.rest.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import co.edu.uniquindio.sga.application.usecase.ListarRechazosReservasExternasUseCase;
import co.edu.uniquindio.sga.domain.entity.RechazoReservaExterna;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.IdentificadorExterno;
import co.edu.uniquindio.sga.infrastructure.rest.exception.GlobalExceptionHandler;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.RechazoReservaExternaMapperImpl;

/** Contrato HTTP del listado de reservas externas rechazadas (RN-18). */
@ExtendWith(MockitoExtension.class)
class RechazoReservaExternaControllerTest {

    @Mock private ListarRechazosReservasExternasUseCase listar;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(
                        new RechazoReservaExternaController(listar, new RechazoReservaExternaMapperImpl()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void listarResponde200ConLaConstanciaDelRechazo() throws Exception {
        RechazoReservaExterna rechazo = new RechazoReservaExterna(CanalOrigen.EXTERNO,
                new IdentificadorExterno("BKG-1"), new IdentificacionApartamento("APT-202"),
                new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12)),
                "El apartamento ya tiene una reserva activa que solapa esas noches",
                LocalDateTime.of(2026, 11, 2, 10, 0));
        when(listar.ejecutar(new SolicitudPagina(0))).thenReturn(new Pagina<>(List.of(rechazo), 0, 1));

        mvc.perform(get("/api/reservas-externas/rechazos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].canalOrigen").value("EXTERNO"))
                .andExpect(jsonPath("$.contenido[0].identificadorExterno").value("BKG-1"))
                .andExpect(jsonPath("$.contenido[0].identificacionApartamento").value("APT-202"))
                .andExpect(jsonPath("$.contenido[0].fechaEntrada").value("2026-12-10"))
                .andExpect(jsonPath("$.contenido[0].fechaSalida").value("2026-12-12"))
                .andExpect(jsonPath("$.contenido[0].motivo").value(org.hamcrest.Matchers.containsString("solapa")))
                .andExpect(jsonPath("$.totalPaginas").value(1));
    }

    @Test
    void unaPaginaNegativaResponde400() throws Exception {
        mvc.perform(get("/api/reservas-externas/rechazos").param("pagina", "-1"))
                .andExpect(status().isBadRequest());
    }
}
