package co.edu.uniquindio.sga.application.dto.response;

import co.edu.uniquindio.sga.domain.valueobject.TipoCanalVenta;

/**
 * La respuesta de crear un canal: sus datos y su credencial en claro. Es la ÚNICA vez que el sistema la muestra (después solo existe
 * su hash): hay que copiarla en ese momento y entregarla al canal.
 */
public record CanalVentaCreadoResponse(String codigo, String nombre, TipoCanalVenta tipo, boolean activo, String credencial) {
}
