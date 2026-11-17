package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import co.edu.uniquindio.sga.domain.entity.Novedad;
import co.edu.uniquindio.sga.domain.repository.NovedadRepository;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.NovedadEntity;

/** Adaptador JPA del puerto de novedades: convierte entre la entidad JPA y el registro del dominio. */
@Repository
public class NovedadJpaRepository implements NovedadRepository {

    /** Con la misma fecha desempata el id: la que se registró después va primero. */
    private static final Sort MAS_RECIENTE_PRIMERO = Sort.by(Sort.Direction.DESC, "fecha", "id");

    private final NovedadJpaDataRepository datos;

    public NovedadJpaRepository(NovedadJpaDataRepository datos) {
        this.datos = datos;
    }

    @Override
    public Novedad registrar(Novedad novedad) {
        return aDominio(datos.save(new NovedadEntity(novedad.apartamento().valor(), novedad.fecha(), novedad.autor(),
                novedad.descripcion(), novedad.gravedad())));
    }

    @Override
    public Pagina<Novedad> listarPorApartamento(IdentificacionApartamento apartamento, SolicitudPagina pagina) {
        var resultado = datos.findByApartamento(apartamento.valor(), PaginacionJpa.solicitud(pagina, MAS_RECIENTE_PRIMERO));
        return PaginacionJpa.aDominio(resultado, pagina, NovedadJpaRepository::aDominio);
    }

    private static Novedad aDominio(NovedadEntity entidad) {
        return new Novedad(new IdentificacionApartamento(entidad.getApartamento()), entidad.getFecha(), entidad.getAutor(),
                entidad.getDescripcion(), entidad.getGravedad());
    }
}
