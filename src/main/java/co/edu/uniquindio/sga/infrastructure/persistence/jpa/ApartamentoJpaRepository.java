package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper.ApartamentoPersistenceMapper;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class ApartamentoJpaRepository implements ApartamentoRepository {

    private static final Sort POR_IDENTIFICACION = Sort.by("identificacion");

    private final ApartamentoJpaDataRepository datos;
    private final ApartamentoPersistenceMapper mapper;

    public ApartamentoJpaRepository(ApartamentoJpaDataRepository datos, ApartamentoPersistenceMapper mapper) {
        this.datos = datos;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Apartamento guardar(Apartamento apartamento) {
        return mapper.aDominio(datos.save(mapper.aEntidad(apartamento)));
    }

    @Override
    public Optional<Apartamento> buscarPorIdentificacion(IdentificacionApartamento identificacion) {
        return datos.findById(identificacion.valor()).map(mapper::aDominio);
    }

    @Override
    public List<Apartamento> listarActivos() {
        return datos.findByActivoTrueOrderByIdentificacion().stream().map(mapper::aDominio).toList();
    }

    @Override
    public Pagina<Apartamento> listarActivos(SolicitudPagina pagina) {
        return PaginacionJpa.aDominio(
                datos.findByActivoTrue(PaginacionJpa.solicitud(pagina, POR_IDENTIFICACION)),
                pagina, mapper::aDominio);
    }

    @Override
    public Pagina<Apartamento> listarTodos(SolicitudPagina pagina) {
        return PaginacionJpa.aDominio(
                datos.findAll(PaginacionJpa.solicitud(pagina, POR_IDENTIFICACION)),
                pagina, mapper::aDominio);
    }
}
