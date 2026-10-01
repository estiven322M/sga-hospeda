package co.edu.uniquindio.sga.domain.service;

import java.util.List;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Bloqueo;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.BloqueoRepository;
import co.edu.uniquindio.sga.domain.repository.ConfiguracionAlojamiento;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.TiempoPreparacion;

public class DisponibilidadApartamentoService {

	private final ReservaRepository reservaRepository;
	private final BloqueoRepository bloqueoRepository;
	private final ConfiguracionAlojamiento configuracion;

	public DisponibilidadApartamentoService(ReservaRepository reservaRepository, BloqueoRepository bloqueoRepository,
			ConfiguracionAlojamiento configuracion) {
		this.reservaRepository = reservaRepository;
		this.bloqueoRepository = bloqueoRepository;
		this.configuracion = configuracion;
	}

	public void verificarDisponible(Apartamento apartamento, Estancia estancia, int totalOcupantes,
			CodigoReserva reservaExcluida) {

		if (!apartamento.isActivo()) {
			throw new ReglaDominioException("El apartamento no está disponible para la venta");
		}

// RN-02: la capacidad es un tope rígido
		if (!apartamento.admite(totalOcupantes)) {
			throw new ReglaDominioException("El número de ocupantes excede la capacidad del apartamento");
		}

		List<Reserva> activas = reservaRepository.buscarActivasPorApartamento(apartamento.getIdentificacion());
		TiempoPreparacion tiempoPreparacion = configuracion.tiempoPreparacion();

		for (Reserva otra : activas) {
			if (otra.getCodigo().equals(reservaExcluida)) {
				continue; // Al modificar, la reserva no compite consigo misma
			}
// RN-01: solapamiento con otra reserva activa
			if (otra.getEstancia().seSolapaCon(estancia)) {
				throw new ReglaDominioException("El apartamento ya tiene una reserva activa que solapa esas noches");
			}
// RN-20: tiempo de preparación entre reservas
			if (!tiempoPreparacion.permiteEntradaElMismoDia()
					&& otra.getEstancia().fechaSalida().equals(estancia.fechaEntrada())) {
				throw new ReglaDominioException("No se respeta el tiempo de preparación configurado entre reservas");
			}
		}

// RN-07: solapamiento con bloqueos vigentes
		List<Bloqueo> bloqueos = bloqueoRepository.buscarVigentesPorApartamento(apartamento.getIdentificacion());
		for (Bloqueo bloqueo : bloqueos) {
			if (bloqueo.afecta(estancia)) {
				throw new ReglaDominioException(
						"El apartamento tiene un bloqueo vigente en las fechas solicitadas: " + bloqueo.getMotivo());
			}
		}
	}

}
