package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Servicio adicional (L-17). El valor se guarda en pesos COP enteros (regla 14). La baja es lógica: {@code activo}. */
@Entity
@Table(name = "servicio_adicional")
public class ServicioAdicionalEntity {

    @Id
    @Column(name = "codigo", nullable = false, length = 30)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @Column(name = "descripcion", nullable = false, length = 300)
    private String descripcion;

    @Column(name = "genera_cargo", nullable = false)
    private boolean generaCargo;

    @Column(name = "valor", nullable = false)
    private long valor;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    protected ServicioAdicionalEntity() {
    }

    public ServicioAdicionalEntity(String codigo, String nombre, String descripcion, boolean generaCargo, long valor,
                                   boolean activo) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.generaCargo = generaCargo;
        this.valor = valor;
        this.activo = activo;
    }

    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public boolean isGeneraCargo() { return generaCargo; }
    public long getValor() { return valor; }
    public boolean isActivo() { return activo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServicioAdicionalEntity otro)) return false;
        return Objects.equals(codigo, otro.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(codigo);
    }
}
