package co.edu.uniquindio.sga.domain.service;

import co.edu.uniquindio.sga.domain.entity.Temporada;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.TemporadaRepository;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.Tarifa;

/**
 * 7.4: la tarifa es el valor por ocupante facturable y por noche de un apartamento en una temporada.
 * Solo puede definirse para una temporada que exista (incluida la base), y no puede ser negativa.
 * El registro de temporadas es otro agregado, por eso la comprobación vive en un servicio de dominio.
 */
public class RegistroTarifaService {

    private final TemporadaRepository temporadaRepository;

    public RegistroTarifaService(TemporadaRepository temporadaRepository) {
        this.temporadaRepository = temporadaRepository;
    }

    public Tarifa crear(String temporada, Dinero valorPorOcupanteNoche) {
        if (temporada == null || temporada.isBlank()) {
            throw new ReglaDominioException("La tarifa debe indicar su temporada");
        }
        if (valorPorOcupanteNoche == null || valorPorOcupanteNoche.esNegativo()) {
            throw new ReglaDominioException("La tarifa no puede ser negativa");
        }
        String nombre = temporada.trim();
        boolean existe = temporadaRepository.listarTodas().stream().map(Temporada::getNombre)
                .anyMatch(nombre::equals);
        if (!existe) {
            throw new ReglaDominioException("La temporada " + nombre + " no existe");
        }
        return new Tarifa(nombre, valorPorOcupanteNoche);
    }
}
