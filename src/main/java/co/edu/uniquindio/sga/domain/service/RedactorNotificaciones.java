package co.edu.uniquindio.sga.domain.service;

import java.time.LocalDateTime;
import java.util.function.Supplier;

import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.valueobject.CorreoElectronico;
import co.edu.uniquindio.sga.domain.valueobject.Notificacion;
import co.edu.uniquindio.sga.domain.valueobject.TipoNotificacion;

/**
 * Redacta los avisos al titular (10.3.2-A): qué se dice en cada momento de la reserva. Es dominio puro: solo arma
 * el texto; por dónde sale lo decide el puerto Notificador. El destinatario puede ser nulo (no se conoce correo): el
 * aviso se redacta igual, para dejar constancia de que no pudo enviarse.
 */
public class RedactorNotificaciones {

    /** El nombre se pide en cada aviso porque el administrador puede editarlo (L-01). */
    private final Supplier<String> nombreAlojamiento;

    public RedactorNotificaciones(Supplier<String> nombreAlojamiento) {
        this.nombreAlojamiento = nombreAlojamiento;
    }

    public Notificacion reservaCreada(Reserva reserva, CorreoElectronico destinatario, LocalDateTime ahora) {
        return new Notificacion(TipoNotificacion.RESERVA_CREADA, destinatario,
                "Reserva " + reserva.getCodigo().valor() + " registrada",
                saludo(reserva) + "Registramos su reserva " + reserva.getCodigo().valor() + ". " + detalle(reserva)
                        + " Queda pendiente de confirmación: se confirma al cubrir el anticipo y fijar la hora estimada de llegada.",
                reserva.getCodigo(), ahora);
    }

    public Notificacion reservaConfirmada(Reserva reserva, CorreoElectronico destinatario, LocalDateTime ahora) {
        return new Notificacion(TipoNotificacion.RESERVA_CONFIRMADA, destinatario,
                "Reserva " + reserva.getCodigo().valor() + " confirmada",
                saludo(reserva) + "Su reserva " + reserva.getCodigo().valor() + " está confirmada. " + detalle(reserva)
                        + " Lo esperamos.",
                reserva.getCodigo(), ahora);
    }

    public Notificacion reservaCancelada(Reserva reserva, CorreoElectronico destinatario, LocalDateTime ahora) {
        return new Notificacion(TipoNotificacion.RESERVA_CANCELADA, destinatario,
                "Reserva " + reserva.getCodigo().valor() + " cancelada",
                saludo(reserva) + "Su reserva " + reserva.getCodigo().valor() + " fue cancelada. " + detalle(reserva)
                        + " Si tiene saldo a favor, recepción le informará el reembolso según la política de cancelación.",
                reserva.getCodigo(), ahora);
    }

    /** El código va en claro solo en este mensaje: nunca se guarda así (solo su hash). */
    public Notificacion codigoRecuperacion(CorreoElectronico destinatario, String codigo, int vigenciaMinutos,
                                           LocalDateTime ahora) {
        return new Notificacion(TipoNotificacion.CODIGO_RECUPERACION, destinatario,
                "Código para recuperar su contraseña",
                "Su código de recuperación es " + codigo + ". Vence en " + vigenciaMinutos
                        + " minutos y solo sirve una vez. Si usted no lo solicitó, ignore este mensaje.",
                null, ahora);
    }

    private String saludo(Reserva reserva) {
        return "Hola " + reserva.getTitular().getNombre() + ", gracias por elegir " + nombreAlojamiento.get() + ". ";
    }

    private static String detalle(Reserva reserva) {
        int noches = reserva.getEstancia().noches();
        return "Apartamento " + reserva.getApartamento().valor() + ", del " + reserva.getEstancia().fechaEntrada()
                + " al " + reserva.getEstancia().fechaSalida() + " (" + noches + (noches == 1 ? " noche)." : " noches).");
    }

}
