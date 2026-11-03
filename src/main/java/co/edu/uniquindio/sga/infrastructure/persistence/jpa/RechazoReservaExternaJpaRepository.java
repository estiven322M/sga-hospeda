package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.entity.RechazoReservaExterna;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.RechazoReservaExternaRepository;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.RechazoReservaExternaPersistenceMapper;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Los rechazos solo se insertan: no hay actualización ni borrado. El id desempata rechazos del mismo instante. */
@Repository
public class RechazoReservaExternaJpaRepository implements RechazoReservaExternaRepository {

    private static final Sort MAS_RECIENTE_PRIMERO = Sort.by(Sort.Direction.DESC, "rechazadaEn", "id");

    private final RechazoReservaExternaJpaDataRepository datos;
    private final RechazoReservaExternaPersistenceMapper mapper;

    public RechazoReservaExternaJpaRepository(RechazoReservaExternaJpaDataRepository datos,
                                              RechazoReservaExternaPersistenceMapper mapper) {
        this.datos = datos;
        this.mapper = mapper;
    }

    /** Se une a la transacción del caso de uso, que no se revierte por la regla de dominio (RN-18). */
    @Override
    @Transactional
    public RechazoReservaExterna guardar(RechazoReservaExterna rechazo) {
        return mapper.aDominio(datos.save(mapper.aEntidad(rechazo)));
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<RechazoReservaExterna> listar(SolicitudPagina pagina) {
        return PaginacionJpa.aDominio(datos.findAll(PaginacionJpa.solicitud(pagina, MAS_RECIENTE_PRIMERO)),
                pagina, mapper::aDominio);
    }
}
