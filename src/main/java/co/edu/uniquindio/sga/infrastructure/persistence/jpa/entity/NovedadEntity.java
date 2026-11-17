package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import java.time.LocalDateTime;
import java.util.Objects;

import co.edu.uniquindio.sga.domain.valueobject.GravedadNovedad;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** Una novedad reportada sobre un apartamento (7.6). Solo se inserta: nunca se actualiza ni se borra. */
@Entity
@Table(name = "novedad", indexes = @Index(name = "idx_novedad_apartamento", columnList = "apartamento, fecha"))
public class NovedadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "apartamento", nullable = false, length = 30)
    private String apartamento;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "autor", nullable = false, length = 120)
    private String autor;

    @Column(name = "descripcion", nullable = false, length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "gravedad", nullable = false, length = 10)
    private GravedadNovedad gravedad;

    protected NovedadEntity() {
    }

    public NovedadEntity(String apartamento, LocalDateTime fecha, String autor, String descripcion, GravedadNovedad gravedad) {
        this.apartamento = apartamento;
        this.fecha = fecha;
        this.autor = autor;
        this.descripcion = descripcion;
        this.gravedad = gravedad;
    }

    public String getApartamento() { return apartamento; }
    public LocalDateTime getFecha() { return fecha; }
    public String getAutor() { return autor; }
    public String getDescripcion() { return descripcion; }
    public GravedadNovedad getGravedad() { return gravedad; }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof NovedadEntity otra && id != null && Objects.equals(id, otra.id));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
