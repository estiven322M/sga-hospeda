package co.edu.uniquindio.sga.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.application.exception.ApartamentoNoEncontradoException;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.repository.TarifarioRepository;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Tarifa;

/** Listado de las tarifas de un apartamento (una por temporada), de 10 en 10. Solo lectura; 404 si no existe. */
@Service
public class ListarTarifasUseCase {

    private final ApartamentoRepository apartamentoRepository;
    private final TarifarioRepository tarifarioRepository;

    public ListarTarifasUseCase(ApartamentoRepository apartamentoRepository, TarifarioRepository tarifarioRepository) {
        this.apartamentoRepository = apartamentoRepository;
        this.tarifarioRepository = tarifarioRepository;
    }

    @Transactional(readOnly = true)
    public Pagina<Tarifa> ejecutar(IdentificacionApartamento apartamento, SolicitudPagina pagina) {
        apartamentoRepository.buscarPorIdentificacion(apartamento)
                .orElseThrow(() -> new ApartamentoNoEncontradoException(apartamento));
        return Pagina.de(tarifarioRepository.listarTarifas(apartamento), pagina);
    }
}
