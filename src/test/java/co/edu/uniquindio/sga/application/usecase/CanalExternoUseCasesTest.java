package co.edu.uniquindio.sga.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.application.exception.CanalNoEncontradoException;
import co.edu.uniquindio.sga.application.exception.RechazoNoEncontradoException;
import co.edu.uniquindio.sga.application.exception.ReservaExternaNoEncontradaException;
import co.edu.uniquindio.sga.domain.entity.CanalVenta;
import co.edu.uniquindio.sga.domain.entity.Ocupante;
import co.edu.uniquindio.sga.domain.entity.RechazoReservaExterna;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.MensajeRepetidoException;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.CanalVentaRepository;
import co.edu.uniquindio.sga.domain.repository.RechazoReservaExternaRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.service.RegistroCanalVentaService;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.CargoNoche;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.CredencialCanal;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.DocumentoIdentidad;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.IdentificadorExterno;
import co.edu.uniquindio.sga.domain.valueobject.OperacionCanal;
import co.edu.uniquindio.sga.domain.valueobject.ResultadoCanal;
import co.edu.uniquindio.sga.domain.valueobject.TipoCanalVenta;
import co.edu.uniquindio.sga.domain.valueobject.ValorCongelado;
import co.edu.uniquindio.sga.domain.valueobject.VersionPolitica;
import co.edu.uniquindio.sga.testsupport.ReservaDePrueba;

/**
 * 10.3.1 y 7.8 en los casos de uso: qué queda en la bitácora (con qué resultado y código HTTP) en cada desenlace, que el mensaje repetido
 * no cree otra reserva, que un canal solo cancele las suyas (idempotente), que se avise al titular DESPUÉS de crear o cancelar y que
 * resolver un conflicto no toque ninguna reserva.
 */
class CanalExternoUseCasesTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2027, 3, 10, 10, 0);
    private static final IdentificacionApartamento APT = new IdentificacionApartamento("APT-101");
    private static final Estancia ESTANCIA = new Estancia(LocalDate.of(2027, 9, 6), LocalDate.of(2027, 9, 9));

    private final CanalVenta canal = new CanalVenta("DEMO", "Demo", TipoCanalVenta.PLATAFORMA_EN_LINEA, CredencialCanal.de("x").hash());
    private final CrearReservaUseCase crearReserva = mock(CrearReservaUseCase.class);
    private final CancelarReservaUseCase cancelar = mock(CancelarReservaUseCase.class);
    private final ReservaRepository reservas = mock(ReservaRepository.class);
    private final NotificarReservaUseCase notificar = mock(NotificarReservaUseCase.class);
    private final RegistrarEventoCanalUseCase bitacora = mock(RegistrarEventoCanalUseCase.class);
    private final CrearReservaExternaUseCase crear = new CrearReservaExternaUseCase(crearReserva, notificar, bitacora);
    private final CancelarReservaExternaUseCase cancelarExterna = new CancelarReservaExternaUseCase(reservas, cancelar, notificar, bitacora);

    private final Ocupante titular = new Ocupante(new DocumentoIdentidad("CC-1"), "Paula", LocalDate.of(1985, 4, 4), LocalDate.of(2027, 3, 10));

    private Reserva reserva(String identificadorGuardado) {
        return Reserva.crear(new CodigoReserva("RES-2027-00001"), APT, ESTANCIA, titular, List.of(titular), CanalOrigen.EXTERNO,
                new IdentificadorExterno(identificadorGuardado), new ValorCongelado(List.of(
                        new CargoNoche(LocalDate.of(2027, 9, 6), "BASE", Dinero.de(100_000), 1),
                        new CargoNoche(LocalDate.of(2027, 9, 7), "BASE", Dinero.de(100_000), 1),
                        new CargoNoche(LocalDate.of(2027, 9, 8), "BASE", Dinero.de(100_000), 1))),
                new VersionPolitica(1), AHORA, ReservaDePrueba.EDAD_ADULTO);
    }

    private Reserva crear() {
        return crear.ejecutar(canal, "RES-1", APT, ESTANCIA, titular, List.of(titular), null, "paula@correo.com", AHORA);
    }

    // ---------- crear ----------

    @Test
    void crearGuardaElIdentificadorConElCodigoDelCanalComoCanalExternoYRegistraACEPTADA() {
        Reserva creada = reserva("DEMO:RES-1");
        when(crearReserva.ejecutar(eq(APT), eq(ESTANCIA), eq(titular), any(), eq(CanalOrigen.EXTERNO),
                eq(new IdentificadorExterno("DEMO:RES-1")), any(), eq(AHORA))).thenReturn(creada);

        Reserva resultado = crear();

        assertEquals(creada, resultado);
        verify(bitacora).ejecutar("DEMO", OperacionCanal.CREAR_RESERVA, "RES-1", ResultadoCanal.ACEPTADA, 201, "Reserva RES-2027-00001 creada");
    }

    @Test
    void despuesDeCrearSeAvisaAlTitularConSuCorreoDeContacto() {
        Reserva creada = reserva("DEMO:RES-1");
        when(crearReserva.ejecutar(any(), any(), any(), any(), any(), any(IdentificadorExterno.class), any(), any())).thenReturn(creada);

        crear();

        verify(notificar).reservaCreada(creada, "paula@correo.com", AHORA);
    }

    @Test
    void unMensajeRepetidoSeRegistraComoREPETIDAYNoSeAvisaNiSeCreaOtra() {
        when(crearReserva.ejecutar(any(), any(), any(), any(), any(), any(IdentificadorExterno.class), any(), any()))
                .thenThrow(new MensajeRepetidoException("El mensaje ya fue recibido: ya creó la reserva RES-2027-00001"));

        assertThrows(MensajeRepetidoException.class, this::crear);

        verify(bitacora).ejecutar("DEMO", OperacionCanal.CREAR_RESERVA, "RES-1", ResultadoCanal.REPETIDA, 409,
                "El mensaje ya fue recibido: ya creó la reserva RES-2027-00001");
        verify(notificar, never()).reservaCreada(any(), any(), any());
    }

    @Test
    void unConflictoSeRegistraComoRECHAZADAYSeRelanzaParaElCliente() {
        when(crearReserva.ejecutar(any(), any(), any(), any(), any(), any(IdentificadorExterno.class), any(), any()))
                .thenThrow(new ReglaDominioException("El apartamento ya tiene una reserva activa que solapa esas noches"));

        ReglaDominioException e = assertThrows(ReglaDominioException.class, this::crear);

        assertEquals("El apartamento ya tiene una reserva activa que solapa esas noches", e.getMessage());
        verify(bitacora).ejecutar("DEMO", OperacionCanal.CREAR_RESERVA, "RES-1", ResultadoCanal.RECHAZADA, 409,
                "El apartamento ya tiene una reserva activa que solapa esas noches");
        verify(notificar, never()).reservaCreada(any(), any(), any());
    }

    @Test
    void unApartamentoQueNoExisteSeRegistraComoNOENCONTRADA() {
        when(crearReserva.ejecutar(any(), any(), any(), any(), any(), any(IdentificadorExterno.class), any(), any()))
                .thenThrow(new ApartamentoNoEncontradoException(APT));

        assertThrows(ApartamentoNoEncontradoException.class, this::crear);

        verify(bitacora).ejecutar(eq("DEMO"), eq(OperacionCanal.CREAR_RESERVA), eq("RES-1"), eq(ResultadoCanal.NO_ENCONTRADA), eq(404), any());
    }

    @Test
    void unIdentificadorDemasiadoLargoParaElCanalSeRechazaYSeRegistra() {
        ReglaDominioException e = assertThrows(ReglaDominioException.class, () ->
                crear.ejecutar(canal, "x".repeat(60), APT, ESTANCIA, titular, List.of(titular), null, null, AHORA));

        assertTrue(e.getMessage().contains("no puede superar 55 caracteres"));
        verify(crearReserva, never()).ejecutar(any(), any(), any(), any(), any(), any(IdentificadorExterno.class), any(), any());
        verify(bitacora).ejecutar(eq("DEMO"), eq(OperacionCanal.CREAR_RESERVA), any(), eq(ResultadoCanal.RECHAZADA), eq(409), any());
    }

    // ---------- cancelar ----------

    @Test
    void cancelarBuscaLaReservaPorElIdentificadorDelCanalLaCancelaYAvisaAlTitular() {
        Reserva vigente = reserva("DEMO:RES-1");
        Reserva cancelada = reserva("DEMO:RES-1");
        cancelada.cancelar("Cambio de planes", "Canal DEMO", AHORA);
        when(reservas.buscarPorCanalEIdentificadorExterno(CanalOrigen.EXTERNO, new IdentificadorExterno("DEMO:RES-1"))).thenReturn(Optional.of(vigente));
        when(cancelar.ejecutar(vigente.getCodigo(), "Cambio de planes", "Canal DEMO", AHORA)).thenReturn(cancelada);

        Reserva resultado = cancelarExterna.ejecutar(canal, "RES-1", "Cambio de planes", AHORA);

        assertEquals(EstadoReserva.CANCELADA, resultado.getEstado());
        verify(bitacora).ejecutar("DEMO", OperacionCanal.CANCELAR_RESERVA, "RES-1", ResultadoCanal.ACEPTADA, 200, "Reserva RES-2027-00001 cancelada");
        verify(notificar).reservaCancelada(cancelada, AHORA);
    }

    @Test
    void sinMotivoSeUsaUnoPorOmision() {
        Reserva vigente = reserva("DEMO:RES-1");
        when(reservas.buscarPorCanalEIdentificadorExterno(any(), any())).thenReturn(Optional.of(vigente));
        when(cancelar.ejecutar(any(), any(), any(), any())).thenReturn(vigente);

        cancelarExterna.ejecutar(canal, "RES-1", "  ", AHORA);

        verify(cancelar).ejecutar(vigente.getCodigo(), "Cancelada por el canal", "Canal DEMO", AHORA);
    }

    @Test
    void cancelarDosVecesEsIdempotenteNoVuelveACobrarNiAAvisar() {
        Reserva yaCancelada = reserva("DEMO:RES-1");
        yaCancelada.cancelar("antes", "Canal DEMO", AHORA);
        when(reservas.buscarPorCanalEIdentificadorExterno(any(), any())).thenReturn(Optional.of(yaCancelada));

        Reserva resultado = cancelarExterna.ejecutar(canal, "RES-1", "otra vez", AHORA.plusHours(1));

        assertEquals(EstadoReserva.CANCELADA, resultado.getEstado());
        verify(cancelar, never()).ejecutar(any(), any(), any(), any());
        verify(notificar, never()).reservaCancelada(any(), any());
        verify(bitacora).ejecutar("DEMO", OperacionCanal.CANCELAR_RESERVA, "RES-1", ResultadoCanal.ACEPTADA, 200, "La reserva RES-2027-00001 ya estaba cancelada");
    }

    @Test
    void unCanalNoPuedeCancelarLaReservaDeOtroPorqueSeBuscaConSuPropioCodigo() {
        when(reservas.buscarPorCanalEIdentificadorExterno(CanalOrigen.EXTERNO, new IdentificadorExterno("DEMO:RES-1"))).thenReturn(Optional.empty());

        assertThrows(ReservaExternaNoEncontradaException.class, () -> cancelarExterna.ejecutar(canal, "RES-1", null, AHORA));

        verify(cancelar, never()).ejecutar(any(), any(), any(), any());
        verify(bitacora).ejecutar(eq("DEMO"), eq(OperacionCanal.CANCELAR_RESERVA), eq("RES-1"), eq(ResultadoCanal.NO_ENCONTRADA), eq(404), any());
    }

    @Test
    void siElDominioNoPermiteCancelarSeRegistraRECHAZADA() {
        Reserva vigente = reserva("DEMO:RES-1");
        when(reservas.buscarPorCanalEIdentificadorExterno(any(), any())).thenReturn(Optional.of(vigente));
        when(cancelar.ejecutar(any(), any(), any(), any())).thenThrow(new ReglaDominioException("Solo se cancela antes del registro"));

        assertThrows(ReglaDominioException.class, () -> cancelarExterna.ejecutar(canal, "RES-1", null, AHORA));

        verify(bitacora).ejecutar("DEMO", OperacionCanal.CANCELAR_RESERVA, "RES-1", ResultadoCanal.RECHAZADA, 409, "Solo se cancela antes del registro");
        verify(notificar, never()).reservaCancelada(any(), any());
    }

    // ---------- resolver, canales y autenticación ----------

    @Test
    void resolverSoloEscribeLaRevisionDelRechazoYNoRecibeNingunRepositorioDeReservas() {
        RechazoReservaExternaRepository rechazos = mock(RechazoReservaExternaRepository.class);
        RechazoReservaExterna rechazo = new RechazoReservaExterna(CanalOrigen.EXTERNO, new IdentificadorExterno("DEMO:RES-2"), APT, ESTANCIA, "Solapa", AHORA);
        when(rechazos.buscarPorId(3)).thenReturn(Optional.of(rechazo));
        when(rechazos.guardar(rechazo)).thenReturn(rechazo);

        RechazoReservaExterna resuelto = new ResolverRechazoReservaExternaUseCase(rechazos).ejecutar(3, "Revisado con el canal", "Admin", AHORA);

        assertTrue(resuelto.estaRevisado());
        verify(rechazos).guardar(rechazo);
    }

    @Test
    void resolverUnConflictoQueNoExisteEsUn404() {
        RechazoReservaExternaRepository rechazos = mock(RechazoReservaExternaRepository.class);
        when(rechazos.buscarPorId(9)).thenReturn(Optional.empty());

        assertThrows(RechazoNoEncontradoException.class, () -> new ResolverRechazoReservaExternaUseCase(rechazos).ejecutar(9, "n", "Admin", AHORA));
    }

    @Test
    void crearUnCanalDevuelveLaCredencialEnClaroUnaVezYGuardaSoloElHash() {
        CanalVentaRepository canales = mock(CanalVentaRepository.class);
        when(canales.buscarPorCodigo("NUEVO")).thenReturn(Optional.empty());
        when(canales.guardar(any(CanalVenta.class))).thenAnswer(i -> i.getArgument(0));

        var resultado = new CrearCanalVentaUseCase(canales, new RegistroCanalVentaService(canales)).ejecutar("nuevo", "Nuevo", TipoCanalVenta.OTRO);

        assertTrue(resultado.credencial().startsWith(CredencialCanal.PREFIJO));
        assertEquals(CredencialCanal.hashDe(resultado.credencial()), resultado.canal().getCredencialHash());
        assertFalse(resultado.canal().getCredencialHash().contains(resultado.credencial()));
    }

    @Test
    void autenticarEncuentraAlCanalPorElHashYUnaCredencialVaciaNoEsDeNadie() {
        CanalVentaRepository canales = mock(CanalVentaRepository.class);
        when(canales.buscarPorCredencialHash(CredencialCanal.hashDe("sgc_buena"))).thenReturn(Optional.of(canal));
        AutenticarCanalUseCase autenticar = new AutenticarCanalUseCase(canales);

        assertEquals(Optional.of(canal), autenticar.ejecutar("sgc_buena"));
        assertEquals(Optional.empty(), autenticar.ejecutar("sgc_mala"));
        assertEquals(Optional.empty(), autenticar.ejecutar(null));
        assertEquals(Optional.empty(), autenticar.ejecutar("  "));
        verify(canales, never()).buscarPorCredencialHash(null);
    }

    @Test
    void desactivarUnCanalQueNoExisteEsUn404() {
        CanalVentaRepository canales = mock(CanalVentaRepository.class);
        when(canales.buscarPorCodigo("NADA")).thenReturn(Optional.empty());

        assertThrows(CanalNoEncontradoException.class, () -> new CambiarActividadCanalVentaUseCase(canales).desactivar("NADA"));
    }

    @Test
    void desactivarGuardaElCanalDesactivadoSinBorrarlo() {
        CanalVentaRepository canales = mock(CanalVentaRepository.class);
        when(canales.buscarPorCodigo("DEMO")).thenReturn(Optional.of(canal));
        when(canales.guardar(canal)).thenReturn(canal);

        CanalVenta resultado = new CambiarActividadCanalVentaUseCase(canales).desactivar("DEMO");

        assertFalse(resultado.isActivo());
        verify(canales).guardar(canal);
    }
}
