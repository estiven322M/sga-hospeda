package co.edu.uniquindio.sga.infrastructure.rest.mapper;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.ContenidoImagen;

/**
 * Archivo subido (multipart) → contenido de imagen del dominio. El formato NO se toma del tipo que declara el cliente ni de la
 * extensión: ContenidoImagen lo reconoce por los primeros bytes del archivo (JPG, PNG o WEBP) y rechaza todo lo demás con 409.
 */
@Component
public class ImagenSubidaMapper {

    public ContenidoImagen aContenido(MultipartFile archivo) {
        try {
            return ContenidoImagen.de(archivo.getOriginalFilename(), archivo.getBytes());
        } catch (IOException e) {
            throw new ReglaDominioException("No se pudo leer la imagen " + archivo.getOriginalFilename());
        }
    }

    public List<ContenidoImagen> aContenidos(List<MultipartFile> archivos) {
        return archivos.stream().map(this::aContenido).toList();
    }
}
