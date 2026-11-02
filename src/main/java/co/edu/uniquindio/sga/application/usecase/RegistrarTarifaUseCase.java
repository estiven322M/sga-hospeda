package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.TarifarioRepository;
import co.edu.uniquindio.sga.domain.service.RegistroTarifaService;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Tarifa;

/**
 * Definir o reemplazar la tarifa de un apartamento en una temporada (7.4). Orquesta: verifica que el apartamento
 * exista (404) y delega en RegistroTarifaService, que exige que la temporada exista y que el valor no sea negativo.
 */
@Service
public class RegistrarTarifaUseCase {

    private final ApartamentoRepository apartamentoRepository;
    private final TarifarioRepository tarifarioRepository;
    private final RegistroTarifaService registroService;

    public RegistrarTarifaUseCase(ApartamentoRepository apartamentoRepository,
                                  TarifarioRepository tarifarioRepository, RegistroTarifaService registroService) {
        this.apartamentoRepository = apartamentoRepository;
        this.tarifarioRepository = tarifarioRepository;
        this.registroService = registroService;
    }

    @Transactional
    public Tarifa ejecutar(IdentificacionApartamento apartamento, String temporada, Dinero valorPorOcupanteNoche) {
        apartamentoRepository.buscarPorIdentificacion(apartamento)
                .orElseThrow(() -> new ApartamentoNoEncontradoException(apartamento));
        Tarifa tarifa = registroService.crear(temporada, valorPorOcupanteNoche);
        tarifarioRepository.guardarTarifa(apartamento, tarifa);
        return tarifa;
    }
}
