package co.edu.uniquindio.sga.domain.repository;

import java.time.LocalDate;

import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.DocumentoIdentidad;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

/**
 * Criterios opcionales para listar reservas: cualquier campo nulo significa "no filtrar por él".
 * Las fechas filtran por fecha de entrada de la estancia.
 */
public record FiltroReservas(
        LocalDate entradaDesde,
        LocalDate entradaHasta,
        EstadoReserva estado,
        IdentificacionApartamento apartamento,
        CanalOrigen canal,
        DocumentoIdentidad titular) {
}
