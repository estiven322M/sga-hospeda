package co.edu.uniquindio.sga.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import co.edu.uniquindio.sga.application.dto.request.CrearReservaRequest;
import co.edu.uniquindio.sga.application.dto.request.OcupanteRequest;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;

/** Rol y canal (Guía 12, 7.5 punto 3): quién puede crear por qué canal y a nombre de quién. */
class CanalReservaVerificadorTest {

    private static final String HUESPED_1 = "1094000111";
    private final CanalReservaVerificador verificador = new CanalReservaVerificador();

    private static Authentication usuario(String rol, String documento) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject("u@sga.local")
                .claim("documento", documento).build();
        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority(rol)));
    }

    private static CrearReservaRequest reserva(CanalOrigen canal, String documentoTitular) {
        OcupanteRequest titular = new OcupanteRequest(documentoTitular, "Laura Gomez", LocalDate.of(1995, 1, 30));
        return new CrearReservaRequest("APT-101", LocalDate.of(2028, 3, 10), LocalDate.of(2028, 3, 12), canal,
                canal == CanalOrigen.EXTERNO ? "EXT-1" : null, null, titular, List.of(titular));
    }

    @Test
    void elHuespedReservaPorElPortalASuNombre() {
        assertTrue(verificador.puedeCrear(reserva(CanalOrigen.PORTAL, HUESPED_1), usuario("HUESPED", HUESPED_1)));
    }

    @Test
    void elHuespedNoReservaANombreDeOtraPersona() {
        assertFalse(verificador.puedeCrear(reserva(CanalOrigen.PORTAL, "1094000222"), usuario("HUESPED", HUESPED_1)));
    }

    @Test
    void elHuespedNoUsaElCanalDirectoNiElExterno() {
        assertFalse(verificador.puedeCrear(reserva(CanalOrigen.DIRECTO, HUESPED_1), usuario("HUESPED", HUESPED_1)));
        assertFalse(verificador.puedeCrear(reserva(CanalOrigen.EXTERNO, HUESPED_1), usuario("HUESPED", HUESPED_1)));
    }

    @Test
    void elRecepcionistaReservaPorElCanalDirectoANombreDeCualquierTitular() {
        assertTrue(verificador.puedeCrear(reserva(CanalOrigen.DIRECTO, "CC-9001"),
                usuario("RECEPCIONISTA", "1000000002")));
    }

    @Test
    void elRecepcionistaNoUsaElPortalNiElCanalExterno() {
        assertFalse(verificador.puedeCrear(reserva(CanalOrigen.PORTAL, "CC-9001"),
                usuario("RECEPCIONISTA", "1000000002")));
        assertFalse(verificador.puedeCrear(reserva(CanalOrigen.EXTERNO, "CC-9001"),
                usuario("RECEPCIONISTA", "1000000002")));
    }

    @Test
    void elAdministradorReservaPorElCanalDirectoYRegistraLasExternas() {
        assertTrue(verificador.puedeCrear(reserva(CanalOrigen.DIRECTO, "CC-9001"),
                usuario("ADMINISTRADOR", "1000000001")));
        assertTrue(verificador.puedeCrear(reserva(CanalOrigen.EXTERNO, "CC-9001"),
                usuario("ADMINISTRADOR", "1000000001")));
        assertFalse(verificador.puedeCrear(reserva(CanalOrigen.PORTAL, "CC-9001"),
                usuario("ADMINISTRADOR", "1000000001")));
    }

    @Test
    void elPersonalDeServicioNoCreaReservas() {
        assertFalse(verificador.puedeCrear(reserva(CanalOrigen.DIRECTO, "CC-9001"),
                usuario("PERSONAL_SERVICIO", "1000000003")));
    }
}
