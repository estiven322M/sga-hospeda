package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Correo opcional que se da al crear una reserva para avisar al titular sin cuenta (10.3.2-A). Una fila por reserva. */
@Entity
@Table(name = "contacto_reserva")
public class ContactoReservaEntity {

    @Id
    @Column(name = "codigo_reserva", nullable = false, length = 20)
    private String codigoReserva;

    @Column(name = "correo", nullable = false, length = 120)
    private String correo;

    protected ContactoReservaEntity() {
    }

    public ContactoReservaEntity(String codigoReserva, String correo) {
        this.codigoReserva = codigoReserva;
        this.correo = correo;
    }

    public String getCodigoReserva() { return codigoReserva; }
    public String getCorreo() { return correo; }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof ContactoReservaEntity otro && Objects.equals(codigoReserva, otro.codigoReserva));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(codigoReserva);
    }
}
