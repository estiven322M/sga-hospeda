package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Una tarifa por apartamento y temporada; ambos se referencian por su identificador natural (regla 3). */
@Embeddable
public record TarifaId(
        @Column(name = "apartamento_id", nullable = false, length = 20) String apartamentoId,
        @Column(name = "temporada_nombre", nullable = false, length = 60) String temporada) {
}
