package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.repository.DatosReportesRepository;
import co.edu.uniquindio.sga.domain.valueobject.Pago;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.FolioPersistenceMapper;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.ReservaPersistenceMapper;

/** Adaptador JPA de los datos de los reportes: lee reservas y folios y los devuelve como dominio, sin calcular. */
@Repository
public class DatosReportesJpaRepository implements DatosReportesRepository {

    private final ReservaJpaDataRepository reservas;
    private final FolioJpaDataRepository folios;
    private final ReservaPersistenceMapper reservaMapper;
    private final FolioPersistenceMapper folioMapper;

    public DatosReportesJpaRepository(ReservaJpaDataRepository reservas, FolioJpaDataRepository folios,
                                      ReservaPersistenceMapper reservaMapper, FolioPersistenceMapper folioMapper) {
        this.reservas = reservas;
        this.folios = folios;
        this.reservaMapper = reservaMapper;
        this.folioMapper = folioMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reserva> listarReservas() {
        return reservas.findAll().stream().map(reservaMapper::aDominio).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pago> listarPagos() {
        return folios.findAll().stream().flatMap(folio -> folioMapper.aDominio(folio).obtenerPagos().stream()).toList();
    }
}
