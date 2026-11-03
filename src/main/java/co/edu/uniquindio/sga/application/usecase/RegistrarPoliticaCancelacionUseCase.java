package co.edu.uniquindio.sga.application.usecase;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.domain.entity.PoliticaCancelacion;
import co.edu.uniquindio.sga.domain.repository.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga.domain.service.RegistroPoliticaCancelacionService;
import co.edu.uniquindio.sga.domain.valueobject.TramoRetencion;

/**
 * Definir o actualizar la política de cancelación (F-07). Orquesta: el servicio de dominio asigna el número de
 * versión siguiente y valida las reglas; aquí solo se guarda. La versión anterior no se modifica ni se borra,
 * porque las reservas existentes la tienen congelada (RN-13, RN-22).
 */
@Service
public class RegistrarPoliticaCancelacionUseCase {

    private final PoliticaCancelacionRepository politicaRepository;
    private final RegistroPoliticaCancelacionService registroService;

    public RegistrarPoliticaCancelacionUseCase(PoliticaCancelacionRepository politicaRepository,
                                               RegistroPoliticaCancelacionService registroService) {
        this.politicaRepository = politicaRepository;
        this.registroService = registroService;
    }

    @Transactional
    public PoliticaCancelacion ejecutar(LocalDate vigenteDesde, List<TramoRetencion> tramos) {
        return politicaRepository.guardar(registroService.crearNuevaVersion(vigenteDesde, tramos));
    }
}
