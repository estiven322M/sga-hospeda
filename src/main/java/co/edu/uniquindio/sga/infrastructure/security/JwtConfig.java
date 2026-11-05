package co.edu.uniquindio.sga.infrastructure.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Firma y validación del JWT con HMAC-SHA256 (Guía 12, 5.1). Una sola clave simétrica firma y valida:
 * el SGA es una sola aplicación que emite y valida sus propios tokens.
 * <ul>
 *   <li>{@code macAlgorithm(HS256)} fija el algoritmo: un token que declare otro se rechaza (algorithm swap).</li>
 *   <li>{@code createDefaultWithIssuer} valida la expiración ({@code exp}) y que lo emitió el SGA ({@code iss}).</li>
 *   <li>{@code setAuthorityPrefix("")}: el rol del token queda tal cual (RECEPCIONISTA), sin el prefijo SCOPE_.</li>
 * </ul>
 */
@Configuration
public class JwtConfig {

    /** HS256 exige una clave de al menos 256 bits. */
    static final int LONGITUD_MINIMA_CLAVE = 32;

    private final SecretKey clave;
    private final String emisor;

    public JwtConfig(@Value("${sga.jwt.secret}") String secreto,
                     @Value("${sga.jwt.emisor}") String emisor) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < LONGITUD_MINIMA_CLAVE) {
            throw new IllegalStateException(
                    "sga.jwt.secret debe tener al menos 32 caracteres (256 bits) para HS256");
        }
        this.clave = new SecretKeySpec(bytes, "HmacSHA256");
        this.emisor = emisor;
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(clave));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(clave)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(emisor));
        return decoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }
}
