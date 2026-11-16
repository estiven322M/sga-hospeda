package co.edu.uniquindio.sga.infrastructure.security;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import co.edu.uniquindio.sga.application.usecase.AutenticarCanalUseCase;
import co.edu.uniquindio.sga.application.usecase.RegistrarEventoCanalUseCase;
import co.edu.uniquindio.sga.domain.entity.CanalVenta;
import co.edu.uniquindio.sga.domain.valueobject.OperacionCanal;
import co.edu.uniquindio.sga.domain.valueobject.ResultadoCanal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Autenticación del canal externo (10.3.1), SIN JWT: la credencial llega en el encabezado {@code X-Canal-Credencial}. Solo actúa en la
 * cadena de seguridad de {@code /api/canal/**}.
 * <ul>
 *   <li>Sin credencial, o desconocida: 401 y nada más (no hay canal al que atribuir el mensaje; solo queda en el log).</li>
 *   <li>Credencial de un canal DESACTIVADO: 401 y queda en la bitácora como NO_AUTORIZADA.</li>
 *   <li>Credencial válida: el canal queda disponible para el controller ({@link #ATRIBUTO_CANAL}) y con la autoridad CANAL_EXTERNO.</li>
 * </ul>
 * Después de la respuesta registra en la bitácora los mensajes que ni el caso de uso ni el 401 dejaron registrados: los mal armados
 * (400, INVALIDA), los que fallaron al convertirse (409 o 404, antes de llegar al caso de uso) y los fallos del servidor (500, ERROR). El mensaje del 401 es el mismo para una credencial desconocida y para un
 * canal desactivado: no se revela cuál de las dos es.
 */
public class CredencialCanalFilter extends OncePerRequestFilter {

    public static final String ENCABEZADO = "X-Canal-Credencial";
    public static final String ATRIBUTO_CANAL = "sga.canal";
    public static final String AUTORIDAD = "CANAL_EXTERNO";
    private static final String MENSAJE_401 = "Credencial del canal inválida o canal desactivado: envíe una credencial vigente en el encabezado " + ENCABEZADO;
    private static final Logger log = LoggerFactory.getLogger(CredencialCanalFilter.class);

    private final AutenticarCanalUseCase autenticar;
    private final RegistrarEventoCanalUseCase bitacora;

    public CredencialCanalFilter(AutenticarCanalUseCase autenticar, RegistrarEventoCanalUseCase bitacora) {
        this.autenticar = autenticar;
        this.bitacora = bitacora;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadena)
            throws ServletException, IOException {
        Optional<CanalVenta> encontrado = autenticar.ejecutar(request.getHeader(ENCABEZADO));
        if (encontrado.isEmpty()) {
            log.warn("Mensaje rechazado en {} {}: credencial de canal ausente o desconocida", request.getMethod(), request.getRequestURI());
            responder401(request, response);
            return;
        }
        CanalVenta canal = encontrado.get();
        if (!canal.isActivo()) {
            bitacora.ejecutar(canal.getCodigo(), operacionDe(request), identificadorDe(request), ResultadoCanal.NO_AUTORIZADA, 401,
                    "El canal está desactivado");
            responder401(request, response);
            return;
        }

        request.setAttribute(ATRIBUTO_CANAL, canal);
        RegistrarEventoCanalUseCase.reiniciarConstancia();
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(canal.getCodigo(), null,
                List.of(new SimpleGrantedAuthority(AUTORIDAD))));
        SecurityContextHolder.setContext(contexto);
        try {
            cadena.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
        // Un mensaje que falló antes de llegar al caso de uso (mal armado, fecha imposible...) no dejó constancia: se registra aquí
        int estado = response.getStatus();
        if (!RegistrarEventoCanalUseCase.huboRegistro() && estado >= 400) {
            ResultadoCanal resultado = estado == 400 ? ResultadoCanal.INVALIDA : estado == 404 ? ResultadoCanal.NO_ENCONTRADA
                    : estado == 409 ? ResultadoCanal.RECHAZADA : estado >= 500 ? ResultadoCanal.ERROR : null;
            if (resultado != null) {
                bitacora.ejecutar(canal.getCodigo(), operacionDe(request), identificadorDe(request), resultado, estado, detalleDe(estado));
            }
        }
        RegistrarEventoCanalUseCase.reiniciarConstancia();
    }

    private static String detalleDe(int estado) {
        return switch (estado) {
            case 400 -> "El mensaje está mal armado";
            case 404 -> "El recurso pedido no existe";
            case 409 -> "El mensaje no cumple una regla del negocio";
            default -> "Fallo del servidor al procesar el mensaje";
        };
    }

    private static OperacionCanal operacionDe(HttpServletRequest request) {
        String ruta = request.getRequestURI();
        if (ruta.endsWith("/cancelar")) return OperacionCanal.CANCELAR_RESERVA;
        if (ruta.endsWith("/disponibilidad")) return OperacionCanal.CONSULTAR_DISPONIBILIDAD;
        if ("GET".equals(request.getMethod())) return OperacionCanal.CONSULTAR_RESERVA;
        return OperacionCanal.CREAR_RESERVA;
    }

    /** En la cancelación el identificador viaja en la ruta: /api/canal/reservas/{id}/cancelar. */
    private static String identificadorDe(HttpServletRequest request) {
        String ruta = request.getRequestURI();
        String prefijo = "/api/canal/reservas/";
        if (ruta.startsWith(prefijo) && ruta.endsWith("/cancelar") && ruta.length() > prefijo.length() + "/cancelar".length()) {
            String id = ruta.substring(prefijo.length(), ruta.length() - "/cancelar".length());
            return id.length() > 60 ? id.substring(0, 60) : id;
        }
        return null;
    }

    private static void responder401(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String ruta = request.getRequestURI().replace("\\", "\\\\").replace("\"", "\\\"");
        response.getWriter().write("{\"timestamp\":\"" + Instant.now() + "\",\"status\":401,\"error\":\"Unauthorized\",\"mensaje\":\""
                + MENSAJE_401 + "\",\"path\":\"" + ruta + "\",\"errores\":null}");
    }
}
