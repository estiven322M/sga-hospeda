package co.edu.uniquindio.sga.application.dto.request;

/**
 * Forma de una contraseña segura (7.2): mínimo 8 caracteres, al menos una mayúscula y un número. Es la MISMA regla del
 * registro, del cambio de contraseña y de la recuperación; Bean Validation la revisa (400) en cada DTO que la usa.
 */
public final class ReglasContrasena {

    public static final String PATRON = "^(?=.*[A-Z])(?=.*\\d).{8,72}$";
    public static final String MENSAJE = "La contraseña debe tener mínimo 8 caracteres, al menos una mayúscula y un número";

    private ReglasContrasena() {
    }
}
