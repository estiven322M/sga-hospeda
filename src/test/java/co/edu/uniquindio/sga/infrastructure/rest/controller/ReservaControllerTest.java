package co.edu.uniquindio.sga.infrastructure.rest.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import co.edu.uniquindio.sga.application.exception.ReservaNoEncontradaException;
import co.edu.uniquindio.sga.application.usecase.CancelarReservaUseCase;
import co.edu.uniquindio.sga.application.usecase.ConfirmarReservaUseCase;
import co.edu.uniquindio.sga.application.usecase.ConsultarReservaUseCase;
import co.edu.uniquindio.sga.application.usecase.CrearReservaUseCase;
import co.edu.uniquindio.sga.application.usecase.DeclararNoShowUseCase;
import co.edu.uniquindio.sga.application.usecase.ListarReservasUseCase;
import co.edu.uniquindio.sga.application.usecase.RegistrarLlegadaUseCase;
import co.edu.uniquindio.sga.application.usecase.RegistrarSalidaUseCase;
import co.edu.uniquindio.sga.application.usecase.ModificarReservaUseCase;
import co.edu.uniquindio.sga.domain.entity.Ocupante;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.FiltroReservas;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.valueobject.*;
import co.edu.uniquindio.sga.infrastructure.rest.exception.GlobalExceptionHandler;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.ReservaMapperImpl;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.SolicitudMapperImpl;

/** Contrato HTTP del recurso Reserva: códigos, Location, forma del error y delegación al caso de uso. */
@ExtendWith(MockitoExtension.class)
class ReservaControllerTest {

    @Mock private CrearReservaUseCase crear;
    @Mock private ConsultarReservaUseCase consultar;
    @Mock private ListarReservasUseCase listar;
    @Mock private ModificarReservaUseCase modificar;
    @Mock private CancelarReservaUseCase cancelar;
    @Mock private ConfirmarReservaUseCase confirmar;
    @Mock private RegistrarLlegadaUseCase llegada;
    @Mock private RegistrarSalidaUseCase salida;
    @Mock private DeclararNoShowUseCase noShow;
    private MockMvc mvc;

    // 10:00 hora de Colombia del 2-nov-2026
    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-02T15:00:00Z"), ZoneId.of("America/Bogota"));
    private final CodigoReserva codigo = new CodigoReserva("RES-2026-00001");
    private final Ocupante titular = new Ocupante(new DocumentoIdentidad("CC-100"), "Juan",
            LocalDate.of(1990, 1, 1), LocalDate.of(2026, 1, 1));

