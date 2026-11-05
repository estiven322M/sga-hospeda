package co.edu.uniquindio.sga.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * Firma y validación del JWT (Guía 12, 5.1 y 5.5), sin levantar Spring: el token que firma el SGA se valida, y uno
 * alterado, de otro emisor, vencido o con el algoritmo cambiado se rechaza.
 */
class JwtConfigTest {

    private static final String SECRETO = "clave-de-pruebas-con-mas-de-32-caracteres-0123";
    private final JwtConfig config = new JwtConfig(SECRETO, "sga");
    private final JwtDecoder decoder = config.jwtDecoder();
    private final JwtTokenProvider proveedor = new JwtTokenProvider(config.jwtEncoder(), "sga");

    private String tokenVigente() {
        Instant ahora = Instant.now();
        return proveedor.generateTokenAsString("huesped1@sga.local", "1094000111", List.of("HUESPED"),
                ahora, ahora.plus(30, ChronoUnit.MINUTES));
    }

    @Test
    void elTokenEmitidoSeValidaYTraeCorreoRolesYDocumento() {
        Jwt jwt = decoder.decode(tokenVigente());

        assertEquals("huesped1@sga.local", jwt.getSubject());
        assertEquals("sga", jwt.getClaimAsString("iss"));
        assertEquals(List.of("HUESPED"), jwt.getClaimAsStringList("roles"));
        assertEquals("1094000111", jwt.getClaimAsString("documento"));
        assertEquals("HS256", jwt.getHeaders().get("alg").toString());
    }

    /**
     * El rol del claim "roles" queda tal cual (HUESPED), sin el prefijo SCOPE_. Spring Security 7 agrega además la
     * autoridad FACTOR_BEARER (indica que se autenticó con un token Bearer); no es un rol del SGA y no estorba a
     * hasAuthority('HUESPED').
     */
    @Test
    void elRolDelTokenQuedaComoAutoridadSinPrefijo() {
        Jwt jwt = decoder.decode(tokenVigente());

        List<String> autoridades = config.jwtAuthenticationConverter().convert(jwt).getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList();

        assertTrue(autoridades.contains("HUESPED"));
        assertFalse(autoridades.contains("SCOPE_HUESPED"));
        assertFalse(autoridades.stream().anyMatch(a -> a.startsWith("ROLE_")));
    }

    @Test
    void unTokenModificadoAManoSeRechaza() {
        String[] partes = tokenVigente().split("\\.");
        String payloadAlterado = Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"iss\":\"sga\",\"sub\":\"huesped1@sga.local\",\"roles\":[\"ADMINISTRADOR\"],"
                        + "\"exp\":" + Instant.now().plusSeconds(600).getEpochSecond() + "}")
                        .getBytes(StandardCharsets.UTF_8));

        assertThrows(JwtException.class, () -> decoder.decode(partes[0] + "." + payloadAlterado + "." + partes[2]));
    }

    @Test
    void unTokenSinFirmaConAlgoritmoNoneSeRechaza() {
        String header = base64("{\"alg\":\"none\",\"typ\":\"JWT\"}");
        String payload = base64("{\"iss\":\"sga\",\"sub\":\"x@sga.local\",\"roles\":[\"ADMINISTRADOR\"],\"exp\":"
                + Instant.now().plusSeconds(600).getEpochSecond() + "}");

        assertThrows(JwtException.class, () -> decoder.decode(header + "." + payload + "."));
    }

    @Test
    void unTokenFirmadoConOtraClaveSeRechaza() {
        JwtConfig otra = new JwtConfig("otra-clave-distinta-tambien-de-32-caracteres", "sga");
        Instant ahora = Instant.now();
        String ajeno = new JwtTokenProvider(otra.jwtEncoder(), "sga").generateTokenAsString(
                "x@sga.local", "1", List.of("ADMINISTRADOR"), ahora, ahora.plusSeconds(600));

        assertThrows(JwtException.class, () -> decoder.decode(ajeno));
    }

    @Test
    void unTokenDeOtroEmisorSeRechaza() {
        JwtTokenProvider otroEmisor = new JwtTokenProvider(config.jwtEncoder(), "otro-sistema");
        Instant ahora = Instant.now();
        String token = otroEmisor.generateTokenAsString("x@sga.local", "1", List.of("HUESPED"),
                ahora, ahora.plusSeconds(600));

        assertThrows(JwtException.class, () -> decoder.decode(token));
    }

    @Test
    void unTokenVencidoSeRechaza() {
        Instant haceUnaHora = Instant.now().minus(1, ChronoUnit.HOURS);
        String vencido = proveedor.generateTokenAsString("x@sga.local", "1", List.of("HUESPED"),
                haceUnaHora, haceUnaHora.plus(30, ChronoUnit.MINUTES));

        assertThrows(JwtException.class, () -> decoder.decode(vencido));
    }

    @Test
    void unaClaveDeMenosDe32CaracteresImpideArrancar() {
        assertThrows(IllegalStateException.class, () -> new JwtConfig("corta", "sga"));
    }

    private static String base64(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
