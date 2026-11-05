package co.edu.uniquindio.sga.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

/**
 * Cadena de filtros de seguridad del SGA (Guía 12, 5.4).
 * <ul>
 *   <li>Por URL se decide lo grueso: qué es público (login, registro, /error y herramientas de desarrollo) y que
 *       todo lo demás exige token.</li>
 *   <li>Por método ({@code @PreAuthorize}, habilitado con {@code @EnableMethodSecurity}) se decide lo fino:
 *       qué rol y si es el propietario del recurso (matriz 7.1).</li>
 * </ul>
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
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                // La consola de H2 se dibuja en un iframe del mismo origen
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()
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
