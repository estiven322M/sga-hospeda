package co.edu.uniquindio.sga.domain.repository;

import java.util.List;
import co.edu.uniquindio.sga.domain.entity.Bloqueo;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

public interface BloqueoRepository {
	
	List<Bloqueo> buscarVigentesPorApartamento(IdentificacionApartamento apartamento);

}
