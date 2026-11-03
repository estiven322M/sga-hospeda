package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.util.Objects;

/** El dominio no da identidad al bloqueo, por eso esta entidad usa un identificador generado. */
@Entity
@Table(name = "bloqueo", indexes = @Index(name = "idx_bloqueo_apartamento", columnList = "apartamento_id"))
public class BloqueoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "apartamento_id", nullable = false, length = 20)
    private String apartamentoId;

    @Embedded
    private EstanciaEmbeddable estancia;

    @Column(name = "motivo", nullable = false, length = 255)
    private String motivo;

    protected BloqueoEntity() {
    }

    public BloqueoEntity(String apartamentoId, EstanciaEmbeddable estancia, String motivo) {
        this.apartamentoId = apartamentoId;
        this.estancia = estancia;
        this.motivo = motivo;
    }

    public String getApartamentoId() { return apartamentoId; }
    public EstanciaEmbeddable getEstancia() { return estancia; }
    public String getMotivo() { return motivo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BloqueoEntity otro)) return false;
        return id != null && Objects.equals(id, otro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
