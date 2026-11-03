package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.entity.Ocupante;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.CargoNoche;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.IdentificadorExterno;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.DocumentoIdentidad;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.IdentificadorExterno;
import co.edu.uniquindio.sga.domain.valueobject.ValorCongelado;
import co.edu.uniquindio.sga.domain.valueobject.VersionPolitica;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.IntStream;

/** Construye reservas válidas con los métodos reales del dominio para las pruebas de persistencia. */
public final class ReservasDePrueba {

    public static final ZoneId ZONA = ZoneId.of("America/Bogota");
    public static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 2, 10, 0);
    public static final Clock RELOJ = Clock.fixed(AHORA.atZone(ZONA).toInstant(), ZONA);

    private ReservasDePrueba() {
    }

    public static Ocupante ocupante(String documento, String nombre) {
        return new Ocupante(new DocumentoIdentidad(documento), nombre, LocalDate.of(1990, 5, 20),
                LocalDate.now(RELOJ));
    }

    public static ValorCongelado valor(LocalDate entrada, int noches) {
        return new ValorCongelado(IntStream.range(0, noches)
                .mapToObj(i -> new CargoNoche(entrada.plusDays(i), "BASE", Dinero.de(100_000), 2))
                .toList());
    }

    public static Reserva reserva(String codigo, String apartamento, LocalDate entrada, int noches, String documentoTitular,
                           CanalOrigen canal, LocalDateTime creadaEn) {
        Ocupante titular = ocupante(documentoTitular, "Titular " + documentoTitular);
        return Reserva.crear(new CodigoReserva(codigo), new IdentificacionApartamento(apartamento),
                new Estancia(entrada, entrada.plusDays(noches)), titular,
                List.of(titular, ocupante(documentoTitular + "9", "Acompañante")), canal,
                canal.exigeIdentificadorExterno() ? new IdentificadorExterno("EXT-" + codigo) : null,
                valor(entrada, noches), new VersionPolitica(1), creadaEn);
    }

    public static Reserva reserva(String codigo, String apartamento, LocalDate entrada) {
        return reserva(codigo, apartamento, entrada, 2, "100", CanalOrigen.PORTAL, AHORA);
    }
}
