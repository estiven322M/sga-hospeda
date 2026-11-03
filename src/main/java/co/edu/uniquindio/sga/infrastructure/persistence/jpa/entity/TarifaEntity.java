package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "tarifa")
public class TarifaEntity {

    @EmbeddedId
    private TarifaId id;

    @Column(name = "valor_por_ocupante_noche", nullable = false)
    private long valorPorOcupanteNoche;

    protected TarifaEntity() {
    }

    public TarifaEntity(TarifaId id, long valorPorOcupanteNoche) {
        this.id = id;
        this.valorPorOcupanteNoche = valorPorOcupanteNoche;
    }

    public TarifaId getId() { return id; }
    public long getValorPorOcupanteNoche() { return valorPorOcupanteNoche; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TarifaEntity otra)) return false;
        return Objects.equals(id, otra.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
