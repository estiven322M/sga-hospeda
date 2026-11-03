package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Las colecciones usan carga por lotes del tamaño de una página: listar 10 reservas cuesta una consulta
 * por colección, no una por reserva.
 */
@Entity
@Table(name = "reserva",
        // RN-19: red de seguridad en la base de datos; los NULL (canales sin identificador) no chocan entre sí.
        uniqueConstraints = @UniqueConstraint(name = "uk_reserva_canal_identificador_externo",
                columnNames = {"canal_origen", "identificador_externo"}),
        indexes = {
        @Index(name = "idx_reserva_apartamento_estado", columnList = "apartamento_id, estado"),
        @Index(name = "idx_reserva_estado", columnList = "estado"),
        @Index(name = "idx_reserva_fecha_entrada", columnList = "fecha_entrada"),
        @Index(name = "idx_reserva_titular", columnList = "titular_documento"),
        @Index(name = "idx_reserva_creada_en", columnList = "creada_en")})
public class ReservaEntity {

    @Id
    @Column(name = "codigo", nullable = false, length = 15)
    private String codigo;

    @Column(name = "apartamento_id", nullable = false, length = 20)
    private String apartamentoId;

    @Embedded
    private EstanciaEmbeddable estancia;

    @Column(name = "titular_documento", nullable = false, length = 20)
    private String titularDocumento;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal_origen", nullable = false, length = 12)
    private CanalOrigen canalOrigen;

    @Column(name = "identificador_externo", length = 60)
    private String identificadorExterno;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 12)
    private EstadoReserva estado;

    @Column(name = "hora_estimada_llegada")
    private LocalTime horaEstimadaLlegada;

    @Column(name = "version_politica", nullable = false)
    private int versionPolitica;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    @ElementCollection
    @CollectionTable(name = "reserva_ocupante", joinColumns = @JoinColumn(name = "reserva_codigo"))
    @OrderColumn(name = "orden")
    @BatchSize(size = SolicitudPagina.TAMANIO)
    private List<OcupanteEmbeddable> ocupantes = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "reserva_evento", joinColumns = @JoinColumn(name = "reserva_codigo"))
    @OrderColumn(name = "orden")
    @BatchSize(size = SolicitudPagina.TAMANIO)
    private List<EventoReservaEmbeddable> historial = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "reserva_cargo_noche", joinColumns = @JoinColumn(name = "reserva_codigo"))
    @OrderColumn(name = "orden")
    @BatchSize(size = SolicitudPagina.TAMANIO)
    private List<CargoNocheEmbeddable> desglose = new ArrayList<>();

    protected ReservaEntity() {
    }

    public ReservaEntity(String codigo, String apartamentoId, EstanciaEmbeddable estancia, String titularDocumento,
                         CanalOrigen canalOrigen, EstadoReserva estado, LocalTime horaEstimadaLlegada,
                         int versionPolitica, LocalDateTime creadaEn, List<OcupanteEmbeddable> ocupantes,
                         List<EventoReservaEmbeddable> historial, List<CargoNocheEmbeddable> desglose,
                         String identificadorExterno) {
        this.codigo = codigo;
        this.apartamentoId = apartamentoId;
        this.estancia = estancia;
        this.titularDocumento = titularDocumento;
        this.canalOrigen = canalOrigen;
        this.identificadorExterno = identificadorExterno;
        this.estado = estado;
        this.horaEstimadaLlegada = horaEstimadaLlegada;
        this.versionPolitica = versionPolitica;
        this.creadaEn = creadaEn;
        this.ocupantes = new ArrayList<>(ocupantes);
        this.historial = new ArrayList<>(historial);
        this.desglose = new ArrayList<>(desglose);
    }

    public String getCodigo() { return codigo; }
    public String getApartamentoId() { return apartamentoId; }
    public EstanciaEmbeddable getEstancia() { return estancia; }
    public String getTitularDocumento() { return titularDocumento; }
    public CanalOrigen getCanalOrigen() { return canalOrigen; }
    public String getIdentificadorExterno() { return identificadorExterno; }
    public EstadoReserva getEstado() { return estado; }
    public LocalTime getHoraEstimadaLlegada() { return horaEstimadaLlegada; }
    public int getVersionPolitica() { return versionPolitica; }
    public LocalDateTime getCreadaEn() { return creadaEn; }
    public List<OcupanteEmbeddable> getOcupantes() { return List.copyOf(ocupantes); }
    public List<EventoReservaEmbeddable> getHistorial() { return List.copyOf(historial); }
    public List<CargoNocheEmbeddable> getDesglose() { return List.copyOf(desglose); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReservaEntity otra)) return false;
        return Objects.equals(codigo, otra.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(codigo);
    }
}
