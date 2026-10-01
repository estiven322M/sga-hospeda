package co.edu.uniquindio.sga.domain.repository;

import co.edu.uniquindio.sga.domain.valueobject.PlazoConfirmacion;
import co.edu.uniquindio.sga.domain.valueobject.TiempoPreparacion;
import co.edu.uniquindio.sga.domain.valueobject.UmbralEdadFacturable;

public interface ConfiguracionAlojamiento {
	
	TiempoPreparacion tiempoPreparacion();
    PlazoConfirmacion plazoConfirmacion();
    UmbralEdadFacturable umbralEdadFacturable();

}
