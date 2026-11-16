package co.edu.uniquindio.sga.domain.service;

import java.util.Collection;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.valueobject.Caracteristica;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

/**
 * L-04: la identificación de cada apartamento es única y estable. Un apartamento no puede saber si su
 * identificación ya existe (necesita conocer a los demás), por eso la regla vive en un servicio de dominio.
 * El apartamento nace inactivo (7.4): se activa después, cuando tenga tarifas en todas las temporadas.
 */
public class RegistroApartamentoService {

    private final ApartamentoRepository apartamentoRepository;

    public RegistroApartamentoService(ApartamentoRepository apartamentoRepository) {
        this.apartamentoRepository = apartamentoRepository;
    }

    public Apartamento crear(IdentificacionApartamento identificacion, String nombre, String descripcion,
                             int dormitorios, int capacidad, Collection<String> dotacion,
                             Collection<Caracteristica> caracteristicas) {
        Apartamento apartamento = new Apartamento(identificacion, nombre, descripcion, dormitorios, capacidad,
                dotacion, caracteristicas);
        if (apartamentoRepository.buscarPorIdentificacion(identificacion).isPresent()) {
            throw new ReglaDominioException("Ya existe un apartamento con la identificación " + identificacion.valor());
        }
        return apartamento;
    }
}
