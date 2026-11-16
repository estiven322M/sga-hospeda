package co.edu.uniquindio.sga.testsupport;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import co.edu.uniquindio.sga.domain.entity.Ocupante;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.CargoNoche;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.DocumentoIdentidad;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.ValorCongelado;
import co.edu.uniquindio.sga.domain.valueobject.VersionPolitica;

/** Construye reservas válidas para las pruebas de dominio, en el estado que cada prueba necesita. */
public final class ReservasParaPruebas {

    public static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 2, 10, 0);
    private static final LocalDate HOY = LocalDate.of(2026, 11, 2);

    private ReservasParaPruebas() {
    }

    public static Ocupante adulto(String documento) {
        return new Ocupante(new DocumentoIdentidad(documento), "Persona " + documento, LocalDate.of(1990, 1, 1), HOY);
    }

    /** Una reserva PENDIENTE de {@code apartamento} con la estancia dada y {@code ocupantes} personas adultas. */
    public static Reserva pendiente(String codigo, String apartamento, LocalDate entrada, LocalDate salida, int ocupantes) {
        List<Ocupante> grupo = new ArrayList<>();
        for (int i = 0; i < ocupantes; i++) {
            grupo.add(adulto("CC-" + codigo.substring(codigo.length() - 3) + "-" + i));
        }
        List<CargoNoche> noches = entrada.datesUntil(salida)
                .map(noche -> new CargoNoche(noche, "BASE", Dinero.de(100_000), ocupantes)).toList();
        return Reserva.crear(new CodigoReserva(codigo), new IdentificacionApartamento(apartamento),
                new Estancia(entrada, salida), grupo.get(0), grupo, CanalOrigen.DIRECTO, new ValorCongelado(noches),
                new VersionPolitica(1), AHORA, ReservaDePrueba.EDAD_ADULTO);
    }

    public static Reserva confirmada(String codigo, String apartamento, LocalDate entrada, LocalDate salida, int ocupantes) {
        Reserva reserva = pendiente(codigo, apartamento, entrada, salida, ocupantes);
        reserva.indicarHoraEstimadaLlegada(LocalTime.of(16, 0), "Recepción", AHORA);
        reserva.confirmar("Recepción", AHORA.plusHours(1));
        return reserva;
    }
}
