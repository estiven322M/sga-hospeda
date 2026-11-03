package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "politica_cancelacion",
        indexes = @Index(name = "idx_politica_vigente_desde", columnList = "vigente_desde"))
public class PoliticaCancelacionEntity {

    @Id
    @Column(name = "numero_version", nullable = false)
    private int numeroVersion;

    @Column(name = "vigente_desde", nullable = false)
    private LocalDate vigenteDesde;

    @ElementCollection
    @CollectionTable(name = "politica_cancelacion_tramo", joinColumns = @JoinColumn(name = "numero_version"))
    @OrderColumn(name = "orden")
    private List<TramoRetencionEmbeddable> tramos = new ArrayList<>();

    protected PoliticaCancelacionEntity() {
    }

    public PoliticaCancelacionEntity(int numeroVersion, LocalDate vigenteDesde,
                                     List<TramoRetencionEmbeddable> tramos) {
        this.numeroVersion = numeroVersion;
        this.vigenteDesde = vigenteDesde;
        this.tramos = new ArrayList<>(tramos);
    }

    public int getNumeroVersion() { return numeroVersion; }
    public LocalDate getVigenteDesde() { return vigenteDesde; }
    public List<TramoRetencionEmbeddable> getTramos() { return List.copyOf(tramos); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PoliticaCancelacionEntity otra)) return false;
        return numeroVersion == otra.numeroVersion;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(numeroVersion);
    }
}
