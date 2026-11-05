package co.edu.uniquindio.sga.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import co.edu.uniquindio.sga.domain.entity.Ocupante;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.CargoNoche;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.DocumentoIdentidad;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.ValorCongelado;
import co.edu.uniquindio.sga.domain.valueobject.VersionPolitica;

/** El rol no basta (7.2): un huésped solo accede a la reserva de la que es titular. */
@ExtendWith(MockitoExtension.class)
class PropietarioReservaVerificadorTest {

    private static final String CODIGO = "RES-2026-00001";

    @Mock private ReservaRepository reservas;

    private PropietarioReservaVerificador verificador() {
        return new PropietarioReservaVerificador(reservas);
    }

    static Authentication huespedConDocumento(String documento) {
        Jwt.Builder jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject("huesped@sga.local");
        if (documento != null) {
            jwt.claim("documento", documento);
        } else {
            jwt.claim("roles", List.of("HUESPED"));
        }
        return new JwtAuthenticationToken(jwt.build(), List.of(new SimpleGrantedAuthority("HUESPED")));
    }

    private Reserva reservaDe(String documentoTitular) {
        Ocupante titular = new Ocupante(new DocumentoIdentidad(documentoTitular), "Laura",
                LocalDate.of(1995, 1, 30), LocalDate.of(2026, 1, 1));
        return Reserva.crear(new CodigoReserva(CODIGO), new IdentificacionApartamento("APT-101"),
                new Estancia(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12)), titular, List.of(titular),
                CanalOrigen.PORTAL, new ValorCongelado(List.of(
                        new CargoNoche(LocalDate.of(2026, 12, 10), "ALTA", Dinero.de(100_000), 1),
                        new CargoNoche(LocalDate.of(2026, 12, 11), "ALTA", Dinero.de(100_000), 1))),
                new VersionPolitica(1), LocalDateTime.of(2026, 11, 2, 10, 0));
    }

    @Test
    void elTitularDeLaReservaEsSuPropietario() {
        when(reservas.buscarPorCodigo(new CodigoReserva(CODIGO))).thenReturn(Optional.of(reservaDe("1094000111")));

        assertTrue(verificador().esTitular(CODIGO, huespedConDocumento("1094000111")));
    }

    @Test
    void otroHuespedNoEsPropietarioAunqueTengaElMismoRol() {
        when(reservas.buscarPorCodigo(new CodigoReserva(CODIGO))).thenReturn(Optional.of(reservaDe("1094000111")));

        assertFalse(verificador().esTitular(CODIGO, huespedConDocumento("1094000222")));
    }

    @Test
    void siLaReservaNoExisteTambienSeNiegaParaNoRevelarQueCodigosExisten() {
        when(reservas.buscarPorCodigo(new CodigoReserva(CODIGO))).thenReturn(Optional.empty());

        assertFalse(verificador().esTitular(CODIGO, huespedConDocumento("1094000111")));
    }

    @Test
    void unCodigoMalFormadoSeNiegaSinConsultarLaBase() {
        assertFalse(verificador().esTitular("RES-NO-EXISTE", huespedConDocumento("1094000111")));
    }

    @Test
    void unTokenSinDocumentoNoEsPropietarioDeNada() {
        assertFalse(verificador().esTitular(CODIGO, huespedConDocumento(null)));
    }

    @Test
    void unaAutenticacionQueNoEsJwtSeNiega() {
        Authentication otra = UsernamePasswordAuthenticationToken.authenticated("x", null,
                List.of(new SimpleGrantedAuthority("HUESPED")));

        assertFalse(verificador().esTitular(CODIGO, otra));
    }
}
