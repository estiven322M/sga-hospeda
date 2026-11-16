package co.edu.uniquindio.sga.domain.repository;

import co.edu.uniquindio.sga.domain.exception.AlmacenamientoImagenesException;
import co.edu.uniquindio.sga.domain.valueobject.ContenidoImagen;
import co.edu.uniquindio.sga.domain.valueobject.ReferenciaImagen;

/**
 * Puerto de salida del almacenamiento de imágenes (7.3, I-01): el dominio pide guardar, no sabe dónde. Adaptadores: en disco
 * (local, el que viene activo, sin internet ni credenciales) y Cloudinary (plan gratuito, con tiempo máximo de espera).
 * Retirar una imagen es lógico: el puerto no borra nada.
 */
public interface AlmacenImagenes {

    /**
     * Guarda la imagen en la carpeta lógica dada (por ejemplo {@code apartamentos/APT-101} o {@code perfiles}) y devuelve su URL
     * pública y su referencia.
     *
     * @throws AlmacenamientoImagenesException si el almacén no pudo guardarla; el caso de uso lo deja pasar y la API responde 503
     */
    ReferenciaImagen guardar(ContenidoImagen contenido, String carpeta);
}
