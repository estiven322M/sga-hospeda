package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDate;

@Embeddable
public record CargoNocheEmbeddable(
        @Column(name = "noche", nullable = false) LocalDate noche,
        @Column(name = "temporada", nullable = false, length = 60) String temporada,
        @Column(name = "tarifa_por_ocupante", nullable = false) long tarifaPorOcupante,
        @Column(name = "ocupantes_facturables", nullable = false) int ocupantesFacturables) {
}
