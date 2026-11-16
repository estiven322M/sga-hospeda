package co.edu.uniquindio.sga.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import co.edu.uniquindio.sga.domain.entity.Ocupante;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.CargoNoche;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.CorreoElectronico;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.DocumentoIdentidad;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Notificacion;
import co.edu.uniquindio.sga.domain.valueobject.TipoNotificacion;
import co.edu.uniquindio.sga.domain.valueobject.ValorCongelado;
import co.edu.uniquindio.sga.domain.valueobject.VersionPolitica;
import co.edu.uniquindio.sga.testsupport.ReservaDePrueba;

/** 10.3.2-A: lo que se le dice al titular en cada momento de su reserva y al recuperar la contraseña. */
class RedactorNotificacionesTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 16, 10, 0);
    private static final CorreoElectronico CORREO = new CorreoElectronico("laura@correo.com");

    private String nombre = "Hospeda Salento";
    private final RedactorNotificaciones redactor = new RedactorNotificaciones(() -> nombre);

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

    @Test
    void elAvisoDeReservaCreadaLlevaElCodigoElApartamentoLasFechasYLasNoches() {
        Notificacion n = redactor.reservaCreada(reserva(), CORREO, AHORA);

        assertEquals(TipoNotificacion.RESERVA_CREADA, n.tipo());
        assertEquals(CORREO, n.destinatario());
        assertEquals("Reserva RES-2026-00007 registrada", n.asunto());
        assertTrue(n.mensaje().contains("Laura Gomez"));
        assertTrue(n.mensaje().contains("Hospeda Salento"));
        assertTrue(n.mensaje().contains("RES-2026-00007"));
        assertTrue(n.mensaje().contains("APT-101"));
        assertTrue(n.mensaje().contains("2026-12-10 al 2026-12-12 (2 noches)"));
        assertTrue(n.mensaje().contains("pendiente de confirmación"));
        assertEquals("RES-2026-00007", n.codigoReserva().valor());
        assertEquals(AHORA, n.generadaEn());
    }

    @Test
    void elAvisoDeConfirmacionYElDeCancelacionTienenSuTipoYSuAsunto() {
        Notificacion confirmada = redactor.reservaConfirmada(reserva(), CORREO, AHORA);
        Notificacion cancelada = redactor.reservaCancelada(reserva(), CORREO, AHORA);

        assertEquals(TipoNotificacion.RESERVA_CONFIRMADA, confirmada.tipo());
        assertEquals("Reserva RES-2026-00007 confirmada", confirmada.asunto());
        assertTrue(confirmada.mensaje().contains("está confirmada"));
        assertEquals(TipoNotificacion.RESERVA_CANCELADA, cancelada.tipo());
        assertEquals("Reserva RES-2026-00007 cancelada", cancelada.asunto());
        assertTrue(cancelada.mensaje().contains("fue cancelada"));
    }

    @Test
    void sinCorreoElAvisoSeRedactaIgualParaDejarConstanciaDeQueNoSePudoEnviar() {
        Notificacion n = redactor.reservaCreada(reserva(), null, AHORA);

        assertNull(n.destinatario());
        assertEquals(TipoNotificacion.RESERVA_CREADA, n.tipo());
    }

    @Test
    void elAvisoDeRecuperacionLlevaElCodigoYSuVigenciaYNoPerteneceAUnaReserva() {
        Notificacion n = redactor.codigoRecuperacion(CORREO, "048213", 15, AHORA);

        assertEquals(TipoNotificacion.CODIGO_RECUPERACION, n.tipo());
        assertTrue(n.mensaje().contains("048213"));
        assertTrue(n.mensaje().contains("15 minutos"));
        assertTrue(n.mensaje().contains("una vez"));
        assertNull(n.codigoReserva());
    }

    @Test
    void elNombreDelAlojamientoSeLeeEnCadaAvisoPorqueElAdministradorPuedeEditarlo() {
        nombre = "Casa Cafetera";

        assertTrue(redactor.reservaConfirmada(reserva(), CORREO, AHORA).mensaje().contains("Casa Cafetera"));
    }
}
