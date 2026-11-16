package co.edu.uniquindio.sga.domain.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import co.edu.uniquindio.sga.domain.entity.Ocupante;
import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.repository.ConfiguracionAlojamiento;
import co.edu.uniquindio.sga.domain.repository.TarifarioRepository;
import co.edu.uniquindio.sga.domain.valueobject.CargoNoche;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.EstanciaMinimaPorTemporada;
import co.edu.uniquindio.sga.domain.valueobject.PoliticaDescuentoEstanciaLarga;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Tarifa;
import co.edu.uniquindio.sga.domain.valueobject.UmbralEdadFacturable;
import co.edu.uniquindio.sga.domain.valueobject.ValorCongelado;

/**
 * Calcula el valor de una estancia noche por noche (RN-05, RN-06) y le aplica las reglas propias de L-20 que dependen
 * de la temporada de cada noche o del número de noches: estancia mínima por temporada (RP-01) y descuento por estancia
 * larga (RP-02). Es un servicio de dominio porque necesita el tarifario y el calendario, que son otros agregados.
 */
public class CotizadorEstancia {
	
	private final TarifarioRepository tarifarioRepository;
    private final ConfiguracionAlojamiento configuracion;

    public CotizadorEstancia(TarifarioRepository tarifarioRepository,
                             ConfiguracionAlojamiento configuracion) {
        this.tarifarioRepository = tarifarioRepository;
        this.configuracion = configuracion;
    }

    public ValorCongelado cotizar(IdentificacionApartamento apartamento,
                                  Estancia estancia, List<Ocupante> ocupantes) {

        if (ocupantes == null || ocupantes.isEmpty()) {
            throw new ReglaDominioException("La cotización requiere la composición del grupo");
        }

        UmbralEdadFacturable umbral = configuracion.umbralEdadFacturable();
        int facturables = (int) ocupantes.stream()
                .filter(o -> o.esFacturableEn(estancia, umbral))
                .count();

        if (facturables == 0) {
            throw new ReglaDominioException("La reserva debe tener al menos un ocupante facturable");
        }

        List<CargoNoche> detalle = new ArrayList<>();
        for (LocalDate noche : estancia.nochesOcupadas()) {
            Tarifa tarifa = tarifarioRepository.tarifaDe(apartamento, noche);
            if (tarifa == null) {
                throw new ReglaDominioException("El apartamento no tiene tarifa definida para la noche del " + noche);
            }
            detalle.add(new CargoNoche(noche, tarifa.temporada(),
                                       tarifa.valorPorOcupanteNoche(), facturables));
        }
        // RP-01: la estancia debe cumplir el mínimo de noches de cada temporada por la que pasa
        new EstanciaMinimaPorTemporada(configuracion.estanciaMinimaPorTemporada()).verificar(detalle);
        // RP-02: el descuento por estancia larga se decide aquí y se congela en el valor (una sola vez, sobre el total)
        int porcentajeDescuento = new PoliticaDescuentoEstanciaLarga(configuracion.nochesDescuentoEstanciaLarga(),
                configuracion.porcentajeDescuentoEstanciaLarga()).porcentajePara(detalle.size());
        return new ValorCongelado(detalle, porcentajeDescuento);
    }

}
