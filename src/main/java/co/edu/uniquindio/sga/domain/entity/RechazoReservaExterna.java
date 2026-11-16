package co.edu.uniquindio.sga.domain.entity;

import java.time.LocalDateTime;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.CanalOrigen;
import co.edu.uniquindio.sga.domain.valueobject.Estancia;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.IdentificadorExterno;
import co.edu.uniquindio.sga.domain.valueobject.RevisionRechazo;

/**
 * RN-18: constancia de una reserva externa que se rechazó por estar en conflicto. Es un registro inmutable: guarda
 * lo que pidió el canal y la razón del rechazo, y nunca toca la reserva vigente. Solo se aceptan canales que exigen
 * identificador externo, porque sin él no hay forma de reconocer el mensaje.
 * 7.8: el administrador lo REVISA: {@link #resolver} lo marca como revisado con una nota. La revisión es solo del rechazo; esta clase
 * no conoce ninguna reserva, así que resolver un conflicto no puede tocar la reserva vigente.
 */
public class RechazoReservaExterna {

    private final CanalOrigen canalOrigen;
    private final IdentificadorExterno identificadorExterno;
    private final IdentificacionApartamento apartamento;
    private final Estancia estancia;
    private final String motivo;
    private final LocalDateTime rechazadaEn;
    /** Identidad: la asigna la persistencia al guardarlo por primera vez (nula antes). */
    private final Long id;
    private RevisionRechazo revision;

    public RechazoReservaExterna(CanalOrigen canalOrigen, IdentificadorExterno identificadorExterno,
                                 IdentificacionApartamento apartamento, Estancia estancia, String motivo,
                                 LocalDateTime rechazadaEn) {
        this(null, canalOrigen, identificadorExterno, apartamento, estancia, motivo, rechazadaEn, null);
    }

    /** Restaura un rechazo guardado (persistencia), con su identidad y su revisión si ya la tiene. */
    public static RechazoReservaExterna reconstruir(Long id, CanalOrigen canalOrigen, IdentificadorExterno identificadorExterno,
                                                    IdentificacionApartamento apartamento, Estancia estancia, String motivo,
                                                    LocalDateTime rechazadaEn, RevisionRechazo revision) {
        return new RechazoReservaExterna(id, canalOrigen, identificadorExterno, apartamento, estancia, motivo, rechazadaEn, revision);
    }

    private RechazoReservaExterna(Long id, CanalOrigen canalOrigen, IdentificadorExterno identificadorExterno,
                                  IdentificacionApartamento apartamento, Estancia estancia, String motivo,
                                  LocalDateTime rechazadaEn, RevisionRechazo revision) {
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
        this.id = id;
        this.revision = revision;
    }

    /**
     * 7.8: marca el conflicto como revisado, con la nota de quien lo revisó. Un conflicto se revisa UNA vez: volver a resolverlo se
     * rechaza para no pisar la nota anterior. No modifica nada más: el rechazo es una constancia.
     */
    public void resolver(String nota, String revisadoPor, LocalDateTime ahora) {
        if (revision != null) {
            throw new ReglaDominioException("El conflicto ya fue revisado por " + revision.revisadoPor());
        }
        this.revision = new RevisionRechazo(nota, revisadoPor, ahora);
    }

    public boolean estaRevisado() {
        return revision != null;
    }

    public CanalOrigen getCanalOrigen() { return canalOrigen; }
    public IdentificadorExterno getIdentificadorExterno() { return identificadorExterno; }
    public IdentificacionApartamento getApartamento() { return apartamento; }
    public Estancia getEstancia() { return estancia; }
    public String getMotivo() { return motivo; }
    public LocalDateTime getRechazadaEn() { return rechazadaEn; }
    public Long getId() { return id; }
    /** La revisión del administrador; nula mientras el conflicto está pendiente. */
    public RevisionRechazo getRevision() { return revision; }
}
