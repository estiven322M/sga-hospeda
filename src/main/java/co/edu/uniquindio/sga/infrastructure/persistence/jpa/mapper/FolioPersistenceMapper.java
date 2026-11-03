package co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper;

import co.edu.uniquindio.sga.domain.entity.Folio;
import co.edu.uniquindio.sga.domain.valueobject.Ajuste;
import co.edu.uniquindio.sga.domain.valueobject.Cargo;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.Pago;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.ConversionesMonetarias;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.FolioEntity;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.MovimientoEmbeddable;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.PagoEmbeddable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FolioPersistenceMapper {

    public FolioEntity aEntidad(Folio folio) {
        return new FolioEntity(folio.getReserva().valor(), folio.estaCerrado(), folio.getAutorizacionCierre(),
                folio.obtenerCargos().stream().map(c -> movimiento(c.concepto(), c.valor())).toList(),
                folio.obtenerAjustes().stream().map(a -> movimiento(a.concepto(), a.valor())).toList(),
                folio.obtenerPagos().stream().map(FolioPersistenceMapper::aEmbeddable).toList());
    }

    /** El folio no tiene setters: se reproducen sus movimientos en el orden guardado y, al final, el cierre. */
    public Folio aDominio(FolioEntity entidad) {
        List<Cargo> cargos = entidad.getCargos().stream()
                .map(m -> new Cargo(m.concepto(), ConversionesMonetarias.aDinero(m.valor()))).toList();
        if (cargos.isEmpty()) {
            throw new IllegalStateException("El folio de la reserva " + entidad.getReservaCodigo()
                    + " guardado no tiene el cargo de alojamiento con el que se abre");
        }
        Folio folio = new Folio(new CodigoReserva(entidad.getReservaCodigo()), cargos.get(0));
        cargos.stream().skip(1).forEach(folio::registrarCargo);
        entidad.getAjustes().forEach(m ->
                folio.registrarAjuste(new Ajuste(m.concepto(), ConversionesMonetarias.aDinero(m.valor()))));
        entidad.getPagos().forEach(p -> folio.registrarPago(new Pago(p.medio(), p.fecha(),
                ConversionesMonetarias.aDinero(p.valor()))));
        if (entidad.isCerrado()) {
            folio.cerrar(entidad.getAutorizacionCierre());
        }
        return folio;
    }

    private static MovimientoEmbeddable movimiento(String concepto, Dinero valor) {
        return new MovimientoEmbeddable(concepto, ConversionesMonetarias.aPesos(valor));
    }

    private static PagoEmbeddable aEmbeddable(Pago pago) {
        return new PagoEmbeddable(pago.medio(), pago.fecha(), ConversionesMonetarias.aPesos(pago.valor()));
    }
}
