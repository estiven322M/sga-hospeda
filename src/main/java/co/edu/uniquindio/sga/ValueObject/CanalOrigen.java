package co.edu.uniquindio.sga.ValueObject;

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
