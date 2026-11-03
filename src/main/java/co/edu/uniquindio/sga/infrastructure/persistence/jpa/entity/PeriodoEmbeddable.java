package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDate;

@Embeddable
public record PeriodoEmbeddable(
        @Column(name = "desde", nullable = false) LocalDate desde,
        @Column(name = "hasta", nullable = false) LocalDate hasta) {
}
