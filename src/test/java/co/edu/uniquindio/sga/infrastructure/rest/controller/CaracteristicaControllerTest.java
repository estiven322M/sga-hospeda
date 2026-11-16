package co.edu.uniquindio.sga.infrastructure.rest.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import co.edu.uniquindio.sga.application.usecase.ListarCaracteristicasUseCase;
import co.edu.uniquindio.sga.domain.valueobject.Caracteristica;
import co.edu.uniquindio.sga.infrastructure.rest.exception.GlobalExceptionHandler;

/** L-06: catálogo cerrado de características. La autorización por rol se prueba en SeguridadApiIntegracionTest. */
class CaracteristicaControllerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new CaracteristicaController(new ListarCaracteristicasUseCase()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void devuelveElCatalogoCompletoEnElOrdenDelEnum() throws Exception {
        mvc.perform(get("/api/caracteristicas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(Caracteristica.values().length)))
                .andExpect(jsonPath("$", contains("TERRAZA", "BALCON", "VISTA_AL_PARQUE", "VISTA_PANORAMICA",
                        "JACUZZI", "PARQUEADERO", "ACCESIBLE")));
    }

    @Test
    void esSoloLecturaNoSePuedeCrearUnaCaracteristica() throws Exception {
        mvc.perform(post("/api/caracteristicas").contentType("application/json").content("\"PISCINA\""))
                .andExpect(status().isMethodNotAllowed());
    }
}
