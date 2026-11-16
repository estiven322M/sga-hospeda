package co.edu.uniquindio.sga.application.usecase;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import co.edu.uniquindio.sga.domain.entity.CanalVenta;
import co.edu.uniquindio.sga.domain.entity.Ocupante;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.valueobject.Caracteristica;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.OperacionCanal;
import co.edu.uniquindio.sga.domain.valueobject.ResultadoCanal;

/**
 * 10.3.1, operación 1: un canal consulta la disponibilidad de un rango de fechas para un grupo. Reutiliza la consulta CU-02 y deja
 * constancia del mensaje en la bitácora (aceptado o rechazado por una regla).
 */
@Service
public class ConsultarDisponibilidadCanalUseCase {

    private final ConsultarDisponibilidadUseCase disponibilidad;
    private final RegistrarEventoCanalUseCase bitacora;

    public ConsultarDisponibilidadCanalUseCase(ConsultarDisponibilidadUseCase disponibilidad, RegistrarEventoCanalUseCase bitacora) {
        this.disponibilidad = disponibilidad;
        this.bitacora = bitacora;
    }

    public Pagina<ApartamentoDisponible> ejecutar(CanalVenta canal, Estancia estancia, List<Ocupante> ocupantes,
                                                  Integer capacidadMinima, Set<Caracteristica> caracteristicas, SolicitudPagina pagina) {
        try {
            Pagina<ApartamentoDisponible> resultado = disponibilidad.ejecutar(estancia, ocupantes, capacidadMinima, null, null,
                    caracteristicas, pagina);
            bitacora.ejecutar(canal.getCodigo(), OperacionCanal.CONSULTAR_DISPONIBILIDAD, null, ResultadoCanal.ACEPTADA, 200,
                    resultado.totalElementos() + " apartamento(s) disponible(s) del " + estancia.fechaEntrada() + " al " + estancia.fechaSalida());
            return resultado;
        } catch (ReglaDominioException regla) {
            bitacora.ejecutar(canal.getCodigo(), OperacionCanal.CONSULTAR_DISPONIBILIDAD, null, ResultadoCanal.RECHAZADA, 409, regla.getMessage());
            throw regla;
        }
    }
}
