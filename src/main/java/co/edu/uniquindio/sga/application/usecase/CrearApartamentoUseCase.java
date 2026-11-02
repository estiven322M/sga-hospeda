package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.service.RegistroApartamentoService;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

/**
 * Crear apartamento. Orquesta: RegistroApartamentoService valida que la identificación sea única (L-04) y
 * el constructor de Apartamento valida nombre, dormitorios y capacidad. El apartamento nace inactivo (7.4):
 * hasta que no se active con tarifas completas no aparece como vendible.
 */
@Service
public class CrearApartamentoUseCase {

    private final ApartamentoRepository apartamentoRepository;
    private final RegistroApartamentoService registroService;

    public CrearApartamentoUseCase(ApartamentoRepository apartamentoRepository,
                                   RegistroApartamentoService registroService) {
        this.apartamentoRepository = apartamentoRepository;
        this.registroService = registroService;
    }

    @Transactional
    public Apartamento ejecutar(IdentificacionApartamento identificacion, String nombre, int dormitorios,
                                int capacidad) {
        return apartamentoRepository.guardar(registroService.crear(identificacion, nombre, dormitorios, capacidad));
    }
}
