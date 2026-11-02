package co.edu.uniquindio.sga.application.usecase;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniquindio.sga.domain.entity.Apartamento;
import co.edu.uniquindio.sga.domain.entity.Ocupante;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ApartamentoRepository;
import co.edu.uniquindio.sga.domain.repository.Pagina;
import co.edu.uniquindio.sga.domain.repository.SolicitudPagina;
import co.edu.uniquindio.sga.domain.service.CotizadorEstancia;
import co.edu.uniquindio.sga.domain.service.DisponibilidadApartamentoService;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;

/**
 * CU-02 Consultar disponibilidad. Para cada apartamento activo le pregunta al dominio si puede alojar
 * al grupo en la estancia (RN-01, RN-02, RN-07, RN-20) y lo cotiza (RN-05, RN-06). No decide ninguna
 * regla: un apartamento que el dominio rechaza simplemente no aparece en el resultado.
 * Filtros opcionales: capacidad mínima y rango de precio del total de la estancia. Pagina de a 10.
 */
@Service
public class ConsultarDisponibilidadUseCase {

    private final ApartamentoRepository apartamentoRepository;
    private final DisponibilidadApartamentoService disponibilidadService;
    private final CotizadorEstancia cotizador;

    public ConsultarDisponibilidadUseCase(ApartamentoRepository apartamentoRepository,
                                          DisponibilidadApartamentoService disponibilidadService,
                                          CotizadorEstancia cotizador) {
        this.apartamentoRepository = apartamentoRepository;
        this.disponibilidadService = disponibilidadService;
        this.cotizador = cotizador;
    }

    /**
     * @param capacidadMinima filtro opcional (null = sin filtro)
     * @param precioMinimo    filtro opcional sobre el total de la estancia (null = sin filtro)
     * @param precioMaximo    filtro opcional sobre el total de la estancia (null = sin filtro)
     */
    @Transactional(readOnly = true)
    public Pagina<ApartamentoDisponible> ejecutar(Estancia estancia, List<Ocupante> ocupantes,
                                                  Integer capacidadMinima, Dinero precioMinimo,
                                                  Dinero precioMaximo, SolicitudPagina pagina) {
        List<ApartamentoDisponible> disponibles = apartamentoRepository.listarActivos().stream()
                .filter(a -> capacidadMinima == null || a.getCapacidad() >= capacidadMinima)
                .filter(a -> estaDisponible(a, estancia, ocupantes.size()))
                .map(a -> new ApartamentoDisponible(a,
                        cotizador.cotizar(a.getIdentificacion(), estancia, ocupantes)))
                .filter(d -> precioMinimo == null
                        || d.cotizacion().total().valor().compareTo(precioMinimo.valor()) >= 0)
                .filter(d -> precioMaximo == null
                        || d.cotizacion().total().valor().compareTo(precioMaximo.valor()) <= 0)
                .sorted(Comparator.comparing((ApartamentoDisponible d) -> d.cotizacion().total().valor())
                        .thenComparing(d -> d.apartamento().getIdentificacion().valor()))
                .toList();
        return Pagina.de(disponibles, pagina);
    }

    /** El dominio responde con una excepción cuando no puede alojar; aquí esa respuesta significa "no disponible". */
    private boolean estaDisponible(Apartamento apartamento, Estancia estancia, int totalOcupantes) {
        try {
            disponibilidadService.verificarDisponible(apartamento, estancia, totalOcupantes, null);
            return true;
        } catch (ReglaDominioException noDisponible) {
            return false;
        }
    }
}
