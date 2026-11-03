package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import co.edu.uniquindio.sga.domain.valueobject.EstadoOperativo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "apartamento", indexes = @Index(name = "idx_apartamento_activo", columnList = "activo"))
public class ApartamentoEntity {

    @Id
    @Column(name = "identificacion", nullable = false, length = 20)
    private String identificacion;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "dormitorios", nullable = false)
    private int dormitorios;

    @Column(name = "capacidad", nullable = false)
    private int capacidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_operativo", nullable = false, length = 25)
    private EstadoOperativo estadoOperativo;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    protected ApartamentoEntity() {
    }

    public ApartamentoEntity(String identificacion, String nombre, int dormitorios, int capacidad,
                             EstadoOperativo estadoOperativo, boolean activo) {
        this.identificacion = identificacion;
        this.nombre = nombre;
        this.dormitorios = dormitorios;
        this.capacidad = capacidad;
        this.estadoOperativo = estadoOperativo;
        this.activo = activo;
    }

    public String getIdentificacion() { return identificacion; }
    public String getNombre() { return nombre; }
    public int getDormitorios() { return dormitorios; }
    public int getCapacidad() { return capacidad; }
    public EstadoOperativo getEstadoOperativo() { return estadoOperativo; }
    public boolean isActivo() { return activo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ApartamentoEntity otro)) return false;
        return Objects.equals(identificacion, otro.identificacion);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(identificacion);
    }
}
