package co.edu.uniquindio.sga.domain.entity;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;

public class Bloqueo {
	
	private final IdentificacionApartamento apartamento;
    private final Estancia rango;
    private final String motivo;

    public Bloqueo(IdentificacionApartamento apartamento, Estancia rango, String motivo) {
        if (apartamento == null) throw new ReglaDominioException("El bloqueo debe especificar el apartamento");
        if (rango == null) throw new ReglaDominioException("El bloqueo debe tener un rango de fechas");
        if (motivo == null || motivo.isBlank()) throw new ReglaDominioException("Todo bloqueo debe justificar un motivo");
        
        this.apartamento = apartamento;
        this.rango = rango;
        this.motivo = motivo;
    }
    
    /** Permite a los servicios de dominio consultar si este bloqueo afecta las fechas deseadas (RN-07) */
    public boolean afecta(Estancia otraEstancia) {
        return this.rango.seSolapaCon(otraEstancia);
    }

    public IdentificacionApartamento getApartamento() { return apartamento; }
    public Estancia getRango() { return rango; }
    public String getMotivo() { return motivo; }

}
