package co.edu.uniquindio.sga.domain.entity;

import java.time.LocalDateTime;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.IdentificadorExterno;

/**
 * RN-18: constancia de una reserva externa que se rechazó por estar en conflicto. Es un registro inmutable: guarda
 * lo que pidió el canal y la razón del rechazo, y nunca toca la reserva vigente. Solo se aceptan canales que exigen
 * identificador externo, porque sin él no hay forma de reconocer el mensaje.
 */
public class RechazoReservaExterna {

    private final CanalOrigen canalOrigen;
    private final IdentificadorExterno identificadorExterno;
    private final IdentificacionApartamento apartamento;
    private final Estancia estancia;
    private final String motivo;
    private final LocalDateTime rechazadaEn;

    public RechazoReservaExterna(CanalOrigen canalOrigen, IdentificadorExterno identificadorExterno,
                                 IdentificacionApartamento apartamento, Estancia estancia, String motivo,
                                 LocalDateTime rechazadaEn) {
        if (canalOrigen == null || !canalOrigen.exigeIdentificadorExterno()) {
            throw new ReglaDominioException("Solo se registran rechazos de reservas que llegan por un canal externo");
        }
        if (identificadorExterno == null) {
            throw new ReglaDominioException("El rechazo debe indicar el identificador externo de la reserva");
        }
        if (apartamento == null) throw new ReglaDominioException("El rechazo debe indicar el apartamento solicitado");
        if (estancia == null) throw new ReglaDominioException("El rechazo debe indicar la estancia solicitada");
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaDominioException("Todo rechazo debe registrar su motivo");
        }
        if (rechazadaEn == null) throw new ReglaDominioException("El rechazo debe tener fecha y hora");

        this.canalOrigen = canalOrigen;
        this.identificadorExterno = identificadorExterno;
        this.apartamento = apartamento;
        this.estancia = estancia;
        this.motivo = motivo;
        this.rechazadaEn = rechazadaEn;
    }

    public CanalOrigen getCanalOrigen() { return canalOrigen; }
    public IdentificadorExterno getIdentificadorExterno() { return identificadorExterno; }
    public IdentificacionApartamento getApartamento() { return apartamento; }
    public Estancia getEstancia() { return estancia; }
    public String getMotivo() { return motivo; }
    public LocalDateTime getRechazadaEn() { return rechazadaEn; }
}
