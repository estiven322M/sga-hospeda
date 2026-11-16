package co.edu.uniquindio.sga.domain.valueobject;

import java.util.Locale;
import java.util.regex.Pattern;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;

/**
 * Correo electrónico de contacto (7.2, 10.3.2-A). Se guarda en minúsculas y sin espacios: Laura@SGA.local y
 * laura@sga.local son el mismo correo. Solo valida la forma básica (texto@dominio.ext); que exista de verdad
 * lo sabe únicamente el servidor de correo.
 */
public record CorreoElectronico(String valor) {

    private static final Pattern FORMATO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MAXIMO = 120;

    public CorreoElectronico {
        if (valor == null || valor.isBlank()) {
            throw new ReglaDominioException("El correo electrónico es obligatorio");
        }
        valor = valor.trim().toLowerCase(Locale.ROOT);
        if (valor.length() > MAXIMO || !FORMATO.matcher(valor).matches()) {
            throw new ReglaDominioException("El correo electrónico no tiene un formato válido");
        }
    }
}
