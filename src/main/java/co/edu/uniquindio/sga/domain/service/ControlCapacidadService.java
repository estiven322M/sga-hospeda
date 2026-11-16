package co.edu.uniquindio.sga.domain.service;

import java.time.LocalDate;
import java.util.List;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;

/**
 * 7.3: "Cambiar la capacidad de un apartamento no afecta las reservas ya creadas, aunque las deje por encima de la nueva
 * capacidad. El sistema advierte al administrador y las lista." Este servicio encuentra esas reservas: las activas
 * (PENDIENTE, CONFIRMADA o EN_CURSO) cuya estancia aún no terminó y cuyo grupo supera la capacidad del apartamento. No
 * bloquea nada ni modifica ninguna reserva: solo informa.
 */
public class ControlCapacidadService {

    private final ReservaRepository reservas;

    public ControlCapacidadService(ReservaRepository reservas) {
        this.reservas = reservas;
    }

    public List<Reserva> reservasPorEncimaDeLaCapacidad(Apartamento apartamento, LocalDate hoy) {
        return reservas.buscarActivasPorApartamento(apartamento.getIdentificacion()).stream()
                .filter(reserva -> reserva.getEstancia().fechaSalida().isAfter(hoy))
                .filter(reserva -> !apartamento.admite(reserva.getOcupantes().size()))
                .toList();
    }
}
