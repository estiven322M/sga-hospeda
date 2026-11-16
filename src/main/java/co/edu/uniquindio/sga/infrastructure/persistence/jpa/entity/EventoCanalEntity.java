package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import java.time.LocalDateTime;
import java.util.Objects;

import co.edu.uniquindio.sga.domain.valueobject.OperacionCanal;
import co.edu.uniquindio.sga.domain.valueobject.ResultadoCanal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** Un mensaje intercambiado con un canal externo (bitácora, 7.8). Solo se inserta: nunca se actualiza ni se borra. */
@Entity
@Table(name = "evento_canal", indexes = {
        @Index(name = "idx_evento_canal_fecha", columnList = "registrado_en"),
        @Index(name = "idx_evento_canal_canal", columnList = "canal")})
public class EventoCanalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "registrado_en", nullable = false)
    private LocalDateTime registradoEn;

    @Column(name = "canal", nullable = false, length = 12)
    private String canal;

    @Enumerated(EnumType.STRING)
    @Column(name = "operacion", nullable = false, length = 30)
    private OperacionCanal operacion;

    @Column(name = "identificador_externo", length = 60)
    private String identificadorExterno;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", nullable = false, length = 20)
    private ResultadoCanal resultado;

    @Column(name = "codigo_http", nullable = false)
    private int codigoHttp;

    @Column(name = "detalle", length = 300)
    private String detalle;

    protected EventoCanalEntity() {
    }

    public EventoCanalEntity(LocalDateTime registradoEn, String canal, OperacionCanal operacion, String identificadorExterno,
                             ResultadoCanal resultado, int codigoHttp, String detalle) {
        this.registradoEn = registradoEn;
        this.canal = canal;
        this.operacion = operacion;
        this.identificadorExterno = identificadorExterno;
        this.resultado = resultado;
        this.codigoHttp = codigoHttp;
        this.detalle = detalle;
    }

    public LocalDateTime getRegistradoEn() { return registradoEn; }
    public String getCanal() { return canal; }
    public OperacionCanal getOperacion() { return operacion; }
    public String getIdentificadorExterno() { return identificadorExterno; }
    public ResultadoCanal getResultado() { return resultado; }
    public int getCodigoHttp() { return codigoHttp; }
    public String getDetalle() { return detalle; }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof EventoCanalEntity otro && id != null && Objects.equals(id, otro.id));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
