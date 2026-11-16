package co.edu.uniquindio.sga.application.dto.response;

import java.time.LocalDateTime;

import co.edu.uniquindio.sga.domain.valueobject.OperacionCanal;
import co.edu.uniquindio.sga.domain.valueobject.ResultadoCanal;

/** Un renglón de la bitácora del canal (7.8). {@code identificadorExterno} es el del canal (sin el código por delante). */
public record EventoCanalResponse(
        LocalDateTime registradoEn,
        String canal,
        OperacionCanal operacion,
        String identificadorExterno,
        ResultadoCanal resultado,
        int codigoHttp,
        String detalle) {
}
