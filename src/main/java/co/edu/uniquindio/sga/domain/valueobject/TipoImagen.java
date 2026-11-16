package co.edu.uniquindio.sga.domain.valueobject;

import java.util.Optional;

/**
 * Formatos de imagen que admite el sistema (7.3): JPG, PNG y WEBP. El formato se reconoce por los primeros bytes del
 * contenido (su "firma"), no por la extensión ni por el tipo que declare el cliente: un archivo renombrado no se cuela.
 */
public enum TipoImagen {
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp");

    private final String tipoMime;
    private final String extension;

    TipoImagen(String tipoMime, String extension) {
        this.tipoMime = tipoMime;
        this.extension = extension;
    }

    public String tipoMime() {
        return tipoMime;
    }

    public String extension() {
        return extension;
    }

    /** El formato del contenido según su firma, o vacío si no es JPG, PNG ni WEBP. */
    public static Optional<TipoImagen> detectar(byte[] bytes) {
        if (bytes == null) {
            return Optional.empty();
        }
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return Optional.of(JPEG);
        }
        if (bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'
                && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A) {
            return Optional.of(PNG);
        }
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return Optional.of(WEBP);
        }
        return Optional.empty();
    }
}
