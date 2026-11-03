package co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper;

import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.ReservaEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static co.edu.uniquindio.sga.infrastructure.persistence.jpa.ReservasDePrueba.AHORA;
import static co.edu.uniquindio.sga.infrastructure.persistence.jpa.ReservasDePrueba.RELOJ;
import static co.edu.uniquindio.sga.infrastructure.persistence.jpa.ReservasDePrueba.reserva;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReservaPersistenceMapperTest {

    private final ReservaPersistenceMapper mapper = new ReservaPersistenceMapper(
            new EstanciaPersistenceMapper(), new OcupantePersistenceMapper(RELOJ));

    private Reserva reservaConfirmada() {
        Reserva reserva = reserva("RES-2026-00001", "APT-101", LocalDate.of(2027, 2, 10), 3, "100",
                CanalOrigen.EXTERNO, AHORA);
        reserva.indicarHoraEstimadaLlegada(LocalTime.of(16, 30), "Titular 100", AHORA.plusMinutes(5));
        reserva.confirmar("Recepción", AHORA.plusMinutes(10));
        return reserva;
    }

    @Test
    void ida_y_vuelta_conserva_estado_historial_ocupantes_y_desglose_en_orden() {
        Reserva original = reservaConfirmada();

        Reserva vuelta = mapper.aDominio(mapper.aEntidad(original));

        assertEquals(original.getCodigo(), vuelta.getCodigo());
        assertEquals(EstadoReserva.CONFIRMADA, vuelta.getEstado());
        assertEquals(original.getApartamento(), vuelta.getApartamento());
        assertEquals(original.getEstancia(), vuelta.getEstancia());
        assertEquals(original.getCanalOrigen(), vuelta.getCanalOrigen());
        assertEquals(original.getHoraEstimadaLlegada(), vuelta.getHoraEstimadaLlegada());
        assertEquals(original.getPolitica(), vuelta.getPolitica());
        assertEquals(original.getCreadaEn(), vuelta.getCreadaEn());
        assertEquals(original.getHistorial(), vuelta.getHistorial());
        assertEquals(original.getValor(), vuelta.getValor());
        assertEquals(original.getOcupantes(), vuelta.getOcupantes());
        assertEquals(original.getTitular(), vuelta.getTitular());
    }

    @Test
    void reserva_pendiente_sin_hora_de_llegada_conserva_los_nulos() {
        Reserva pendiente = reserva("RES-2026-00002", "APT-101", LocalDate.of(2027, 2, 10));

        Reserva vuelta = mapper.aDominio(mapper.aEntidad(pendiente));

        assertEquals(EstadoReserva.PENDIENTE, vuelta.getEstado());
        assertNull(vuelta.getHoraEstimadaLlegada());
        assertNull(vuelta.getHistorial().get(0).estadoAnterior());
    }

    @Test
    void un_titular_que_no_esta_entre_los_ocupantes_guardados_es_un_dato_inconsistente() {
        ReservaEntity valida = mapper.aEntidad(reservaConfirmada());
        ReservaEntity corrupta = new ReservaEntity(valida.getCodigo(), valida.getApartamentoId(),
                valida.getEstancia(), "999", valida.getCanalOrigen(), valida.getEstado(),
                valida.getHoraEstimadaLlegada(), valida.getVersionPolitica(), valida.getCreadaEn(),
                valida.getOcupantes(), valida.getHistorial(), valida.getDesglose(), valida.getIdentificadorExterno());

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> mapper.aDominio(corrupta));

        assertEquals("La reserva RES-2026-00001 guardada no incluye a su titular 999 entre sus ocupantes",
                error.getMessage());
    }
}
