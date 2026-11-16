package co.edu.uniquindio.sga.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.service.CalendarioOcupacionService;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.NocheCalendario;

/**
 * 7.1, 7.5: calendario de ocupación de un apartamento sobre el rango [desde, hasta). Verifica que el apartamento exista
 * (404) y delega en el servicio de dominio, que arma el estado de cada noche y exige el máximo de noches configurado (409).
 * Qué puede ver cada rol del resultado lo decide la presentación, no este caso de uso.
 */
@Service
public class ConsultarCalendarioUseCase {

    private final ApartamentoRepository apartamentos;
    private final CalendarioOcupacionService calendario;

    public ConsultarCalendarioUseCase(ApartamentoRepository apartamentos, CalendarioOcupacionService calendario) {
        this.apartamentos = apartamentos;
        this.calendario = calendario;
    }

    @Transactional(readOnly = true)
    public List<NocheCalendario> ejecutar(IdentificacionApartamento apartamento, Estancia rango) {
        apartamentos.buscarPorIdentificacion(apartamento)
                .orElseThrow(() -> new ApartamentoNoEncontradoException(apartamento));
        return calendario.construir(apartamento, rango);
    }
}
