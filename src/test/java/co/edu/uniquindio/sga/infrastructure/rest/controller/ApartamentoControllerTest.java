package co.edu.uniquindio.sga.infrastructure.rest.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.application.usecase.*;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.infrastructure.rest.exception.GlobalExceptionHandler;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.ApartamentoMapperImpl;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.SolicitudMapperImpl;

/** Contrato HTTP del recurso Apartamento. */
@ExtendWith(MockitoExtension.class)
class ApartamentoControllerTest {

    @Mock private CrearApartamentoUseCase crear;
    @Mock private ConsultarApartamentoUseCase consultar;
    @Mock private ListarApartamentosUseCase listar;
    @Mock private ActualizarApartamentoUseCase actualizar;
    @Mock private ActivarApartamentoUseCase activar;
    @Mock private DarDeBajaApartamentoUseCase darDeBaja;
    private MockMvc mvc;

    private final IdentificacionApartamento id = new IdentificacionApartamento("APT-101");

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ApartamentoController(crear, consultar, listar, actualizar,
                        activar, darDeBaja, new ApartamentoMapperImpl(), new SolicitudMapperImpl()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private Apartamento apartamento() {
        return new Apartamento(id, "Vista", 2, 4);
    }

    @Test
    void crearResponde201ConLocationYElApartamentoNaceInactivo() throws Exception {
        when(crear.ejecutar(eq(id), eq("Vista"), eq(2), eq(4))).thenReturn(apartamento());

        mvc.perform(post("/api/apartamentos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificacion\":\"apt-101\",\"nombre\":\"Vista\",\"dormitorios\":2,\"capacidad\":4}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/apartamentos/APT-101")))
                .andExpect(jsonPath("$.identificacion").value("APT-101"))
                .andExpect(jsonPath("$.estadoOperativo").value("PREPARADO"))
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    void crearConDatosInvalidosResponde400() throws Exception {
        mvc.perform(post("/api/apartamentos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"dormitorios\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.identificacion").exists())
                .andExpect(jsonPath("$.errores.nombre").exists())
                .andExpect(jsonPath("$.errores.dormitorios").exists())
                .andExpect(jsonPath("$.errores.capacidad").exists());
    }

    @Test
    void crearConIdentificacionRepetidaResponde409() throws Exception {
        when(crear.ejecutar(any(), any(), any(Integer.class), any(Integer.class)))
                .thenThrow(new ReglaDominioException("Ya existe un apartamento con la identificación APT-101"));

        mvc.perform(post("/api/apartamentos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificacion\":\"APT-101\",\"nombre\":\"Vista\",\"dormitorios\":2,\"capacidad\":4}"))
                .andExpect(status().isConflict());
    }

    @Test
    void listarResponde200PaginadoDeDiezEnDiez() throws Exception {
        when(listar.ejecutar(new SolicitudPagina(0))).thenReturn(new Pagina<>(List.of(apartamento()), 0, 1));

        mvc.perform(get("/api/apartamentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamanio").value(10))
                .andExpect(jsonPath("$.contenido[0].nombre").value("Vista"));
    }

    @Test
    void consultarResponde200Y404() throws Exception {
        when(consultar.ejecutar(id)).thenReturn(apartamento());
        mvc.perform(get("/api/apartamentos/APT-101")).andExpect(status().isOk())
                .andExpect(jsonPath("$.capacidad").value(4));

        IdentificacionApartamento otro = new IdentificacionApartamento("APT-999");
        when(consultar.ejecutar(otro)).thenThrow(new ApartamentoNoEncontradoException(otro));
        mvc.perform(get("/api/apartamentos/APT-999")).andExpect(status().isNotFound());
    }

    @Test
    void actualizarResponde200() throws Exception {
        Apartamento editado = apartamento();
        editado.actualizarDatos("Nuevo", 3, 6);
        when(actualizar.ejecutar(id, "Nuevo", 3, 6)).thenReturn(editado);

        mvc.perform(put("/api/apartamentos/APT-101").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Nuevo\",\"dormitorios\":3,\"capacidad\":6}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nuevo"));
    }

    @Test
    void activarSinTarifasCompletasResponde409() throws Exception {
        when(activar.ejecutar(id)).thenThrow(new ReglaDominioException(
                "No se puede activar el apartamento: faltan tarifas para las temporadas ALTA"));

        mvc.perform(put("/api/apartamentos/APT-101/activar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(
                        "No se puede activar el apartamento: faltan tarifas para las temporadas ALTA"));
    }

    @Test
    void activarYDarDeBajaResponden200() throws Exception {
        Apartamento activo = apartamento();
        activo.activar();
        when(activar.ejecutar(id)).thenReturn(activo);
        mvc.perform(put("/api/apartamentos/APT-101/activar")).andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));

        activo.darDeBaja();
        when(darDeBaja.ejecutar(id)).thenReturn(activo);
        mvc.perform(put("/api/apartamentos/APT-101/baja")).andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    void noExisteEliminacionFisica() throws Exception {
        mvc.perform(delete("/api/apartamentos/APT-101")).andExpect(status().isMethodNotAllowed());
    }
}
