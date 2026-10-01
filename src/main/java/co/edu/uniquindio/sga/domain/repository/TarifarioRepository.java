package co.edu.uniquindio.sga.domain.repository;

import java.time.LocalDate;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.Tarifa;

public interface TarifarioRepository {
	
	Tarifa tarifaDe(IdentificacionApartamento apartamento, LocalDate noche);

}
