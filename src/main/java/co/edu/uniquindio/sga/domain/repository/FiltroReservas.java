package co.edu.uniquindio.sga.domain.repository;

import java.time.LocalDate;

import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.DocumentoIdentidad;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

/**
 * Criterios opcionales para listar reservas: cualquier campo nulo significa "no filtrar por él".
 * <ul>
 *   <li>{@code entradaDesde} / {@code entradaHasta}: sobre la fecha de ENTRADA de la estancia (llegadas previstas, 7.6).</li>
 *   <li>{@code salidaDesde} / {@code salidaHasta}: sobre la fecha de SALIDA (salidas previstas, 7.6).</li>
 *   <li>{@code titular}: documento EXACTO del titular.</li>
 *   <li>{@code nombreTitular}: texto que el nombre del titular CONTIENE, sin distinguir mayúsculas.</li>
 * </ul>
 */
public record FiltroReservas(
        LocalDate entradaDesde,
        LocalDate entradaHasta,
        LocalDate salidaDesde,
        LocalDate salidaHasta,
        EstadoReserva estado,
        IdentificacionApartamento apartamento,
        CanalOrigen canal,
        DocumentoIdentidad titular,
        String nombreTitular) {

    public FiltroReservas {
        nombreTitular = nombreTitular == null || nombreTitular.isBlank() ? null : nombreTitular.trim();
    }

    /** Filtro anterior a 7.6: solo entradas, estado, apartamento, canal y documento del titular. */
    public FiltroReservas(LocalDate entradaDesde, LocalDate entradaHasta, EstadoReserva estado,
                          IdentificacionApartamento apartamento, CanalOrigen canal, DocumentoIdentidad titular) {
        this(entradaDesde, entradaHasta, null, null, estado, apartamento, canal, titular, null);
    }
}
