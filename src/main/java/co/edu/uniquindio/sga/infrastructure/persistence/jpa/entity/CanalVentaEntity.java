package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import java.util.Objects;

import co.edu.uniquindio.sga.domain.valueobject.TipoCanalVenta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Canal de venta externo (7.8). Guarda solo el HASH de la credencial (SHA-256), nunca su texto. La baja es lógica: {@code activo}. */
@Entity
@Table(name = "canal_venta")
public class CanalVentaEntity {

    @Id
    @Column(name = "codigo", nullable = false, length = 12)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 25)
    private TipoCanalVenta tipo;

    @Column(name = "credencial_hash", nullable = false, unique = true, length = 64)
    private String credencialHash;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    protected CanalVentaEntity() {
    }

    public CanalVentaEntity(String codigo, String nombre, TipoCanalVenta tipo, String credencialHash, boolean activo) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.tipo = tipo;
        this.credencialHash = credencialHash;
        this.activo = activo;
    }

    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public TipoCanalVenta getTipo() { return tipo; }
    public String getCredencialHash() { return credencialHash; }
    public boolean isActivo() { return activo; }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof CanalVentaEntity otro && Objects.equals(codigo, otro.codigo));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(codigo);
    }
}
