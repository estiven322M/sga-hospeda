package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.FolioNoEncontradoException;
import co.edu.uniquindio.sga.application.exception.ReservaNoEncontradaException;
import co.edu.uniquindio.sga.application.exception.ServicioAdicionalNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.entity.ServicioAdicional;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.repository.ServicioAdicionalRepository;
import co.edu.uniquindio.sga.domain.service.CargoServicioAdicionalService;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;

/**
 * L-17, 7.7: registrar un cargo de servicio adicional en el folio de una reserva. Orquesta: busca la reserva, su folio y
 * el servicio (404) y delega en el servicio de dominio, que exige reserva CONFIRMADA o EN_CURSO, servicio activo que
 * genere cargo y folio abierto (409).
 */
@Service
public class RegistrarCargoServicioUseCase {

    private final ReservaRepository reservas;
    private final FolioRepository folios;
    private final ServicioAdicionalRepository servicios;
    private final CargoServicioAdicionalService cargoServicio;

    public RegistrarCargoServicioUseCase(ReservaRepository reservas, FolioRepository folios,
                                         ServicioAdicionalRepository servicios, CargoServicioAdicionalService cargoServicio) {
        this.reservas = reservas;
        this.folios = folios;
        this.servicios = servicios;
        this.cargoServicio = cargoServicio;
    }

    @Transactional
    public Folio ejecutar(CodigoReserva codigo, String codigoServicio, int cantidad) {
        Reserva reserva = reservas.buscarPorCodigo(codigo).orElseThrow(() -> new ReservaNoEncontradaException(codigo));
        Folio folio = folios.buscarPorReserva(codigo).orElseThrow(() -> new FolioNoEncontradoException(codigo));
        ServicioAdicional servicio = servicios.buscarPorCodigo(codigoServicio.trim().toUpperCase())
                .orElseThrow(() -> new ServicioAdicionalNoEncontradoException(codigoServicio));
        cargoServicio.registrar(reserva, folio, servicio, cantidad);
        return folios.guardar(folio);
    }
}
