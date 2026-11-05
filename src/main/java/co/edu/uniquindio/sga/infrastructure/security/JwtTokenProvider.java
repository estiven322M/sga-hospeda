package co.edu.uniquindio.sga.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Construye y firma el JWT del SGA (Guía 12, 5.5). El JWT se FIRMA, no se cifra: cualquiera puede leer los claims,
 * por eso solo viajan el correo (sub), los roles y el documento. Nunca la contraseña ni la fecha de nacimiento.
 */
@Component
public class JwtTokenProvider {

    /** Claim con los roles; JwtConfig lo convierte en autoridades. */
    public static final String CLAIM_ROLES = "roles";
    /** Claim con el documento de identidad; con él se verifica el propietario del recurso (7.2). */
    public static final String CLAIM_DOCUMENTO = "documento";

    private final JwtEncoder encoder;
    private final String emisor;

    public JwtTokenProvider(JwtEncoder encoder, @Value("${sga.jwt.emisor}") String emisor) {
        this.encoder = encoder;
        this.emisor = emisor;
    }

    public String generateTokenAsString(String correo, String documento, List<String> roles,
                                        Instant emitidoEn, Instant expiraEn) {

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(emisor)
                .issuedAt(emitidoEn)
                .expiresAt(expiraEn)
                .subject(correo)
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_DOCUMENTO, documento)
                .build();

        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
