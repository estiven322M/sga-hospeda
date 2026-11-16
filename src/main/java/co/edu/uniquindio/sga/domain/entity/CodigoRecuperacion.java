package co.edu.uniquindio.sga.domain.entity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.random.RandomGenerator;
import java.util.regex.Pattern;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.CorreoElectronico;

/**
 * Código de recuperación de contraseña (7.2): de 6 dígitos, vigente un tiempo limitado, de UN solo uso y con un tope de
 * intentos fallidos. El dominio nunca ve el código en claro al validarlo: guarda su HASH y quien lo compara (el
 * adaptador, con el mismo cifrado de las contraseñas) le dice si coincide.
 * Todo fallo se responde con el mismo mensaje: no se revela si el correo existe, si el código venció o si ya se usó.
 */
public class CodigoRecuperacion {

    public static final int DIGITOS = 6;
    private static final Pattern FORMATO = Pattern.compile("^\\d{" + DIGITOS + "}$");
    public static final String MENSAJE_RECHAZO = "El código no es válido o ya venció";

    private final CorreoElectronico correo;
    private final String hash;
    private final LocalDateTime expiraEn;
    private boolean usado;
    private int intentosFallidos;

    private CodigoRecuperacion(CorreoElectronico correo, String hash, LocalDateTime expiraEn, boolean usado,
                               int intentosFallidos) {
        if (correo == null) {
            throw new ReglaDominioException("El código de recuperación exige un correo");
        }
        if (hash == null || hash.isBlank()) {
            throw new ReglaDominioException("El código de recuperación se guarda cifrado: el hash es obligatorio");
        }
        if (expiraEn == null) {
            throw new ReglaDominioException("El código de recuperación exige su fecha de vencimiento");
        }
        this.correo = correo;
        this.hash = hash;
        this.expiraEn = expiraEn;
        this.usado = usado;
        this.intentosFallidos = intentosFallidos;
    }

    /** Emite un código nuevo, sin usar y sin intentos, que vence {@code vigenciaMinutos} después de {@code ahora}. */
    public static CodigoRecuperacion emitir(CorreoElectronico correo, String hashDelCodigo, LocalDateTime ahora,
                                            int vigenciaMinutos) {
        if (vigenciaMinutos <= 0) {
            throw new ReglaDominioException("La vigencia del código de recuperación debe ser mayor que cero");
        }
        Objects.requireNonNull(ahora, "ahora");
        return new CodigoRecuperacion(correo, hashDelCodigo, ahora.plusMinutes(vigenciaMinutos), false, 0);
    }

    /** Restaura un código guardado (persistencia). Solo valida la integridad de los datos. */
    public static CodigoRecuperacion reconstruir(CorreoElectronico correo, String hash, LocalDateTime expiraEn,
                                                 boolean usado, int intentosFallidos) {
        return new CodigoRecuperacion(correo, hash, expiraEn, usado, intentosFallidos);
    }

    /** Un código de {@value #DIGITOS} dígitos (con ceros a la izquierda) a partir de la fuente de azar dada. */
    public static String generarTexto(RandomGenerator azar) {
        return String.format("%0" + DIGITOS + "d", azar.nextInt((int) Math.pow(10, DIGITOS)));
    }

    public static boolean tieneFormato(String texto) {
        return texto != null && FORMATO.matcher(texto).matches();
    }

    /**
     * Usa el código. Falla (con el mismo mensaje) si ya se usó, si venció, si se agotaron los intentos o si no
     * coincide; un código que no coincide suma un intento fallido. Si todo está bien queda usado y no sirve otra vez.
     *
     * @param coincide        si el código escrito corresponde al hash guardado (lo decide el adaptador)
     * @param intentosMaximos cuántos fallos se toleran antes de bloquear el código
     */
    public void consumir(boolean coincide, LocalDateTime ahora, int intentosMaximos) {
        if (usado || !ahora.isBefore(expiraEn) || intentosFallidos >= intentosMaximos) {
            throw new ReglaDominioException(MENSAJE_RECHAZO);
        }
        if (!coincide) {
            intentosFallidos++;
            throw new ReglaDominioException(MENSAJE_RECHAZO);
        }
        usado = true;
    }

    public CorreoElectronico getCorreo() { return correo; }
    public String getHash() { return hash; }
    public LocalDateTime getExpiraEn() { return expiraEn; }
    public boolean isUsado() { return usado; }
    public int getIntentosFallidos() { return intentosFallidos; }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof CodigoRecuperacion otro && correo.equals(otro.correo));
    }

    @Override
    public int hashCode() {
        return correo.hashCode();
    }
}
