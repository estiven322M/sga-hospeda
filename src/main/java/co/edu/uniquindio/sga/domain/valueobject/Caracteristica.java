package co.edu.uniquindio.sga.domain.valueobject;

/**
 * Característica diferenciadora de un apartamento (L-06, 7.3). Es un catálogo cerrado a propósito: el filtro de
 * búsqueda (7.5) necesita valores estables; con texto libre "Terraza" y "terraza " serían características distintas.
 * No es un parámetro de la Ficha sino un vocabulario del negocio, por eso vive en el dominio como enum.
 */
public enum Caracteristica {
    TERRAZA,
    BALCON,
    VISTA_AL_PARQUE,
    VISTA_PANORAMICA,
    JACUZZI,
    PARQUEADERO,
    ACCESIBLE
}
