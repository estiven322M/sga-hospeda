package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.repository.FolioRepository;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.FolioPersistenceMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class FolioJpaRepository implements FolioRepository {

    private final FolioJpaDataRepository datos;
    private final FolioPersistenceMapper mapper;

    public FolioJpaRepository(FolioJpaDataRepository datos, FolioPersistenceMapper mapper) {
        this.datos = datos;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Folio guardar(Folio folio) {
        return mapper.aDominio(datos.save(mapper.aEntidad(folio)));
    }

    @Override
    public Optional<Folio> buscarPorReserva(CodigoReserva reserva) {
        return datos.findById(reserva.valor()).map(mapper::aDominio);
    }
}
