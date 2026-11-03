package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.entity.Bloqueo;
import co.edu.uniquindio.sga.domain.repository.BloqueoRepository;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.BloqueoPersistenceMapper;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * El bloqueo es inmutable y el dominio no le da identidad: guardar siempre inserta una fila nueva.
 * Hoy no tiene fecha de baja, así que todos los bloqueos de un apartamento son vigentes.
 */
@Repository
@Transactional(readOnly = true)
public class BloqueoJpaRepository implements BloqueoRepository {

    private static final Sort CRONOLOGICO = Sort.by("estancia.fechaEntrada", "id");

    private final BloqueoJpaDataRepository datos;
    private final BloqueoPersistenceMapper mapper;

    public BloqueoJpaRepository(BloqueoJpaDataRepository datos, BloqueoPersistenceMapper mapper) {
        this.datos = datos;
        this.mapper = mapper;
    }

    @Override
    public List<Bloqueo> buscarVigentesPorApartamento(IdentificacionApartamento apartamento) {
        return datos.findByApartamentoIdOrderByEstanciaFechaEntradaAscIdAsc(apartamento.valor()).stream()
                .map(mapper::aDominio).toList();
    }

    @Override
    @Transactional
    public Bloqueo guardar(Bloqueo bloqueo) {
        return mapper.aDominio(datos.save(mapper.aEntidad(bloqueo)));
    }

    @Override
    public Pagina<Bloqueo> listarPorApartamento(IdentificacionApartamento apartamento, SolicitudPagina pagina) {
        return PaginacionJpa.aDominio(
                datos.findByApartamentoId(apartamento.valor(), PaginacionJpa.solicitud(pagina, CRONOLOGICO)),
                pagina, mapper::aDominio);
    }
}
