package co.edu.uniquindio.sga.infrastructure.rest.controller;

import co.edu.uniquindio.sga.domain.valueobject.EstadoOperativo;
import co.edu.uniquindio.sga.testsupport.ApartamentoDePrueba;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;

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
import co.edu.uniquindio.sga.domain.repository.FiltroApartamentos;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.valueobject.Caracteristica;
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
    @Mock private co.edu.uniquindio.sga.application.usecase.ConsultarCalendarioUseCase calendario;
    @Mock private AgregarImagenesApartamentoUseCase agregarImagenes;
    @Mock private EstablecerImagenPrincipalUseCase establecerPrincipal;
    @Mock private RetirarImagenApartamentoUseCase retirarImagen;
    private MockMvc mvc;

    private final IdentificacionApartamento id = new IdentificacionApartamento("APT-101");

    private static final String CUERPO_CREAR = """
            {"identificacion":"apt-101","nombre":"Vista","descripcion":"Con vista al parque","dormitorios":2,
             "capacidad":4,"dotacion":["Nevera","Wi-Fi"],"caracteristicas":["VISTA_AL_PARQUE","BALCON"]}""";

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ApartamentoController(crear, consultar, listar, actualizar,
                        activar, darDeBaja, calendario, agregarImagenes, establecerPrincipal, retirarImagen,
                        new co.edu.uniquindio.sga.infrastructure.rest.mapper.ImagenSubidaMapper(), new ApartamentoMapperImpl(),
                        new co.edu.uniquindio.sga.infrastructure.rest.mapper.ReservaMapperImpl(),
                        new co.edu.uniquindio.sga.infrastructure.rest.mapper.CalendarioMapper(), new SolicitudMapperImpl()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private Apartamento apartamento() {
        return ApartamentoDePrueba.nuevo(id, "Vista", 2, 4);
    }

    private Apartamento apartamentoConEquipamiento() {
        return new Apartamento(id, "Vista", "Con vista al parque", 2, 4, List.of("Nevera", "Wi-Fi"),
                Set.of(Caracteristica.VISTA_AL_PARQUE, Caracteristica.BALCON));
    }

    @Test
    void crearResponde201ConLocationYElApartamentoNaceInactivo() throws Exception {
        when(crear.ejecutar(eq(id), eq("Vista"), eq("Con vista al parque"), eq(2), eq(4),
                eq(List.of("Nevera", "Wi-Fi")), eq(Set.of(Caracteristica.VISTA_AL_PARQUE, Caracteristica.BALCON))))
                .thenReturn(apartamentoConEquipamiento());

        mvc.perform(post("/api/apartamentos").contentType(MediaType.APPLICATION_JSON).content(CUERPO_CREAR))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/apartamentos/APT-101")))
                .andExpect(jsonPath("$.identificacion").value("APT-101"))
                .andExpect(jsonPath("$.estadoOperativo").value("PREPARADO"))
                .andExpect(jsonPath("$.activo").value(false))
                // L-06: el cuerpo devuelve lo que se guardó
                .andExpect(jsonPath("$.descripcion").value("Con vista al parque"))
                .andExpect(jsonPath("$.dotacion[0]").value("Nevera"))
                .andExpect(jsonPath("$.dotacion[1]").value("Wi-Fi"))
                .andExpect(jsonPath("$.caracteristicas", containsInAnyOrder("VISTA_AL_PARQUE", "BALCON")));
    }

    @Test
    void crearSinDotacionNiCaracteristicasResponde201ConColeccionesVacias() throws Exception {
        when(crear.ejecutar(eq(id), eq("Vista"), eq("Descripción"), eq(2), eq(4), any(), any()))
                .thenReturn(ApartamentoDePrueba.nuevo(id, "Vista", 2, 4));

        mvc.perform(post("/api/apartamentos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificacion\":\"apt-101\",\"nombre\":\"Vista\",\"descripcion\":\"Descripción\","
                                + "\"dormitorios\":2,\"capacidad\":4}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dotacion", hasSize(0)))
                .andExpect(jsonPath("$.caracteristicas", hasSize(0)));
    }

    @Test
    void crearConDatosInvalidosResponde400() throws Exception {
        mvc.perform(post("/api/apartamentos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"dormitorios\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.identificacion").exists())
                .andExpect(jsonPath("$.errores.nombre").exists())
                .andExpect(jsonPath("$.errores.descripcion").exists())
                .andExpect(jsonPath("$.errores.dormitorios").exists())
                .andExpect(jsonPath("$.errores.capacidad").exists());
        verifyNoInteractions(crear);
    }

    @Test
    void crearConDescripcionDemasiadoLargaOElementoDeDotacionEnBlancoResponde400() throws Exception {
        String larga = "d".repeat(501);
        mvc.perform(post("/api/apartamentos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificacion\":\"APT-101\",\"nombre\":\"V\",\"descripcion\":\"" + larga
                                + "\",\"dormitorios\":1,\"capacidad\":2,\"dotacion\":[\"Nevera\",\" \"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.descripcion").exists())
                .andExpect(jsonPath("$.errores").isNotEmpty());
        verifyNoInteractions(crear);
    }

    @Test
    void crearConUnaCaracteristicaFueraDelCatalogoResponde400() throws Exception {
        mvc.perform(post("/api/apartamentos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificacion\":\"APT-101\",\"nombre\":\"V\",\"descripcion\":\"D\","
                                + "\"dormitorios\":1,\"capacidad\":2,\"caracteristicas\":[\"PISCINA\"]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(crear);
    }

    @Test
    void crearConIdentificacionRepetidaResponde409() throws Exception {
        when(crear.ejecutar(any(), any(), any(), any(Integer.class), any(Integer.class), any(), any()))
                .thenThrow(new ReglaDominioException("Ya existe un apartamento con la identificación APT-101"));

        mvc.perform(post("/api/apartamentos").contentType(MediaType.APPLICATION_JSON).content(CUERPO_CREAR))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe un apartamento con la identificación APT-101"));
    }

    @Test
    void crearConDotacionRepetidaResponde409PorqueEsUnaReglaDelDominioL06() throws Exception {
        when(crear.ejecutar(any(), any(), any(), any(Integer.class), any(Integer.class), any(), any()))
                .thenThrow(new ReglaDominioException("La dotación no puede repetir elementos: Nevera"));

        mvc.perform(post("/api/apartamentos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificacion\":\"APT-101\",\"nombre\":\"V\",\"descripcion\":\"D\","
                                + "\"dormitorios\":1,\"capacidad\":2,\"dotacion\":[\"Nevera\",\"nevera\"]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("La dotación no puede repetir elementos: Nevera"));
    }

    @Test
    void listarResponde200PaginadoDeDiezEnDiezYSinFiltroPorDefecto() throws Exception {
        when(listar.ejecutar(FiltroApartamentos.SIN_FILTRO, new SolicitudPagina(0)))
                .thenReturn(new Pagina<>(List.of(apartamentoConEquipamiento()), 0, 1));

        mvc.perform(get("/api/apartamentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamanio").value(10))
                .andExpect(jsonPath("$.contenido[0].nombre").value("Vista"))
                .andExpect(jsonPath("$.contenido[0].caracteristicas", containsInAnyOrder("VISTA_AL_PARQUE", "BALCON")));
    }

    @Test
    void listarConCaracteristicasLasPasaComoFiltroQueExigeTodasL06() throws Exception {
        FiltroApartamentos esperado = FiltroApartamentos.de(Set.of(Caracteristica.TERRAZA, Caracteristica.JACUZZI));
        when(listar.ejecutar(esperado, new SolicitudPagina(0)))
                .thenReturn(new Pagina<>(List.of(apartamentoConEquipamiento()), 0, 1));

        mvc.perform(get("/api/apartamentos?caracteristicas=TERRAZA&caracteristicas=JACUZZI"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1));
        verify(listar).ejecutar(esperado, new SolicitudPagina(0));
    }

    @Test
    void listarConUnaCaracteristicaFueraDelCatalogoResponde400SinConsultarL06() throws Exception {
        mvc.perform(get("/api/apartamentos?caracteristicas=PISCINA"))
                .andExpect(status().isBadRequest());
        verify(listar, never()).ejecutar(any(), any());
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
    void actualizarResponde200ConLosNuevosDatosL06() throws Exception {
        Apartamento editado = apartamento();
        editado.actualizarDatos("Nuevo", "Nueva descripción", 3, 6, List.of("Lavadora"), Set.of(Caracteristica.JACUZZI));
        when(actualizar.ejecutar(eq(id), eq("Nuevo"), eq("Nueva descripción"), eq(3), eq(6),
                eq(List.of("Lavadora")), eq(Set.of(Caracteristica.JACUZZI))))
                .thenReturn(new ActualizarApartamentoUseCase.Resultado(editado, List.of()));

        mvc.perform(put("/api/apartamentos/APT-101").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Nuevo\",\"descripcion\":\"Nueva descripción\",\"dormitorios\":3,"
                                + "\"capacidad\":6,\"dotacion\":[\"Lavadora\"],\"caracteristicas\":[\"JACUZZI\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nuevo"))
                .andExpect(jsonPath("$.descripcion").value("Nueva descripción"))
                .andExpect(jsonPath("$.dotacion[0]").value("Lavadora"))
                .andExpect(jsonPath("$.caracteristicas[0]").value("JACUZZI"))
                // 7.3: sin bajar la capacidad el aviso viene vacío
                .andExpect(jsonPath("$.reservasPorEncimaDeLaCapacidad", hasSize(0)));
    }

    @Test
    void actualizarSinDescripcionResponde400() throws Exception {
        mvc.perform(put("/api/apartamentos/APT-101").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Nuevo\",\"dormitorios\":3,\"capacidad\":6}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.descripcion").exists());
        verifyNoInteractions(actualizar);
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

    @Test
    void listarConEstadoOperativoLoPasaAlFiltro() throws Exception {
        FiltroApartamentos esperado = FiltroApartamentos.de(Set.of(Caracteristica.BALCON), EstadoOperativo.PREPARADO);
        when(listar.ejecutar(esperado, new SolicitudPagina(0)))
                .thenReturn(new Pagina<>(List.of(apartamentoConEquipamiento()), 0, 1));

        mvc.perform(get("/api/apartamentos?caracteristicas=BALCON&estadoOperativo=PREPARADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].estadoOperativo").value("PREPARADO"));
        verify(listar).ejecutar(esperado, new SolicitudPagina(0));
    }

    @Test
    void listarConUnEstadoOperativoFueraDelCatalogoResponde400SinConsultar() throws Exception {
        mvc.perform(get("/api/apartamentos?estadoOperativo=DESCOMPUESTO")).andExpect(status().isBadRequest());
        verify(listar, never()).ejecutar(any(), any());
    }
}
