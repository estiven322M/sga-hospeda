package co.edu.uniquindio.sga.infrastructure.persistence.jpa;

import java.time.LocalDate;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.domain.entity.EventoCanal;
import co.edu.uniquindio.sga.domain.repository.BitacoraCanalRepository;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.EventoCanalEntity;

/**
 * Adaptador JPA de la bitácora del canal. Cada registro se guarda en su PROPIA transacción (REQUIRES_NEW): la constancia de un
 * mensaje rechazado debe quedar aunque la operación de negocio que lo rechazó haya revertido la suya.
 */
@Repository
public class BitacoraCanalJpaRepository implements BitacoraCanalRepository {

    private static final Sort MAS_RECIENTE_PRIMERO = Sort.by(Sort.Direction.DESC, "registradoEn", "id");

    private final EventoCanalJpaDataRepository datos;

    public BitacoraCanalJpaRepository(EventoCanalJpaDataRepository datos) {
        this.datos = datos;
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void registrar(EventoCanal e) {
        datos.save(new EventoCanalEntity(e.registradoEn(), e.canal(), e.operacion(), e.identificadorExterno(), e.resultado(),
                e.codigoHttp(), e.detalle()));
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<EventoCanal> listar(String canal, LocalDate desde, LocalDate hasta, SolicitudPagina pagina) {
        var pagi = datos.buscar(canal == null || canal.isBlank() ? null : canal.trim().toUpperCase(),
                desde == null ? null : desde.atStartOfDay(),
                hasta == null ? null : hasta.plusDays(1).atStartOfDay(),
                PaginacionJpa.solicitud(pagina, MAS_RECIENTE_PRIMERO));
        return PaginacionJpa.aDominio(pagi, pagina, e -> new EventoCanal(e.getRegistradoEn(), e.getCanal(), e.getOperacion(),
                e.getIdentificadorExterno(), e.getResultado(), e.getCodigoHttp(), e.getDetalle()));
    }
}
