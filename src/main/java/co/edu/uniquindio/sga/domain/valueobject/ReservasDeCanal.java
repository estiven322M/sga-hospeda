package co.edu.uniquindio.sga.domain.valueobject;

/** Reservas que entraron por un canal de origen en un periodo; el total incluye las canceladas y los no-show. */
public record ReservasDeCanal(CanalOrigen canal, int total, int canceladas, int noShow) {
}
