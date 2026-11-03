package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/** Último consecutivo entregado en cada año; respalda el formato RES-AAAA-NNNNN. */
@Entity
@Table(name = "contador_codigo_reserva")
public class ContadorCodigoReservaEntity {

    @Id
    @Column(name = "anio", nullable = false)
    private int anio;

    @Column(name = "ultimo", nullable = false)
    private int ultimo;

    protected ContadorCodigoReservaEntity() {
    }

    public ContadorCodigoReservaEntity(int anio) {
        this.anio = anio;
    }

    public int avanzar() {
        return ++ultimo;
    }

    public int getAnio() { return anio; }
    public int getUltimo() { return ultimo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContadorCodigoReservaEntity otro)) return false;
        return anio == otro.anio;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(anio);
    }
}
