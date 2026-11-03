package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.IdentificadorExterno;

/**
 * RN-19: "La combinación canal más identificador externo es única: un mismo mensaje recibido dos veces no crea dos
 * reservas." Una reserva no puede saber si otra ya trae el mismo identificador (es otro agregado), por eso la regla
 * vive en un servicio de dominio. También fija quién lleva identificador: solo los canales que lo exigen.
 * Debe evaluarse ANTES que la disponibilidad: un mensaje repetido chocaría con su propia reserva y se confundiría
 * con un conflicto (RN-18).
 */
public class RecepcionReservaExternaService {

    private final ReservaRepository reservaRepository;

    public RecepcionReservaExternaService(ReservaRepository reservaRepository) {
        this.reservaRepository = reservaRepository;
    }

    public void verificarRecepcion(CanalOrigen canal, IdentificadorExterno identificador) {
        if (canal == null) throw new ReglaDominioException("La reserva debe indicar su canal de origen");
        if (canal.exigeIdentificadorExterno() && identificador == null) {
            throw new ReglaDominioException("Una reserva del canal " + canal + " debe traer su identificador externo");
        }
        if (!canal.exigeIdentificadorExterno() && identificador != null) {
            throw new ReglaDominioException("Una reserva del canal " + canal + " no lleva identificador externo");
        }
        if (identificador == null) {
            return;
        }
        reservaRepository.buscarPorCanalEIdentificadorExterno(canal, identificador).ifPresent(existente -> {
            throw new ReglaDominioException("El mensaje ya fue recibido: el canal " + canal + " con el identificador "
                    + identificador.valor() + " ya creó la reserva " + existente.getCodigo().valor());
        });
    }
}
