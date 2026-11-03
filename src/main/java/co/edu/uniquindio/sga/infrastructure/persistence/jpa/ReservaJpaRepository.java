package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.repository.FiltroReservas;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.ReservaPersistenceMapper;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class ReservaJpaRepository implements ReservaRepository {

    // El código desempata reservas creadas en el mismo instante para que el orden sea estable.
    private static final Sort MAS_RECIENTE_PRIMERO = Sort.by(Sort.Direction.DESC, "creadaEn", "codigo");

    private final ReservaJpaDataRepository datos;
    private final ReservaPersistenceMapper mapper;

    public ReservaJpaRepository(ReservaJpaDataRepository datos, ReservaPersistenceMapper mapper) {
        this.datos = datos;
        this.mapper = mapper;
    }

    @Override
    public List<Reserva> buscarActivasPorApartamento(IdentificacionApartamento apartamento) {
        List<EstadoReserva> activas = Arrays.stream(EstadoReserva.values()).filter(EstadoReserva::esActiva).toList();
        return datos.findByApartamentoIdAndEstadoIn(apartamento.valor(), activas).stream()
                .map(mapper::aDominio).toList();
    }

    @Override
    @Transactional
    public Reserva guardar(Reserva reserva) {
        return mapper.aDominio(datos.save(mapper.aEntidad(reserva)));
    }

    @Override
    public Optional<Reserva> buscarPorCodigo(CodigoReserva codigo) {
        return datos.findByCodigo(codigo.valor()).map(mapper::aDominio);
    }

    @Override
    public Pagina<Reserva> buscar(FiltroReservas filtro, SolicitudPagina pagina) {
        return PaginacionJpa.aDominio(
                datos.findAll(ReservaEspecificaciones.segun(filtro),
                        PaginacionJpa.solicitud(pagina, MAS_RECIENTE_PRIMERO)),
                pagina, mapper::aDominio);
    }
}
