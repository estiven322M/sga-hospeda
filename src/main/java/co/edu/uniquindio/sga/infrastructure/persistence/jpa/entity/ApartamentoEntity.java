package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import co.edu.uniquindio.sga.domain.valueobject.Caracteristica;
import co.edu.uniquindio.sga.domain.valueobject.EstadoOperativo;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "apartamento", indexes = @Index(name = "idx_apartamento_activo", columnList = "activo"))
public class ApartamentoEntity {

    @Id
    @Column(name = "identificacion", nullable = false, length = 20)
    private String identificacion;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "descripcion", nullable = false, length = 500)
    private String descripcion;

    @Column(name = "dormitorios", nullable = false)
    private int dormitorios;

    @Column(name = "capacidad", nullable = false)
    private int capacidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_operativo", nullable = false, length = 25)
    private EstadoOperativo estadoOperativo;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    /** L-06: lista ordenada de textos; la posición se guarda para devolverla en el mismo orden. */
    @ElementCollection
    @CollectionTable(name = "apartamento_dotacion", joinColumns = @JoinColumn(name = "apartamento_id"))
    @OrderColumn(name = "orden")
    @Column(name = "elemento", nullable = false, length = 60)
    @BatchSize(size = 50)
    private List<String> dotacion = new ArrayList<>();

    /** L-06: catálogo cerrado, guardado por nombre del enum (no por ordinal, que cambiaría al reordenarlo). */
    @ElementCollection
    @CollectionTable(name = "apartamento_caracteristica", joinColumns = @JoinColumn(name = "apartamento_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "caracteristica", nullable = false, length = 25)
    @BatchSize(size = 50)
    private Set<Caracteristica> caracteristicas = new LinkedHashSet<>();

    /** 7.3: las imágenes, en el orden en que se agregaron (la posición se guarda). Las retiradas se conservan: el retiro es lógico. */
    @ElementCollection
    @CollectionTable(name = "apartamento_imagen", joinColumns = @JoinColumn(name = "apartamento_id"))
    @OrderColumn(name = "orden")
    @BatchSize(size = 50)
    private List<ImagenApartamentoEmbeddable> imagenes = new ArrayList<>();

    protected ApartamentoEntity() {
    }

    public ApartamentoEntity(String identificacion, String nombre, String descripcion, int dormitorios, int capacidad,
                             List<String> dotacion, Collection<Caracteristica> caracteristicas,
                             EstadoOperativo estadoOperativo, boolean activo, List<ImagenApartamentoEmbeddable> imagenes) {
        this.identificacion = identificacion;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.dormitorios = dormitorios;
        this.capacidad = capacidad;
        this.dotacion = new ArrayList<>(dotacion);
        this.caracteristicas = new LinkedHashSet<>(caracteristicas);
        this.estadoOperativo = estadoOperativo;
        this.activo = activo;
        this.imagenes = new ArrayList<>(imagenes);
    }

    public String getIdentificacion() { return identificacion; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public int getDormitorios() { return dormitorios; }
    public int getCapacidad() { return capacidad; }
    public List<String> getDotacion() { return List.copyOf(dotacion); }
    public Set<Caracteristica> getCaracteristicas() { return Set.copyOf(caracteristicas); }
    public EstadoOperativo getEstadoOperativo() { return estadoOperativo; }
    public boolean isActivo() { return activo; }
    public List<ImagenApartamentoEmbeddable> getImagenes() { return List.copyOf(imagenes); }

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
