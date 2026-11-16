package co.edu.uniquindio.sga.domain.valueobject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/**
 * Credencial de acceso de un canal externo (7.8, 10.3.1): un texto aleatorio de 256 bits que el canal envía en el encabezado
 * {@code X-Canal-Credencial}. Se muestra UNA sola vez, al crearla, y el sistema solo guarda su HASH (SHA-256). Como la credencial
 * ya es aleatoria y de alta entropía, un hash rápido y determinista es suficiente (y permite encontrar al canal por su credencial
 * sin pedir su nombre); las contraseñas de las personas, que sí son predecibles, usan BCrypt. Es JDK puro: no conoce ningún framework.
 */
public record CredencialCanal(String valor, String hash) {

    public static final String PREFIJO = "sgc_";
    private static final SecureRandom AZAR = new SecureRandom();

    public CredencialCanal {
        if (valor == null || valor.isBlank() || hash == null || hash.isBlank()) {
            throw new ReglaDominioException("La credencial del canal y su hash son obligatorios");
        }
    }

    /** Una credencial nueva, con 32 bytes aleatorios (por ejemplo {@code sgc_Zk3...}). */
    public static CredencialCanal generar() {
        byte[] bytes = new byte[32];
        AZAR.nextBytes(bytes);
        String valor = PREFIJO + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new CredencialCanal(valor, hashDe(valor));
    }

    /** La credencial con ese texto (la que se sembró o la que llegó en el encabezado) y su hash. */
    public static CredencialCanal de(String valor) {
        return new CredencialCanal(valor, hashDe(valor));
    }

    /** SHA-256 en hexadecimal del texto de la credencial. */
    public static String hashDe(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new ReglaDominioException("La credencial del canal es obligatoria");
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.trim().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible", e);
        }
    }

    /** Nunca imprime la credencial: un registro o una traza que la incluyera la filtraría. */
    @Override
    public String toString() {
        return "CredencialCanal[oculta]";
    }
}
