package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDateTime;

@Embeddable
public record EventoReservaEmbeddable(
        @Column(name = "momento", nullable = false) LocalDateTime momento,
        @Column(name = "accion", nullable = false, length = 30) String accion,
        @Column(name = "autor", nullable = false, length = 120) String autor,
        @Enumerated(EnumType.STRING) @Column(name = "estado_anterior", length = 12) EstadoReserva estadoAnterior,
        @Enumerated(EnumType.STRING) @Column(name = "estado_nuevo", length = 12) EstadoReserva estadoNuevo,
        @Column(name = "observacion", length = 500) String observacion) {
}
