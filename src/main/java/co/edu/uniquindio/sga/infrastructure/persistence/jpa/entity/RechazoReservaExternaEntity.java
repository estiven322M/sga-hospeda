package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Constancia de una reserva externa rechazada (RN-18). El dominio no le da identidad, por eso usa un identificador
 * generado. Sus datos no se modifican nunca; lo único que se escribe después es su REVISIÓN (7.8). Nunca se borra.
 */
@Entity
@Table(name = "rechazo_reserva_externa",
        indexes = @Index(name = "idx_rechazo_externo_fecha", columnList = "rechazada_en"))
public class RechazoReservaExternaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal_origen", nullable = false, length = 12)
    private CanalOrigen canalOrigen;

    @Column(name = "identificador_externo", nullable = false, length = 60)
    private String identificadorExterno;

    @Column(name = "apartamento_id", nullable = false, length = 20)
    private String apartamentoId;

    @Embedded
    private EstanciaEmbeddable estancia;

    @Column(name = "motivo", nullable = false, length = 500)
    private String motivo;

    @Column(name = "rechazada_en", nullable = false)
    private LocalDateTime rechazadaEn;

    /** 7.8: el administrador lo revisó (con su nota). */
    @Column(name = "revisado", nullable = false)
    private boolean revisado;

    @Column(name = "nota_revision", length = 500)
    private String notaRevision;

    @Column(name = "revisado_por", length = 100)
    private String revisadoPor;

    @Column(name = "revisado_en")
    private LocalDateTime revisadoEn;

    protected RechazoReservaExternaEntity() {
    }

    public RechazoReservaExternaEntity(Long id, CanalOrigen canalOrigen, String identificadorExterno, String apartamentoId,
                                       EstanciaEmbeddable estancia, String motivo, LocalDateTime rechazadaEn, boolean revisado,
                                       String notaRevision, String revisadoPor, LocalDateTime revisadoEn) {
        this(canalOrigen, identificadorExterno, apartamentoId, estancia, motivo, rechazadaEn);
        this.id = id;
        this.revisado = revisado;
        this.notaRevision = notaRevision;
        this.revisadoPor = revisadoPor;
        this.revisadoEn = revisadoEn;
    }

    public RechazoReservaExternaEntity(CanalOrigen canalOrigen, String identificadorExterno, String apartamentoId,
                                       EstanciaEmbeddable estancia, String motivo, LocalDateTime rechazadaEn) {
        this.canalOrigen = canalOrigen;
        this.identificadorExterno = identificadorExterno;
        this.apartamentoId = apartamentoId;
        this.estancia = estancia;
        this.motivo = motivo;
        this.rechazadaEn = rechazadaEn;
    }

    public Long getId() { return id; }
    public boolean isRevisado() { return revisado; }
    public String getNotaRevision() { return notaRevision; }
    public String getRevisadoPor() { return revisadoPor; }
    public LocalDateTime getRevisadoEn() { return revisadoEn; }
    public CanalOrigen getCanalOrigen() { return canalOrigen; }
    public String getIdentificadorExterno() { return identificadorExterno; }
    public String getApartamentoId() { return apartamentoId; }
    public EstanciaEmbeddable getEstancia() { return estancia; }
    public String getMotivo() { return motivo; }
    public LocalDateTime getRechazadaEn() { return rechazadaEn; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RechazoReservaExternaEntity otro)) return false;
        return id != null && Objects.equals(id, otro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
