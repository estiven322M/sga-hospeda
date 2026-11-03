package co.edu.uniquindio.sga.domain.repository;

import java.time.LocalDate;
import java.util.List;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Tarifa;

public interface TarifarioRepository {

	Tarifa tarifaDe(IdentificacionApartamento apartamento, LocalDate noche);

	/** Crea o reemplaza la tarifa del apartamento para la temporada de la tarifa. */
	void guardarTarifa(IdentificacionApartamento apartamento, Tarifa tarifa);

	List<Tarifa> listarTarifas(IdentificacionApartamento apartamento);

}
