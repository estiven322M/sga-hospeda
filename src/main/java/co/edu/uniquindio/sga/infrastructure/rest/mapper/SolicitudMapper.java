package co.edu.uniquindio.sga.infrastructure.rest.mapper;

import java.time.LocalDate;
import java.util.List;

import org.mapstruct.Mapper;

import co.edu.uniquindio.sga.application.dto.request.CerrarFolioRequest;
import co.edu.uniquindio.sga.application.dto.request.OcupanteRequest;
import co.edu.uniquindio.sga.application.dto.request.RegistrarPagoRequest;
import co.edu.uniquindio.sga.application.dto.request.RegistrarPoliticaCancelacionRequest;
import co.edu.uniquindio.sga.application.dto.request.RegistrarTemporadaRequest;
import co.edu.uniquindio.sga.domain.entity.Temporada;
import co.edu.uniquindio.sga.domain.valueobject.Periodo;
import co.edu.uniquindio.sga.domain.valueobject.TramoRetencion;
import co.edu.uniquindio.sga.domain.valueobject.VersionPolitica;
import co.edu.uniquindio.sga.domain.valueobject.Pago;
import co.edu.uniquindio.sga.domain.entity.Ocupante;
import co.edu.uniquindio.sga.domain.repository.FiltroReservas;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.DocumentoIdentidad;
import co.edu.uniquindio.sga.domain.valueobject.EstadoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.IdentificadorExterno;

/**
 * Convierte lo que llega por HTTP en tipos del dominio. Se escribe a mano (métodos default) porque
 * los objetos del dominio se construyen solo por sus constructores, que son los que validan las reglas
 * (RN-03 en Estancia, fecha de nacimiento en Ocupante...). El mapper no valida nada por su cuenta.
 */
@Mapper(componentModel = "spring")
public interface SolicitudMapper {

    default Estancia aEstancia(LocalDate entrada, LocalDate salida) {
        return new Estancia(entrada, salida);
    }

    /** {@code hoy} lo pone el servidor: el Ocupante calcula su edad respecto a la fecha actual. */
    default Ocupante aOcupante(OcupanteRequest request, LocalDate hoy) {
        return new Ocupante(new DocumentoIdentidad(request.documento()), request.nombre(),
                request.fechaNacimiento(), hoy);
    }

    default List<Ocupante> aOcupantes(List<OcupanteRequest> requests, LocalDate hoy) {
        return requests.stream().map(r -> aOcupante(r, hoy)).toList();
    }

    default Temporada aTemporada(RegistrarTemporadaRequest request) {
        List<Periodo> periodos = request.periodos() == null ? List.of()
                : request.periodos().stream().map(p -> new Periodo(p.desde(), p.hasta())).toList();
        return new Temporada(request.nombre(), request.base(), periodos);
    }

    default List<TramoRetencion> aTramos(List<RegistrarPoliticaCancelacionRequest.TramoRequest> requests) {
        return requests.stream()
                .map(t -> new TramoRetencion(t.horasAntelacionMinima(), t.porcentajeRetencion())).toList();
    }

    /**
     * Texto de la autorización de cierre que queda registrada en el folio (quién y por qué, 7.7).
     * Si falta alguno de los dos datos devuelve null: el dominio decide si el cierre sin autorización es válido.
     */
    default String aAutorizacionCierre(CerrarFolioRequest request) {
        if (request.autorizadoPor() == null || request.autorizadoPor().isBlank()
                || request.motivo() == null || request.motivo().isBlank()) {
            return null;
        }
        return "Autorizado por " + request.autorizadoPor().trim() + ": " + request.motivo().trim();
    }

    default VersionPolitica aVersion(int numero) {
        return new VersionPolitica(numero);
    }

    default Pago aPago(RegistrarPagoRequest request) {
        return new Pago(request.medio(), request.fecha(), Dinero.de(request.valor()));
    }

    default IdentificacionApartamento aApartamento(String identificacion) {
        return new IdentificacionApartamento(identificacion);
    }

    /** Texto ausente o vacío significa "sin identificador"; el dominio decide si el canal lo exige (RN-19). */
    default IdentificadorExterno aIdentificadorExterno(String identificador) {
        return identificador == null || identificador.isBlank() ? null : new IdentificadorExterno(identificador);
    }

    default CodigoReserva aCodigo(String codigo) {
        return new CodigoReserva(codigo);
    }

    /** Pesos COP enteros; null significa "sin filtro". */
    default Dinero aDinero(Long pesos) {
        return pesos == null ? null : Dinero.de(pesos);
    }

    /** Texto vacío o ausente significa "sin filtro" en cada criterio. */
    default FiltroReservas aFiltro(LocalDate entradaDesde, LocalDate entradaHasta, EstadoReserva estado,
                                   String apartamento, CanalOrigen canal, String titular) {
        return new FiltroReservas(entradaDesde, entradaHasta, estado,
                apartamento == null || apartamento.isBlank() ? null : new IdentificacionApartamento(apartamento),
                canal,
                titular == null || titular.isBlank() ? null : new DocumentoIdentidad(titular));
    }
}
