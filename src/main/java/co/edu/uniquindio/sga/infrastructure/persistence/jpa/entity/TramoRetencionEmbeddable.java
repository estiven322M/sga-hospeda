package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record TramoRetencionEmbeddable(
        @Column(name = "horas_antelacion_minima", nullable = false) int horasAntelacionMinima,
        @Column(name = "porcentaje_retencion", nullable = false) int porcentajeRetencion) {
}
