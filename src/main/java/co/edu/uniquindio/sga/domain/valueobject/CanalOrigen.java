package co.edu.uniquindio.sga.domain.valueobject;

public enum CanalOrigen {
	
	PORTAL,
    DIRECTO,
    EXTERNO;

    /**
     * RN-19: toda reserva externa llega con un identificador propio del canal.
     */
    public boolean exigeIdentificadorExterno() {
        return this == EXTERNO;
    }

}
