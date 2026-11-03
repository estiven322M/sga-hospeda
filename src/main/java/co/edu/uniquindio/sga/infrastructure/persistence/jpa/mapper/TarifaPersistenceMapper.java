package co.edu.uniquindio.sga.infrastructure.persistence.jpa.mapper;

import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Tarifa;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.ConversionesMonetarias;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.TarifaEntity;
import co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity.TarifaId;
import org.springframework.stereotype.Component;

@Component
public class TarifaPersistenceMapper {

    public TarifaEntity aEntidad(IdentificacionApartamento apartamento, Tarifa tarifa) {
        return new TarifaEntity(new TarifaId(apartamento.valor(), tarifa.temporada()),
                ConversionesMonetarias.aPesos(tarifa.valorPorOcupanteNoche()));
    }

    public Tarifa aDominio(TarifaEntity entidad) {
        return new Tarifa(entidad.getId().temporada(),
                ConversionesMonetarias.aDinero(entidad.getValorPorOcupanteNoche()));
    }
}
