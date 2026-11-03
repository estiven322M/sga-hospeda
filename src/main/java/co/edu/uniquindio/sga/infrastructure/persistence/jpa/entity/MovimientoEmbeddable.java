package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Cargo o ajuste del folio: un concepto y un valor en pesos (los ajustes pueden ser negativos). */
@Embeddable
public record MovimientoEmbeddable(
        @Column(name = "concepto", nullable = false, length = 120) String concepto,
        @Column(name = "valor", nullable = false) long valor) {
}
