package co.edu.uniquindio.sga.infrastructure.rest.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import co.edu.uniquindio.sga.application.exception.RecursoNoEncontradoException;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Único manejador de errores de la API. Convención:
 *  400 el mensaje está mal escrito (forma) · 404 el identificador no existe · 409 el negocio no lo permite.
 * Nunca devuelve trazas técnicas: la traza de un error inesperado va al log, no al cliente.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Bean Validation sobre el cuerpo (@Valid): devuelve campo → mensaje. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidacionCuerpo(MethodArgumentNotValidException ex,
                                                                HttpServletRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errores.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return construir(HttpStatus.BAD_REQUEST, "La petición tiene datos inválidos", request, errores);
    }

    /**
     * Bean Validation cuando el método mezcla restricciones de parámetros de la URL (por ejemplo, página
     * negativa) con un cuerpo @Valid: Spring valida todo junto y lanza esta excepción en lugar de la anterior.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidacionParametros(HandlerMethodValidationException ex,
                                                                    HttpServletRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(resultado -> {
            if (resultado instanceof ParameterErrors errorDeCuerpo) {
                errorDeCuerpo.getFieldErrors()
                        .forEach(error -> errores.putIfAbsent(error.getField(), error.getDefaultMessage()));
            } else {
                String parametro = resultado.getMethodParameter().getParameterName();
                resultado.getResolvableErrors().forEach(error -> errores.putIfAbsent(
                        parametro == null ? "parametro" : parametro, error.getDefaultMessage()));
            }
        });
        return construir(HttpStatus.BAD_REQUEST, "La petición tiene datos inválidos", request, errores);
    }

    /** JSON mal formado, fecha inválida o valor de enum inexistente (en el cuerpo o en la URL). */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleFormatoInvalido(Exception ex, HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST, "La petición tiene un formato inválido: revise el JSON, "
                + "las fechas (AAAA-MM-DD) y los valores permitidos", request, null);
    }

    /** El identificador no existe. */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleNoEncontrado(RecursoNoEncontradoException ex,
                                                            HttpServletRequest request) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    /** Una regla de negocio (RN-xx, L-xx) lo impide. */
    @ExceptionHandler(ReglaDominioException.class)
    public ResponseEntity<ErrorResponse> handleReglaDominio(ReglaDominioException ex, HttpServletRequest request) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    /** Todo lo demás. Los errores propios de Spring MVC (ruta inexistente, método no permitido...) conservan su código. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleInesperado(Exception ex, HttpServletRequest request) {
        if (ex instanceof org.springframework.web.ErrorResponse errorDeSpring) {
            HttpStatus status = HttpStatus.valueOf(errorDeSpring.getStatusCode().value());
            return construir(status, status.getReasonPhrase(), request, null);
        }
        log.error("Error inesperado en {}", request.getRequestURI(), ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", request, null);
    }

    private ResponseEntity<ErrorResponse> construir(HttpStatus status, String mensaje, HttpServletRequest request,
                                                    Map<String, String> errores) {
        ErrorResponse cuerpo = new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(),
                mensaje, request.getRequestURI(), errores);
        return ResponseEntity.status(status).body(cuerpo);
    }
}
