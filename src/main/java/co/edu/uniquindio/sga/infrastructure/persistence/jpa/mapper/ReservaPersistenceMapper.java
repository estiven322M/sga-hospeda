package co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper;

import co.edu.uniquindio.sga.domain.entity.Ocupante;
import co.edu.uniquindio.sga.domain.entity.Reserva;
import co.edu.uniquindio.sga.domain.valueobject.CargoNoche;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.EventoReserva;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.ValorCongelado;
import co.edu.uniquindio.sga.domain.valueobject.VersionPolitica;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.ConversionesMonetarias;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.CargoNocheEmbeddable;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.EventoReservaEmbeddable;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.ReservaEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReservaPersistenceMapper {

    private final EstanciaPersistenceMapper estancias;
    private final OcupantePersistenceMapper ocupantes;

    public ReservaPersistenceMapper(EstanciaPersistenceMapper estancias, OcupantePersistenceMapper ocupantes) {
        this.estancias = estancias;
        this.ocupantes = ocupantes;
    }

    public ReservaEntity aEntidad(Reserva reserva) {
        return new ReservaEntity(reserva.getCodigo().valor(), reserva.getApartamento().valor(),
                estancias.aEmbeddable(reserva.getEstancia()), reserva.getTitular().getDocumento().numero(),
                reserva.getCanalOrigen(), reserva.getEstado(), reserva.getHoraEstimadaLlegada(),
                reserva.getPolitica().numero(), reserva.getCreadaEn(),
                reserva.getOcupantes().stream().map(ocupantes::aEmbeddable).toList(),
                reserva.getHistorial().stream().map(ReservaPersistenceMapper::aEmbeddable).toList(),
                reserva.getValor().detalle().stream().map(ReservaPersistenceMapper::aEmbeddable).toList());
    }

    public Reserva aDominio(ReservaEntity entidad) {
        List<Ocupante> grupo = entidad.getOcupantes().stream().map(ocupantes::aDominio).toList();
        return Reserva.reconstruir(new CodigoReserva(entidad.getCodigo()),
                new IdentificacionApartamento(entidad.getApartamentoId()),
                estancias.aDominio(entidad.getEstancia()), titularEntre(grupo, entidad), grupo,
                entidad.getCanalOrigen(), valorDe(entidad), new VersionPolitica(entidad.getVersionPolitica()),
                entidad.getCreadaEn(), entidad.getEstado(), entidad.getHoraEstimadaLlegada(),
                entidad.getHistorial().stream().map(ReservaPersistenceMapper::aDominio).toList());
    }

    private static Ocupante titularEntre(List<Ocupante> grupo, ReservaEntity entidad) {
        return grupo.stream()
                .filter(o -> o.getDocumento().numero().equals(entidad.getTitularDocumento()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("La reserva " + entidad.getCodigo()
                        + " guardada no incluye a su titular " + entidad.getTitularDocumento()
                        + " entre sus ocupantes"));
    }

    private static ValorCongelado valorDe(ReservaEntity entidad) {
        return new ValorCongelado(entidad.getDesglose().stream().map(ReservaPersistenceMapper::aDominio).toList());
    }

    private static EventoReservaEmbeddable aEmbeddable(EventoReserva evento) {
        return new EventoReservaEmbeddable(evento.momento(), evento.accion(), evento.autor(),
                evento.estadoAnterior(), evento.estadoNuevo(), evento.observacion());
    }

    private static EventoReserva aDominio(EventoReservaEmbeddable embeddable) {
        return new EventoReserva(embeddable.momento(), embeddable.accion(), embeddable.autor(),
                embeddable.estadoAnterior(), embeddable.estadoNuevo(), embeddable.observacion());
    }

    private static CargoNocheEmbeddable aEmbeddable(CargoNoche cargo) {
        return new CargoNocheEmbeddable(cargo.noche(), cargo.temporada(),
                ConversionesMonetarias.aPesos(cargo.tarifaPorOcupante()), cargo.ocupantesFacturables());
    }

    private static CargoNoche aDominio(CargoNocheEmbeddable embeddable) {
        return new CargoNoche(embeddable.noche(), embeddable.temporada(),
                ConversionesMonetarias.aDinero(embeddable.tarifaPorOcupante()), embeddable.ocupantesFacturables());
    }
}