    private static final String CREAR = """
            {"identificacionApartamento":"APT-101","fechaEntrada":"2026-12-10","fechaSalida":"2026-12-12",
             "canalOrigen":"PORTAL","horaEstimadaLlegada":"16:00",
             "titular":{"documento":"CC-100","nombre":"Juan","fechaNacimiento":"1990-01-01"},
             "ocupantes":[{"documento":"CC-100","nombre":"Juan","fechaNacimiento":"1990-01-01"}]}
            """;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ReservaController(crear, consultar, listar, modificar, cancelar,
                        confirmar, llegada, salida, noShow, new ReservaMapperImpl(), new SolicitudMapperImpl(), reloj))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private Reserva reserva() {
        return Reserva.crear(codigo, new IdentificacionApartamento("APT-101"),
                new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12)), titular, List.of(titular),
                CanalOrigen.PORTAL, new ValorCongelado(List.of(
                        new CargoNoche(LocalDate.of(2026, 12, 10), "ALTA", Dinero.de(100_000), 1),
                        new CargoNoche(LocalDate.of(2026, 12, 11), "ALTA", Dinero.de(100_000), 1))),
                new VersionPolitica(2), LocalDateTime.of(2026, 11, 2, 10, 0));
    }

    @Test
    void crearResponde201ConLocationYElMomentoActualLoPoneElServidor() throws Exception {
        when(crear.ejecutar(any(), any(), any(), any(), any(), any(), any())).thenReturn(reserva());

        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content(CREAR))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/reservas/RES-2026-00001")))
                .andExpect(jsonPath("$.codigo").value("RES-2026-00001"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.valorTotal").value(200000))
                .andExpect(jsonPath("$.moneda").value("COP"))
                .andExpect(jsonPath("$.desglose.length()").value(2))
                .andExpect(jsonPath("$.versionPolitica").value(2));

        ArgumentCaptor<LocalDateTime> ahora = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(crear).ejecutar(any(), any(), any(), any(), any(), any(), ahora.capture());
        assertEquals(LocalDateTime.of(2026, 11, 2, 10, 0), ahora.getValue());
    }

    @Test
    void crearConDatosFaltantesResponde400ConErroresPorCampo() throws Exception {
        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errores.identificacionApartamento").exists())
                .andExpect(jsonPath("$.errores.fechaEntrada").exists())
                .andExpect(jsonPath("$.errores.ocupantes").exists());
    }

    @Test
    void crearConJsonMalFormadoResponde400SinDetallesTecnicos() throws Exception {
        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content("{no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", not(containsString("Exception"))));
    }

    @Test
    void crearConCanalInexistenteResponde400() throws Exception {
        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content(CREAR.replace("PORTAL", "TELEPATIA")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unaReglaDeNegocioViolada409ConElMensajeDelDominio() throws Exception {
        when(crear.ejecutar(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new ReglaDominioException("El apartamento ya tiene una reserva activa que solapa esas noches"));

        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content(CREAR))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.mensaje").value("El apartamento ya tiene una reserva activa que solapa esas noches"))
                .andExpect(jsonPath("$.path").value("/api/reservas"));
    }

    @Test
    void unErrorInesperadoResponde500SinFiltrarLaTraza() throws Exception {
        when(crear.ejecutar(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("jdbc:h2:mem:sgadb password=secreto"));

        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content(CREAR))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.mensaje").value("Error interno del servidor"))
                .andExpect(jsonPath("$.mensaje", not(containsString("secreto"))));
    }

    @Test
    void consultarPorCodigoResponde200() throws Exception {
        when(consultar.ejecutar(codigo)).thenReturn(reserva());

        mvc.perform(get("/api/reservas/RES-2026-00001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("RES-2026-00001"))
                .andExpect(jsonPath("$.titular.documento").value("CC-100"))
                .andExpect(jsonPath("$.historial[0].accion").value("CREACION"));
    }

    @Test
    void consultarUnaReservaInexistenteResponde404() throws Exception {
        when(consultar.ejecutar(codigo)).thenThrow(new ReservaNoEncontradaException(codigo));

        mvc.perform(get("/api/reservas/RES-2026-00001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensaje").value("No se encontró la reserva con código: RES-2026-00001"));
    }

    @Test
    void listarPaginaConFiltrosYResponde200() throws Exception {
        when(listar.ejecutar(any(FiltroReservas.class), eq(new SolicitudPagina(1))))
                .thenReturn(new Pagina<>(List.of(reserva()), 1, 11));

        mvc.perform(get("/api/reservas").param("estado", "PENDIENTE").param("canal", "PORTAL")
                        .param("apartamento", "apt-101").param("entradaDesde", "2026-12-01").param("pagina", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.tamanio").value(10))
                .andExpect(jsonPath("$.totalElementos").value(11))
                .andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.contenido[0].codigo").value("RES-2026-00001"))
                .andExpect(jsonPath("$.contenido[0].nombreTitular").value("Juan"));

        ArgumentCaptor<FiltroReservas> filtro = ArgumentCaptor.forClass(FiltroReservas.class);
        verify(listar).ejecutar(filtro.capture(), any());
        assertEquals(EstadoReserva.PENDIENTE, filtro.getValue().estado());
        assertEquals(new IdentificacionApartamento("APT-101"), filtro.getValue().apartamento());
        assertEquals(LocalDate.of(2026, 12, 1), filtro.getValue().entradaDesde());
    }

    @Test
    void listarConPaginaNegativaOEstadoInexistenteResponde400() throws Exception {
        mvc.perform(get("/api/reservas").param("pagina", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/reservas").param("estado", "VOLANDO")).andExpect(status().isBadRequest());
    }

    @Test
    void confirmarResponde200YLeEnviaLaHoraDeLlegadaYElMomentoDelServidor() throws Exception {
        Reserva confirmada = reserva();
        confirmada.indicarHoraEstimadaLlegada(java.time.LocalTime.of(16, 0), "Juan", LocalDateTime.of(2026, 11, 2, 10, 0));
        confirmada.confirmar("Juan", LocalDateTime.of(2026, 11, 2, 10, 0));
        when(confirmar.ejecutar(eq(codigo), eq(java.time.LocalTime.of(16, 0)), eq("Juan"),
                eq(LocalDateTime.of(2026, 11, 2, 10, 0)))).thenReturn(confirmada);

        mvc.perform(put("/api/reservas/RES-2026-00001/confirmar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"horaEstimadaLlegada\":\"16:00\",\"autor\":\"Juan\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.horaEstimadaLlegada").value("16:00:00"));
    }

    @Test
    void confirmarSinAnticipoResponde409() throws Exception {
        when(confirmar.ejecutar(any(), any(), any(), any())).thenThrow(new ReglaDominioException(
                "Para confirmar la reserva debe registrarse un anticipo de 60000 COP (30 % del valor)"));

        mvc.perform(put("/api/reservas/RES-2026-00001/confirmar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autor\":\"Juan\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", containsString("anticipo")));
    }

    @Test
    void confirmarSinAutorResponde400() throws Exception {
        mvc.perform(put("/api/reservas/RES-2026-00001/confirmar").contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.autor").exists());
    }

    @Test
    void cancelarResponde200() throws Exception {
        Reserva cancelada = reserva();
        cancelada.cancelar("Cambio de planes", "Juan", LocalDateTime.of(2026, 11, 2, 10, 0));
        when(cancelar.ejecutar(eq(codigo), eq("Cambio de planes"), eq("Juan"), any())).thenReturn(cancelada);

        mvc.perform(put("/api/reservas/RES-2026-00001/cancelar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Cambio de planes\",\"autor\":\"Juan\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
    }

    @Test
    void cancelarSinMotivoResponde400() throws Exception {
        mvc.perform(put("/api/reservas/RES-2026-00001/cancelar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autor\":\"Juan\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.motivo").exists());
    }

    @Test
    void cancelarUnaReservaQueNoSePuedeCancelarResponde409() throws Exception {
        when(cancelar.ejecutar(any(), any(), any(), any()))
                .thenThrow(new ReglaDominioException("No se puede pasar de EN_CURSO a CANCELADA"));

        mvc.perform(put("/api/reservas/RES-2026-00001/cancelar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"x\",\"autor\":\"Juan\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void modificarResponde200() throws Exception {
        when(modificar.ejecutar(eq(codigo), isNull(), any(), any(), eq("Juan"), any())).thenReturn(reserva());

        mvc.perform(put("/api/reservas/RES-2026-00001/modificar").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fechaEntrada":"2026-12-10","fechaSalida":"2026-12-13","autor":"Juan",
                                 "ocupantes":[{"documento":"CC-100","nombre":"Juan","fechaNacimiento":"1990-01-01"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("RES-2026-00001"));
    }

    @Test
    void modificarConNuevoApartamentoSeLoEntregaAlCasoDeUso() throws Exception {
        when(modificar.ejecutar(eq(codigo), eq(new co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento("APT-202")),
                any(), any(), eq("Juan"), any())).thenReturn(reserva());

        mvc.perform(put("/api/reservas/RES-2026-00001/modificar").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identificacionApartamento":"APT-202","fechaEntrada":"2026-12-10","fechaSalida":"2026-12-13",
                                 "autor":"Juan",
                                 "ocupantes":[{"documento":"CC-100","nombre":"Juan","fechaNacimiento":"1990-01-01"}]}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void modificarConApartamentoEnBlancoLoTrataComoSinCambio() throws Exception {
        when(modificar.ejecutar(eq(codigo), isNull(), any(), any(), eq("Juan"), any())).thenReturn(reserva());

        mvc.perform(put("/api/reservas/RES-2026-00001/modificar").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identificacionApartamento":"  ","fechaEntrada":"2026-12-10","fechaSalida":"2026-12-13",
                                 "autor":"Juan",
                                 "ocupantes":[{"documento":"CC-100","nombre":"Juan","fechaNacimiento":"1990-01-01"}]}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void registrarLlegadaResponde200YLeEnviaElMomentoDelServidor() throws Exception {
        Reserva enCurso = reserva();
        enCurso.indicarHoraEstimadaLlegada(java.time.LocalTime.of(16, 0), "Juan", LocalDateTime.of(2026, 11, 2, 10, 0));
        enCurso.confirmar("Juan", LocalDateTime.of(2026, 11, 2, 10, 0));
        enCurso.registrarLlegada("Recepción", LocalDateTime.of(2026, 12, 10, 16, 0));
        when(llegada.ejecutar(eq(codigo), eq("Recepción"), eq(LocalDateTime.of(2026, 11, 2, 10, 0))))
                .thenReturn(enCurso);

        mvc.perform(put("/api/reservas/RES-2026-00001/registrar-llegada").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autor\":\"Recepción\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_CURSO"));
    }

    @Test
    void registrarLlegadaConElApartamentoSinPrepararResponde409() throws Exception {
        when(llegada.ejecutar(any(), any(), any()))
                .thenThrow(new ReglaDominioException("No se puede pasar de PENDIENTE_PREPARACION a OCUPADO"));

        mvc.perform(put("/api/reservas/RES-2026-00001/registrar-llegada").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autor\":\"Recepción\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", containsString("OCUPADO")));
    }

    @Test
    void registrarSalidaSinFolioCerradoResponde409() throws Exception {
        when(salida.ejecutar(any(), any(), any())).thenThrow(new ReglaDominioException(
                "No se puede registrar la salida: el folio debe cerrarse primero"));

        mvc.perform(put("/api/reservas/RES-2026-00001/registrar-salida").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autor\":\"Recepción\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void declararNoShowResponde200() throws Exception {
        Reserva noShowReserva = reserva();
        noShowReserva.indicarHoraEstimadaLlegada(java.time.LocalTime.of(16, 0), "Juan", LocalDateTime.of(2026, 11, 2, 10, 0));
        noShowReserva.confirmar("Juan", LocalDateTime.of(2026, 11, 2, 10, 0));
        noShowReserva.declararNoShow(java.time.LocalTime.of(22, 0), "Sistema", LocalDateTime.of(2026, 12, 10, 22, 30));
        when(noShow.ejecutar(eq(codigo), eq("Sistema"), any())).thenReturn(noShowReserva);

        mvc.perform(put("/api/reservas/RES-2026-00001/declarar-no-show").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autor\":\"Sistema\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("NO_SHOW"));
    }

    @Test
    void lasOperacionesDeLaReservaSinAutorResponden400() throws Exception {
        for (String operacion : new String[] {"registrar-llegada", "registrar-salida", "declarar-no-show"}) {
            mvc.perform(put("/api/reservas/RES-2026-00001/" + operacion).contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores.autor").exists());
        }
    }

    @Test
    void noExisteUnEndpointGenericoDeCambioDeEstado() throws Exception {
        mvc.perform(put("/api/reservas/RES-2026-00001/estado").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"FINALIZADA\"}"))
                .andExpect(status().isNotFound());
    }
}
