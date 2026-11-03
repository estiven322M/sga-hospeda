package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDate;

@Embeddable
public record PagoEmbeddable(
        @Column(name = "medio", nullable = false, length = 60) String medio,
        @Column(name = "fecha", nullable = false) LocalDate fecha,
        @Column(name = "valor", nullable = false) long valor) {
}
