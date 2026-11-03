package co.edu.uniquindio.sga.infrastructure.rest.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.application.usecase.DeclararFueraDeServicioUseCase;
import co.edu.uniquindio.sga.application.usecase.IniciarPreparacionUseCase;
import co.edu.uniquindio.sga.application.usecase.MarcarPreparadoUseCase;
import co.edu.uniquindio.sga.application.usecase.ReincorporarApartamentoUseCase;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.infrastructure.rest.exception.GlobalExceptionHandler;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.ApartamentoMapperImpl;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.SolicitudMapperImpl;

/** Contrato HTTP de las transiciones del estado operativo: cada una es una operación de negocio con nombre propio. */
@ExtendWith(MockitoExtension.class)
class EstadoOperativoControllerTest {

    @Mock private IniciarPreparacionUseCase iniciar;
    @Mock private MarcarPreparadoUseCase preparado;
    @Mock private DeclararFueraDeServicioUseCase fuera;
    @Mock private ReincorporarApartamentoUseCase reincorporar;
    private MockMvc mvc;

    private final IdentificacionApartamento id = new IdentificacionApartamento("APT-101");

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new EstadoOperativoController(iniciar, preparado, fuera, reincorporar,
                        new ApartamentoMapperImpl(), new SolicitudMapperImpl()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private Apartamento apartamento() {
        Apartamento apt = new Apartamento(id, "Vista", 2, 4);
        apt.activar();
        return apt;
    }

    @Test
    void cicloDeServicioResponde200ConElNuevoEstadoOperativo() throws Exception {
        Apartamento enPreparacion = apartamento();
        enPreparacion.marcarOcupado();
        enPreparacion.liberar();
        enPreparacion.iniciarPreparacion();
        when(iniciar.ejecutar(id)).thenReturn(enPreparacion);
        mvc.perform(put("/api/apartamentos/APT-101/estado-operativo/iniciar-preparacion"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estadoOperativo").value("EN_PREPARACION"));

        enPreparacion.marcarPreparado();
        when(preparado.ejecutar(id)).thenReturn(enPreparacion);
        mvc.perform(put("/api/apartamentos/APT-101/estado-operativo/marcar-preparado"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estadoOperativo").value("PREPARADO"));
    }

    @Test
    void declararFueraDeServicioYReincorporarResponden200() throws Exception {
        Apartamento apt = apartamento();
        apt.declararFueraDeServicio();
        when(fuera.ejecutar(id)).thenReturn(apt);
        mvc.perform(put("/api/apartamentos/APT-101/estado-operativo/declarar-fuera-de-servicio"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estadoOperativo").value("FUERA_DE_SERVICIO"));

        apt.reincorporarAlServicio();
        when(reincorporar.ejecutar(id)).thenReturn(apt);
        mvc.perform(put("/api/apartamentos/APT-101/estado-operativo/reincorporar"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estadoOperativo").value("PENDIENTE_PREPARACION"));
    }

    @Test
    void unaTransicionInvalidaResponde409() throws Exception {
        when(fuera.ejecutar(id)).thenThrow(new ReglaDominioException("No se puede pasar de OCUPADO a FUERA_DE_SERVICIO"));

        mvc.perform(put("/api/apartamentos/APT-101/estado-operativo/declarar-fuera-de-servicio"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("No se puede pasar de OCUPADO a FUERA_DE_SERVICIO"));
    }

    @Test
    void unApartamentoInexistenteResponde404() throws Exception {
        when(iniciar.ejecutar(id)).thenThrow(new ApartamentoNoEncontradoException(id));

        mvc.perform(put("/api/apartamentos/APT-101/estado-operativo/iniciar-preparacion"))
                .andExpect(status().isNotFound());
    }

    @Test
    void noExisteUnEndpointGenericoParaFijarElEstadoOperativo() throws Exception {
        mvc.perform(put("/api/apartamentos/APT-101/estado-operativo/PREPARADO")).andExpect(status().isNotFound());
    }
}
