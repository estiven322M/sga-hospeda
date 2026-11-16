package co.edu.uniquindio.sga.application.usecase;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import co.edu.uniquindio.sga.application.security.DirectorioCorreos;
import co.edu.uniquindio.sga.domain.entity.Ocupante;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.NotificacionFallidaException;
import co.edu.uniquindio.sga.domain.repository.BandejaNotificaciones;
import co.edu.uniquindio.sga.domain.repository.ContactoReservaRepository;
import co.edu.uniquindio.sga.domain.repository.Notificador;
import co.edu.uniquindio.sga.domain.service.RedactorNotificaciones;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.CargoNoche;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.CorreoElectronico;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.DocumentoIdentidad;
import co.edu.uniquindio.sga.domain.valueobject.EstadoNotificacion;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Notificacion;
import co.edu.uniquindio.sga.domain.valueobject.RegistroNotificacion;
import co.edu.uniquindio.sga.domain.valueobject.TipoNotificacion;
import co.edu.uniquindio.sga.domain.valueobject.ValorCongelado;
import co.edu.uniquindio.sga.domain.valueobject.VersionPolitica;
import co.edu.uniquindio.sga.testsupport.ReservaDePrueba;

/**
 * 10.3.2-A, I-04 e I-05: avisar al titular NUNCA puede impedir reservar. Se prueba que el fallo del proveedor, de la
 * bandeja o de la búsqueda del correo no sale de los casos de uso, y a quién se escribe en cada caso.
 */
class NotificacionesUseCasesTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 16, 10, 0);
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-16T15:00:00Z"), ZoneId.of("America/Bogota"));
    private static final CorreoElectronico CUENTA = new CorreoElectronico("laura@cuenta.com");
    private static final CorreoElectronico CONTACTO = new CorreoElectronico("laura@contacto.com");

    private final Notificador notificador = mock(Notificador.class);
    private final BandejaNotificaciones bandeja = mock(BandejaNotificaciones.class);
    private final ContactoReservaRepository contactos = mock(ContactoReservaRepository.class);
    private final DirectorioCorreos directorio = mock(DirectorioCorreos.class);
    private final EmitirNotificacionUseCase emitir = new EmitirNotificacionUseCase(notificador, bandeja, RELOJ);
    private final NotificarReservaUseCase notificar = new NotificarReservaUseCase(
            new RedactorNotificaciones(() -> "Hospeda"), contactos, directorio, emitir);

    private Reserva reserva() {
        Ocupante titular = new Ocupante(new DocumentoIdentidad("1094000111"), "Laura Gomez",
                LocalDate.of(1995, 1, 30), LocalDate.of(2026, 11, 16));
        return Reserva.crear(new CodigoReserva("RES-2026-00007"), new IdentificacionApartamento("APT-101"),
                new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12)), titular, List.of(titular),
                CanalOrigen.DIRECTO, new ValorCongelado(List.of(
                        new CargoNoche(LocalDate.of(2026, 12, 10), "ALTA", Dinero.de(100_000), 1),
                        new CargoNoche(LocalDate.of(2026, 12, 11), "ALTA", Dinero.de(100_000), 1))),
                new VersionPolitica(1), AHORA, ReservaDePrueba.EDAD_ADULTO);
    }

    private Notificacion enviada() {
        ArgumentCaptor<Notificacion> captura = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificador).enviar(captura.capture());
        return captura.getValue();
    }

    // ---------- EmitirNotificacionUseCase ----------

    @Test
    void conDestinatarioEnviaPorElPuerto() {
        Notificacion aviso = new Notificacion(TipoNotificacion.RESERVA_CREADA, CUENTA, "a", "m", null, AHORA);

        emitir.ejecutar(aviso);

        verify(notificador).enviar(aviso);
    }

    @Test
    void sinDestinatarioNoLlamaAlProveedorYDejaConstanciaSinCorreo() {
        Notificacion aviso = new Notificacion(TipoNotificacion.RESERVA_CREADA, null, "a", "m", null, AHORA);

        emitir.ejecutar(aviso);

        verify(notificador, never()).enviar(any());
        ArgumentCaptor<RegistroNotificacion> registro = ArgumentCaptor.forClass(RegistroNotificacion.class);
        verify(bandeja).registrar(registro.capture());
        assertEquals(EstadoNotificacion.SIN_CORREO, registro.getValue().estado());
        assertNull(registro.getValue().destinatario());
        assertEquals(LocalDateTime.of(2026, 11, 16, 10, 0), registro.getValue().registradaEn());
    }

    @Test
    void unAvisoSensibleSinCorreoNoDejaElCodigoEnLaBandeja() {
        emitir.ejecutar(new Notificacion(TipoNotificacion.CODIGO_RECUPERACION, null, "a", "Su código es 123456", null, AHORA));

        ArgumentCaptor<RegistroNotificacion> registro = ArgumentCaptor.forClass(RegistroNotificacion.class);
        verify(bandeja).registrar(registro.capture());
        assertEquals(Notificacion.CONTENIDO_OMITIDO, registro.getValue().mensaje());
    }

    @Test
    void siElProveedorFallaLaExcepcionNoSale() {
        Notificacion aviso = new Notificacion(TipoNotificacion.RESERVA_CREADA, CUENTA, "a", "m", null, AHORA);
        doThrow(new NotificacionFallidaException("tiempo agotado")).when(notificador).enviar(aviso);

        assertDoesNotThrow(() -> emitir.ejecutar(aviso));
    }

    @Test
    void ningunErrorInesperadoDelProveedorSale() {
        Notificacion aviso = new Notificacion(TipoNotificacion.RESERVA_CREADA, CUENTA, "a", "m", null, AHORA);
        doThrow(new IllegalStateException("boom")).when(notificador).enviar(aviso);

        assertDoesNotThrow(() -> emitir.ejecutar(aviso));
    }

    @Test
    void siFallaLaBandejaTampocoSaleLaExcepcion() {
        doThrow(new IllegalStateException("bandeja caída")).when(bandeja).registrar(any());

        assertDoesNotThrow(() -> emitir.ejecutar(new Notificacion(TipoNotificacion.RESERVA_CREADA, null, "a", "m", null, AHORA)));
    }

    // ---------- NotificarReservaUseCase ----------

    @Test
    void siElTitularTieneCuentaSeUsaSuCorreoDeCuenta() {
        when(directorio.correoDeCuenta(new DocumentoIdentidad("1094000111"))).thenReturn(Optional.of(CUENTA));
        when(contactos.buscar(any())).thenReturn(Optional.of(CONTACTO));

        notificar.reservaCreada(reserva(), null, AHORA);

        assertEquals(CUENTA, enviada().destinatario());
        assertEquals(TipoNotificacion.RESERVA_CREADA, enviada().tipo());
    }

    @Test
    void sinCuentaSeUsaElCorreoDeContactoDadoAlReservar() {
        when(directorio.correoDeCuenta(any())).thenReturn(Optional.empty());
        when(contactos.buscar(new CodigoReserva("RES-2026-00007"))).thenReturn(Optional.of(CONTACTO));

        notificar.reservaCreada(reserva(), "Laura@Contacto.com", AHORA);

        verify(contactos).guardar(new CodigoReserva("RES-2026-00007"), CONTACTO);
        assertEquals(CONTACTO, enviada().destinatario());
    }

    @Test
    void sinCuentaNiCorreoDeContactoQuedaRegistradoQueNoSePudoNotificar() {
        when(directorio.correoDeCuenta(any())).thenReturn(Optional.empty());
        when(contactos.buscar(any())).thenReturn(Optional.empty());

        notificar.reservaCreada(reserva(), null, AHORA);

        verify(notificador, never()).enviar(any());
        ArgumentCaptor<RegistroNotificacion> registro = ArgumentCaptor.forClass(RegistroNotificacion.class);
        verify(bandeja).registrar(registro.capture());
        assertEquals(EstadoNotificacion.SIN_CORREO, registro.getValue().estado());
        assertEquals("RES-2026-00007", registro.getValue().codigoReserva().valor());
    }

    @Test
    void laConfirmacionYLaCancelacionUsanSuPropioTipo() {
        when(directorio.correoDeCuenta(any())).thenReturn(Optional.of(CUENTA));

        notificar.reservaConfirmada(reserva(), AHORA);
        notificar.reservaCancelada(reserva(), AHORA);

        ArgumentCaptor<Notificacion> captura = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificador, org.mockito.Mockito.times(2)).enviar(captura.capture());
        assertEquals(List.of(TipoNotificacion.RESERVA_CONFIRMADA, TipoNotificacion.RESERVA_CANCELADA),
                captura.getAllValues().stream().map(Notificacion::tipo).toList());
    }

    @Test
    void unCorreoDeContactoMalEscritoSeIgnoraYLaReservaSigueSinFallar() {
        when(directorio.correoDeCuenta(any())).thenReturn(Optional.of(CUENTA));

        assertDoesNotThrow(() -> notificar.reservaCreada(reserva(), "esto-no-es-un-correo", AHORA));

        verify(contactos, never()).guardar(any(), any());
        assertEquals(CUENTA, enviada().destinatario());
    }

    @Test
    void siFallaLaBusquedaDelCorreoLaOperacionNoSeAfecta() {
        when(directorio.correoDeCuenta(any())).thenThrow(new IllegalStateException("base caída"));

        assertDoesNotThrow(() -> notificar.reservaCreada(reserva(), null, AHORA));
        assertDoesNotThrow(() -> notificar.reservaConfirmada(reserva(), AHORA));
        assertDoesNotThrow(() -> notificar.reservaCancelada(reserva(), AHORA));
        verify(notificador, never()).enviar(any());
    }

    @Test
    void siNoSePuedeGuardarElContactoIgualSeNotifica() {
        when(directorio.correoDeCuenta(any())).thenReturn(Optional.of(CUENTA));
        doThrow(new IllegalStateException("sin espacio")).when(contactos).guardar(any(), eq(CONTACTO));

        assertDoesNotThrow(() -> notificar.reservaCreada(reserva(), "laura@contacto.com", AHORA));

        verify(notificador).enviar(any());
    }

    @Test
    void siElProveedorFallaLaReservaSigueSinExcepcion() {
        when(directorio.correoDeCuenta(any())).thenReturn(Optional.of(CUENTA));
        doThrow(new NotificacionFallidaException("tiempo agotado")).when(notificador).enviar(any());

        assertDoesNotThrow(() -> notificar.reservaCreada(reserva(), null, AHORA));
    }
}
