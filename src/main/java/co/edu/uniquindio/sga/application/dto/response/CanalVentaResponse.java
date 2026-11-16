package co.edu.uniquindio.sga.application.dto.response;

import co.edu.uniquindio.sga.domain.valueobject.TipoCanalVenta;

/** Un canal de venta (7.8). Nunca incluye la credencial: del canal solo se guarda su hash. */
public record CanalVentaResponse(String codigo, String nombre, TipoCanalVenta tipo, boolean activo) {
}
