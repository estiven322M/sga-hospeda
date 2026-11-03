package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDate;

@Embeddable
public record OcupanteEmbeddable(
        @Column(name = "documento", nullable = false, length = 20) String documento,
        @Column(name = "nombre", nullable = false, length = 120) String nombre,
        @Column(name = "fecha_nacimiento", nullable = false) LocalDate fechaNacimiento) {
}
