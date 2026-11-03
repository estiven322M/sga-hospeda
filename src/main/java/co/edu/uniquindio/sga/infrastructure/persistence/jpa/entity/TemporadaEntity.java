package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "temporada")
public class TemporadaEntity {

    @Id
    @Column(name = "nombre", nullable = false, length = 60)
    private String nombre;

    @Column(name = "base", nullable = false)
    private boolean base;

    @ElementCollection
    @CollectionTable(name = "temporada_periodo", joinColumns = @JoinColumn(name = "temporada_nombre"))
    @OrderColumn(name = "orden")
    private List<PeriodoEmbeddable> periodos = new ArrayList<>();

    protected TemporadaEntity() {
    }

    public TemporadaEntity(String nombre, boolean base, List<PeriodoEmbeddable> periodos) {
        this.nombre = nombre;
        this.base = base;
        this.periodos = new ArrayList<>(periodos);
    }

    public String getNombre() { return nombre; }
    public boolean isBase() { return base; }
    public List<PeriodoEmbeddable> getPeriodos() { return List.copyOf(periodos); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TemporadaEntity otra)) return false;
        return Objects.equals(nombre, otra.nombre);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(nombre);
    }
}
