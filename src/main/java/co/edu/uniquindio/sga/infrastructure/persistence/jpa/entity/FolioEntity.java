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

/** Un folio por reserva: se identifica por el código de la reserva, que referencia sin relación JPA. */
@Entity
@Table(name = "folio")
public class FolioEntity {

    @Id
    @Column(name = "reserva_codigo", nullable = false, length = 15)
    private String reservaCodigo;

    @Column(name = "cerrado", nullable = false)
    private boolean cerrado;

    @Column(name = "autorizacion_cierre", length = 255)
    private String autorizacionCierre;

    @ElementCollection
    @CollectionTable(name = "folio_cargo", joinColumns = @JoinColumn(name = "reserva_codigo"))
    @OrderColumn(name = "orden")
    private List<MovimientoEmbeddable> cargos = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "folio_ajuste", joinColumns = @JoinColumn(name = "reserva_codigo"))
    @OrderColumn(name = "orden")
    private List<MovimientoEmbeddable> ajustes = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "folio_pago", joinColumns = @JoinColumn(name = "reserva_codigo"))
    @OrderColumn(name = "orden")
    private List<PagoEmbeddable> pagos = new ArrayList<>();

    protected FolioEntity() {
    }

    public FolioEntity(String reservaCodigo, boolean cerrado, String autorizacionCierre,
                       List<MovimientoEmbeddable> cargos, List<MovimientoEmbeddable> ajustes,
                       List<PagoEmbeddable> pagos) {
        this.reservaCodigo = reservaCodigo;
        this.cerrado = cerrado;
        this.autorizacionCierre = autorizacionCierre;
        this.cargos = new ArrayList<>(cargos);
        this.ajustes = new ArrayList<>(ajustes);
        this.pagos = new ArrayList<>(pagos);
    }

    public String getReservaCodigo() { return reservaCodigo; }
    public boolean isCerrado() { return cerrado; }
    public String getAutorizacionCierre() { return autorizacionCierre; }
    public List<MovimientoEmbeddable> getCargos() { return List.copyOf(cargos); }
    public List<MovimientoEmbeddable> getAjustes() { return List.copyOf(ajustes); }
    public List<PagoEmbeddable> getPagos() { return List.copyOf(pagos); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FolioEntity otro)) return false;
        return Objects.equals(reservaCodigo, otro.reservaCodigo);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(reservaCodigo);
    }
}
