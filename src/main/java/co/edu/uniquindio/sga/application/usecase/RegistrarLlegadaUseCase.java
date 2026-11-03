package co.edu.uniquindio.sga.application.usecase;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.application.exception.ReservaNoEncontradaException;
import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;

/**
 * CU-06 Registrar llegada. Modifica dos agregados (Reserva y Apartamento) en una misma transacción, que es la
 * que garantiza que no queden desincronizados. Las reglas las aplica el dominio:
 *  - Reserva.registrarLlegada: reserva CONFIRMADA y fecha de entrada alcanzada (RN-10, RN-08);
 *  - Apartamento.marcarOcupado: solo un apartamento PREPARADO recibe un grupo (RN-11).
 * Si cualquiera falla, la transacción se revierte y no se guarda ninguno de los dos cambios.
 */
@Service
public class RegistrarLlegadaUseCase {

    private final ReservaRepository reservaRepository;
    private final ApartamentoRepository apartamentoRepository;

    public RegistrarLlegadaUseCase(ReservaRepository reservaRepository, ApartamentoRepository apartamentoRepository) {
        this.reservaRepository = reservaRepository;
        this.apartamentoRepository = apartamentoRepository;
    }

    @Transactional
    public Reserva ejecutar(CodigoReserva codigo, String autor, LocalDateTime ahora) {
        Reserva reserva = reservaRepository.buscarPorCodigo(codigo)
                .orElseThrow(() -> new ReservaNoEncontradaException(codigo));
        Apartamento apartamento = apartamentoRepository.buscarPorIdentificacion(reserva.getApartamento())
                .orElseThrow(() -> new ApartamentoNoEncontradoException(reserva.getApartamento()));

        reserva.registrarLlegada(autor, ahora);
        apartamento.marcarOcupado();

        apartamentoRepository.guardar(apartamento);
        return reservaRepository.guardar(reserva);
    }
}
