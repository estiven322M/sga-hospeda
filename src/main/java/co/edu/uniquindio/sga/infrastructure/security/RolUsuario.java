package co.edu.uniquindio.sga.infrastructure.security;

/**
 * Roles del SGA (7.1). No es un concepto del dominio: el dominio no conoce al usuario (3.6).
 * El nombre de cada constante es, tal cual, la autoridad que viaja en el claim "roles" del JWT
 * y la que se consulta con hasAuthority('...') en @PreAuthorize (sin el prefijo ROLE_).
 */
public enum RolUsuario {
    HUESPED,
    RECEPCIONISTA,
    ADMINISTRADOR,
    PERSONAL_SERVICIO
}
