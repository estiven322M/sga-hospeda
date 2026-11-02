package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.domain.entity.Bloqueo;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.BloqueoRepository;
import co.edu.uniquindio.sga.domain.service.RegistroBloqueoService;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

/**
 * CU-09 Registrar bloqueo. Orquesta: verifica que el apartamento exista (404) y delega en el servicio de
 * dominio la creación del bloqueo, que valida motivo y rango y que no haya reservas activas en esas noches (7.3).
 * Un bloqueo no cambia el estado operativo del apartamento: son conceptos distintos (3.3).
 */
@Service
public class RegistrarBloqueoUseCase {

    private final ApartamentoRepository apartamentoRepository;
    private final BloqueoRepository bloqueoRepository;
    private final RegistroBloqueoService registroBloqueoService;

    public RegistrarBloqueoUseCase(ApartamentoRepository apartamentoRepository, BloqueoRepository bloqueoRepository,
                                   RegistroBloqueoService registroBloqueoService) {
        this.apartamentoRepository = apartamentoRepository;
        this.bloqueoRepository = bloqueoRepository;
        this.registroBloqueoService = registroBloqueoService;
    }

    @Transactional
    public Bloqueo ejecutar(IdentificacionApartamento apartamento, Estancia rango, String motivo) {
        apartamentoRepository.buscarPorIdentificacion(apartamento)
                .orElseThrow(() -> new ApartamentoNoEncontradoException(apartamento));
        Bloqueo bloqueo = registroBloqueoService.crear(apartamento, rango, motivo);
        return bloqueoRepository.guardar(bloqueo);
    }
}
