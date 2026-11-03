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

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.application.exception.FolioNoEncontradoException;
import co.edu.uniquindio.sga.application.usecase.ApartamentoDisponible;
import co.edu.uniquindio.sga.application.usecase.CerrarFolioUseCase;
import co.edu.uniquindio.sga.application.usecase.ConsultarDisponibilidadUseCase;
import co.edu.uniquindio.sga.application.usecase.ConsultarFolioUseCase;
import co.edu.uniquindio.sga.application.usecase.CotizarEstanciaUseCase;
import co.edu.uniquindio.sga.application.usecase.ListarBloqueosUseCase;
import co.edu.uniquindio.sga.application.usecase.RegistrarBloqueoUseCase;
import co.edu.uniquindio.sga.application.usecase.RegistrarPagoUseCase;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Bloqueo;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.valueobject.*;
import co.edu.uniquindio.sga.infrastructure.rest.exception.GlobalExceptionHandler;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.BloqueoMapperImpl;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.FolioMapperImpl;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.ReservaMapperImpl;
import co.edu.uniquindio.sga.infrastructure.rest.mapper.SolicitudMapperImpl;

/** Contrato HTTP de cotización (CU-01), disponibilidad (CU-02), bloqueos (CU-09) y folio. */
@ExtendWith(MockitoExtension.class)
class ConsultasYBloqueosControllerTest {

    @Mock private CotizarEstanciaUseCase cotizar;
    @Mock private ConsultarDisponibilidadUseCase disponibilidad;
    @Mock private RegistrarBloqueoUseCase registrarBloqueo;
    @Mock private ListarBloqueosUseCase listarBloqueos;
    @Mock private ConsultarFolioUseCase consultarFolio;
    @Mock private RegistrarPagoUseCase registrarPago;
    @Mock private CerrarFolioUseCase cerrarFolio;

    private MockMvc mvc;
    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-02T15:00:00Z"), ZoneId.of("America/Bogota"));
    private final IdentificacionApartamento apt = new IdentificacionApartamento("APT-101");

    private static final String GRUPO = """
            "ocupantes":[{"documento":"CC-100","nombre":"Juan","fechaNacimiento":"1990-01-01"}]""";

    private ValorCongelado valor() {
        return new ValorCongelado(List.of(
                new CargoNoche(LocalDate.of(2026, 12, 10), "ALTA", Dinero.de(100_000), 1),
                new CargoNoche(LocalDate.of(2026, 12, 11), "ALTA", Dinero.de(100_000), 1)));
    }

