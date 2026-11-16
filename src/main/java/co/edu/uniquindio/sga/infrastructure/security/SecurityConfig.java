package co.edu.uniquindio.sga.infrastructure.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Cadena de filtros de seguridad del SGA (Guía 12, 5.4).
 * <ul>
 *   <li>Por URL se decide lo grueso: qué es público (login, registro, /error y herramientas de desarrollo) y que
 *       todo lo demás exige token.</li>
 *   <li>Por método ({@code @PreAuthorize}, habilitado con {@code @EnableMethodSecurity}) se decide lo fino:
 *       qué rol y si es el propietario del recurso (matriz 7.1).</li>
 * </ul>
 * CORS permite al frontend (otro origen) llamar a la API; ver {@link #corsConfigurationSource}.
 * CSRF se desactiva porque la API no usa cookies de sesión: el token viaja en el encabezado Authorization,
 * que el navegador no agrega solo. Si el token pasara a una cookie, CSRF debe reactivarse.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /** Rutas públicas: lo mínimo (8.9). Swagger UI y la consola de H2 se cierran en el sistema desplegado. */
    static final String[] RUTAS_PUBLICAS = {
            "/api/auth/login",
            "/api/auth/registro",
            "/error",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/h2-console/**"
    };

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final ManejadorErroresSeguridad manejadorErrores;

    public SecurityConfig(JwtDecoder jwtDecoder, JwtAuthenticationConverter jwtAuthenticationConverter,
                          ManejadorErroresSeguridad manejadorErrores) {
        this.jwtDecoder = jwtDecoder;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        this.manejadorErrores = manejadorErrores;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            // Se inyecta por parámetro del método y con nombre: por constructor, Spring tomaría el bean de MVC (que no tiene
            // configuración CORS) en lugar del definido en esta misma clase, y el preflight respondería 200 sin cabeceras.
            @Qualifier("corsConfigurationSource") CorsConfigurationSource corsConfigurationSource) throws Exception {
        return http
                // CORS antes de la autenticación: el preflight OPTIONS del navegador no lleva token y debe responderse sin él
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                // La consola de H2 se dibuja en un iframe del mismo origen
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()
                        // Los datos del alojamiento son públicos (Inicio, detalle y mapa); editarlos exige ADMINISTRADOR
                        .requestMatchers(HttpMethod.GET, "/api/alojamiento").permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(manejadorErrores)
                        .accessDeniedHandler(manejadorErrores))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(manejadorErrores)
                        .accessDeniedHandler(manejadorErrores))
                .build();
    }

    /**
     * CORS (Guía 15, 4.2): el navegador solo deja que una página servida desde otro origen (p. ej. el servidor de Vite en
     * http://localhost:5173) lea las respuestas de esta API si la API lo autoriza. Solo se autorizan los orígenes de la
     * propiedad {@code sga.cors.origenes-permitidos} (separados por comas): nunca "*", porque el token viaja en un
     * encabezado.
     * <ul>
     *   <li>Métodos: GET, POST, PUT y OPTIONS (el preflight). No hay DELETE: la eliminación es lógica.</li>
     *   <li>Encabezados que puede enviar el cliente: Authorization (el JWT) y Content-Type (el cuerpo JSON).</li>
     *   <li>Encabezado que puede leer el cliente: Location (la URL de lo recién creado, 201).</li>
     *   <li>Sin credenciales (cookies): el token no va en una cookie.</li>
     * </ul>
     * Un origen no autorizado recibe 403 en el preflight y la respuesta no trae Access-Control-Allow-Origin.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${sga.cors.origenes-permitidos}") List<String> origenesPermitidos) {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(origenesPermitidos.stream().map(String::trim).filter(o -> !o.isEmpty()).toList());
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuracion.setExposedHeaders(List.of("Location"));
        configuracion.setAllowCredentials(false);
        configuracion.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", configuracion);
        return fuente;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /** BCrypt, guardando el algoritmo junto al hash ({bcrypt}$2a$10$...) para poder migrarlo sin romper nada. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