    @BeforeEach
    void setUp() {
        var reservaMapper = new ReservaMapperImpl();
        var solicitud = new SolicitudMapperImpl();
        mvc = MockMvcBuilders.standaloneSetup(
                        new CotizacionController(cotizar, reservaMapper, solicitud, reloj),
                        new DisponibilidadController(disponibilidad, reservaMapper, solicitud, reloj),
                        new BloqueoController(registrarBloqueo, listarBloqueos, new BloqueoMapperImpl(), solicitud),
                        new FolioController(consultarFolio, registrarPago, cerrarFolio, new FolioMapperImpl(), solicitud))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    // ---------- CU-01 ----------

    @Test
    void cotizarResponde200ConElDesgloseNochePorNoche() throws Exception {
        when(cotizar.ejecutar(eq(apt), any(), any())).thenReturn(valor());

        mvc.perform(post("/api/cotizaciones").contentType(MediaType.APPLICATION_JSON).content("""
                        {"identificacionApartamento":"APT-101","fechaEntrada":"2026-12-10","fechaSalida":"2026-12-12",
                         %s}""".formatted(GRUPO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noches").value(2))
                .andExpect(jsonPath("$.total").value(200000))
                .andExpect(jsonPath("$.moneda").value("COP"))
                .andExpect(jsonPath("$.desglose[0].temporada").value("ALTA"))
                .andExpect(jsonPath("$.desglose[0].subtotal").value(100000));
    }

    @Test
    void cotizarSinGrupoResponde400() throws Exception {
        mvc.perform(post("/api/cotizaciones").contentType(MediaType.APPLICATION_JSON).content("""
                        {"identificacionApartamento":"APT-101","fechaEntrada":"2026-12-10","fechaSalida":"2026-12-12",
                         "ocupantes":[]}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.ocupantes").exists());
    }

    @Test
    void cotizarConFechasInvalidasParaElNegocioResponde409() throws Exception {
        // salida igual a la entrada: la forma es válida (400 no aplica), pero Estancia lo rechaza (RN-03)
        mvc.perform(post("/api/cotizaciones").contentType(MediaType.APPLICATION_JSON).content("""
                        {"identificacionApartamento":"APT-101","fechaEntrada":"2026-12-10","fechaSalida":"2026-12-10",
                         %s}""".formatted(GRUPO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("La fecha de salida debe ser posterior a la fecha de entrada"));
    }

    @Test
    void cotizarUnApartamentoInexistenteResponde404() throws Exception {
        when(cotizar.ejecutar(eq(apt), any(), any())).thenThrow(new ApartamentoNoEncontradoException(apt));

        mvc.perform(post("/api/cotizaciones").contentType(MediaType.APPLICATION_JSON).content("""
                        {"identificacionApartamento":"APT-101","fechaEntrada":"2026-12-10","fechaSalida":"2026-12-12",
                         %s}""".formatted(GRUPO)))
                .andExpect(status().isNotFound());
    }

    // ---------- CU-02 ----------

    @Test
    void consultarDisponibilidadResponde200Paginado() throws Exception {
        var libre = new ApartamentoDisponible(new Apartamento(apt, "Vista", 2, 4), valor());
        when(disponibilidad.ejecutar(any(), any(), eq(2), any(), any(), eq(new SolicitudPagina(0))))
                .thenReturn(new Pagina<>(List.of(libre), 0, 1));

        mvc.perform(post("/api/disponibilidad").contentType(MediaType.APPLICATION_JSON).content("""
                        {"fechaEntrada":"2026-12-10","fechaSalida":"2026-12-12","capacidadMinima":2,%s}"""
                        .formatted(GRUPO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamanio").value(10))
                .andExpect(jsonPath("$.contenido[0].identificacion").value("APT-101"))
                .andExpect(jsonPath("$.contenido[0].capacidad").value(4))
                .andExpect(jsonPath("$.contenido[0].total").value(200000));
    }

    @Test
    void consultarDisponibilidadConPrecioNegativoResponde400() throws Exception {
        mvc.perform(post("/api/disponibilidad").contentType(MediaType.APPLICATION_JSON).content("""
                        {"fechaEntrada":"2026-12-10","fechaSalida":"2026-12-12","precioMinimo":-5,%s}"""
                        .formatted(GRUPO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.precioMinimo").exists());
    }

    @Test
    void consultarDisponibilidadConPaginaNegativaResponde400ConElParametro() throws Exception {
        mvc.perform(post("/api/disponibilidad").param("pagina", "-1").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fechaEntrada":"2026-12-10","fechaSalida":"2026-12-12",%s}""".formatted(GRUPO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.pagina").value("La página no puede ser negativa"));
    }

    // ---------- CU-09 ----------

    @Test
    void registrarBloqueoResponde201ConLocation() throws Exception {
        when(registrarBloqueo.ejecutar(eq(apt), any(), eq("Mantenimiento")))
                .thenReturn(new Bloqueo(apt, new Estancia(LocalDate.of(2026, 12, 20), LocalDate.of(2026, 12, 23)),
                        "Mantenimiento"));

        mvc.perform(post("/api/apartamentos/APT-101/bloqueos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaInicio\":\"2026-12-20\",\"fechaFin\":\"2026-12-23\",\"motivo\":\"Mantenimiento\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/apartamentos/APT-101/bloqueos")))
                .andExpect(jsonPath("$.identificacionApartamento").value("APT-101"))
                .andExpect(jsonPath("$.fechaInicio").value("2026-12-20"))
                .andExpect(jsonPath("$.motivo").value("Mantenimiento"));
    }

    @Test
    void registrarBloqueoSinMotivoResponde400() throws Exception {
        mvc.perform(post("/api/apartamentos/APT-101/bloqueos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaInicio\":\"2026-12-20\",\"fechaFin\":\"2026-12-23\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.motivo").exists());
    }

    @Test
    void registrarBloqueoSobreNochesConReservaActivaResponde409() throws Exception {
        when(registrarBloqueo.ejecutar(any(), any(), any())).thenThrow(new ReglaDominioException(
                "No se puede bloquear: el apartamento tiene la reserva activa RES-2026-00001 en esas noches"));

        mvc.perform(post("/api/apartamentos/APT-101/bloqueos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaInicio\":\"2026-12-10\",\"fechaFin\":\"2026-12-12\",\"motivo\":\"x\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void registrarBloqueoEnApartamentoInexistenteResponde404() throws Exception {
        when(registrarBloqueo.ejecutar(any(), any(), any())).thenThrow(new ApartamentoNoEncontradoException(apt));

        mvc.perform(post("/api/apartamentos/APT-101/bloqueos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fechaInicio\":\"2026-12-10\",\"fechaFin\":\"2026-12-12\",\"motivo\":\"x\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listarBloqueosResponde200() throws Exception {
        var bloqueo = new Bloqueo(apt, new Estancia(LocalDate.of(2026, 12, 20), LocalDate.of(2026, 12, 23)), "Pintura");
        when(listarBloqueos.ejecutar(apt, new SolicitudPagina(0))).thenReturn(new Pagina<>(List.of(bloqueo), 0, 1));

        mvc.perform(get("/api/apartamentos/APT-101/bloqueos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].motivo").value("Pintura"))
                .andExpect(jsonPath("$.totalPaginas").value(1));
    }

    // ---------- Folio ----------

    @Test
    void consultarFolioResponde200ConSusMovimientosYSaldo() throws Exception {
        Folio folio = new Folio(new CodigoReserva("RES-2026-00001"), new Cargo("Alojamiento", Dinero.de(200_000)));
        folio.registrarPago(new Pago("TRANSFERENCIA", LocalDate.of(2026, 11, 3), Dinero.de(60_000)));
        folio.registrarAjuste(new Ajuste("Menos noches", Dinero.de(-10_000)));
        when(consultarFolio.ejecutar(new CodigoReserva("RES-2026-00001"))).thenReturn(folio);

        mvc.perform(get("/api/reservas/RES-2026-00001/folio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoReserva").value("RES-2026-00001"))
                .andExpect(jsonPath("$.cargos[0].valor").value(200000))
                .andExpect(jsonPath("$.ajustes[0].valor").value(-10000))
                .andExpect(jsonPath("$.pagos[0].medio").value("TRANSFERENCIA"))
                .andExpect(jsonPath("$.saldo").value(130000))
                .andExpect(jsonPath("$.cerrado").value(false));
    }

    @Test
    void registrarPagoResponde201ConLocationYElSaldoActualizado() throws Exception {
        CodigoReserva codigo = new CodigoReserva("RES-2026-00001");
        Folio folio = new Folio(codigo, new Cargo("Alojamiento", Dinero.de(200_000)));
        Pago pago = new Pago("TRANSFERENCIA", LocalDate.of(2026, 11, 3), Dinero.de(60_000));
        folio.registrarPago(pago);
        when(registrarPago.ejecutar(codigo, pago)).thenReturn(folio);

        mvc.perform(post("/api/reservas/RES-2026-00001/folio/pagos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medio\":\"TRANSFERENCIA\",\"fecha\":\"2026-11-03\",\"valor\":60000}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/reservas/RES-2026-00001/folio")))
                .andExpect(jsonPath("$.pagos[0].valor").value(60000))
                .andExpect(jsonPath("$.saldo").value(140000));
    }

    @Test
    void registrarPagoConValorNoPositivoOSinMedioResponde400() throws Exception {
        mvc.perform(post("/api/reservas/RES-2026-00001/folio/pagos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fecha\":\"2026-11-03\",\"valor\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.medio").exists())
                .andExpect(jsonPath("$.errores.valor").exists());
    }

    @Test
    void registrarPagoEnFolioCerradoResponde409() throws Exception {
        when(registrarPago.ejecutar(any(), any()))
                .thenThrow(new ReglaDominioException("Un folio cerrado no admite movimientos"));

        mvc.perform(post("/api/reservas/RES-2026-00001/folio/pagos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medio\":\"EFECTIVO\",\"fecha\":\"2026-11-03\",\"valor\":1000}"))
                .andExpect(status().isConflict());
    }

    @Test
    void cerrarFolioConAutorizacionRegistraQuienYPorQueCU11() throws Exception {
        CodigoReserva codigo = new CodigoReserva("RES-2026-00001");
        Folio cerrado = new Folio(codigo, new Cargo("Alojamiento", Dinero.de(200_000)));
        cerrado.cerrar("Autorizado por Admin: pago por transferencia");
        when(cerrarFolio.ejecutar(codigo, "Autorizado por Admin: pago por transferencia")).thenReturn(cerrado);

        mvc.perform(put("/api/reservas/RES-2026-00001/folio/cerrar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autorizadoPor\":\"Admin\",\"motivo\":\"pago por transferencia\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cerrado").value(true))
                .andExpect(jsonPath("$.autorizacionCierre").value("Autorizado por Admin: pago por transferencia"));
    }

    @Test
    void cerrarFolioSinAutorizacionLlegaAlDominioConNullYSiHaySaldoResponde409() throws Exception {
        CodigoReserva codigo = new CodigoReserva("RES-2026-00001");
        when(cerrarFolio.ejecutar(codigo, null)).thenThrow(new ReglaDominioException(
                "No se puede cerrar un folio con saldo pendiente sin autorización registrada"));

        // falta el motivo: la autorización queda incompleta y el dominio decide
        mvc.perform(put("/api/reservas/RES-2026-00001/folio/cerrar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autorizadoPor\":\"Admin\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void cerrarFolioInexistenteResponde404() throws Exception {
        CodigoReserva codigo = new CodigoReserva("RES-2026-00009");
        when(cerrarFolio.ejecutar(codigo, null)).thenThrow(new FolioNoEncontradoException(codigo));

        mvc.perform(put("/api/reservas/RES-2026-00009/folio/cerrar").contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void consultarFolioInexistenteResponde404() throws Exception {
        CodigoReserva codigo = new CodigoReserva("RES-2026-00009");
        when(consultarFolio.ejecutar(codigo)).thenThrow(new FolioNoEncontradoException(codigo));

        mvc.perform(get("/api/reservas/RES-2026-00009/folio")).andExpect(status().isNotFound());
    }
}
