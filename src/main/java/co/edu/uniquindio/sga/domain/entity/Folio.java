package co.edu.uniquindio.sga.domain.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.Ajuste;
import co.edu.uniquindio.sga.domain.valueobject.Cargo;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.Pago;

public class Folio {

	private final CodigoReserva reserva;
    private final List<Cargo> cargos = new ArrayList<>();
    private final List<Ajuste> ajustes = new ArrayList<>();
    private final List<Pago> pagos = new ArrayList<>();

    private boolean cerrado;
    private String autorizacionCierre;

    public Folio(CodigoReserva reserva, Cargo cargoAlojamiento) {
        if (reserva == null) {
            throw new ReglaDominioException("El folio debe pertenecer a una reserva");
        }
        if (cargoAlojamiento == null) {
            throw new ReglaDominioException("El folio se abre con el cargo de alojamiento de la reserva");
        }
        this.reserva = reserva;
        this.cargos.add(cargoAlojamiento);
    }

    public void registrarCargo(Cargo cargo) {
        verificarQueNoEsteCerrado();
        if (cargo == null) throw new ReglaDominioException("El cargo es obligatorio");
        this.cargos.add(cargo);
    }

    /** RN-16: los movimientos no se editan; una corrección es un ajuste (positivo o negativo). */
    public void registrarAjuste(Ajuste ajuste) {
        verificarQueNoEsteCerrado();
        if (ajuste == null) throw new ReglaDominioException("El ajuste es obligatorio");
        this.ajustes.add(ajuste);
    }

    public void registrarPago(Pago pago) {
        verificarQueNoEsteCerrado();
        if (pago == null) throw new ReglaDominioException("El pago es obligatorio");
        this.pagos.add(pago);
    }

    /**
     * Cancelación de la reserva (7.4, RN-13): se revierte con un ajuste negativo todo lo cargado por la estancia
     * y se registra la retención como penalidad por cancelación. Lo ya pagado queda en el folio, de modo que
     * el saldo resultante es (retención - pagado): negativo significa saldo a favor del huésped (la devolución).
     */
    public void liquidarCancelacion(Dinero retencion) {
        liquidarTerminacion(retencion, "Reverso del alojamiento por cancelación", "Penalidad por cancelación");
    }

    /**
     * No-show (7.4): el titular no se presentó. Misma mecánica que la cancelación: se revierte lo cargado y se
     * registra la retención de la política congelada como penalidad por no-show.
     */
    public void liquidarNoShow(Dinero retencion) {
        liquidarTerminacion(retencion, "Reverso del alojamiento por no-show", "Penalidad por no-show");
    }

    /**
     * Vencimiento de una reserva pendiente (RN-21): la reserva pasa a CANCELADA por decisión del sistema, sin
     * penalidad. Se revierte lo cargado por la estancia; lo que el huésped ya hubiera pagado queda como saldo a favor.
     */
    public void liquidarVencimiento() {
        liquidarTerminacion(Dinero.CERO, "Reverso del alojamiento por vencimiento de la reserva", "Penalidad por vencimiento");
    }

    private void liquidarTerminacion(Dinero retencion, String conceptoReverso, String conceptoPenalidad) {
        verificarQueNoEsteCerrado();
        if (retencion == null || retencion.esNegativo()) {
            throw new ReglaDominioException("La retención por cancelación no puede ser negativa");
        }
        Dinero cargado = totalCargado();
        if (retencion.valor().compareTo(cargado.valor()) > 0) {
            throw new ReglaDominioException("La retención no puede superar lo cargado en el folio");
        }
        this.ajustes.add(new Ajuste(conceptoReverso, Dinero.CERO.menos(cargado)));
        if (!retencion.esCero()) {
            this.cargos.add(new Cargo(conceptoPenalidad, retencion));
        }
    }

    public Dinero saldo() {
        return totalCargado().menos(pagos.stream().map(Pago::valor).reduce(Dinero.CERO, Dinero::mas));
    }

    /** Cargos más ajustes: lo que el huésped debe antes de descontar los pagos. */
    private Dinero totalCargado() {
        Dinero totalCargos = cargos.stream().map(Cargo::valor).reduce(Dinero.CERO, Dinero::mas);
        Dinero totalAjustes = ajustes.stream().map(Ajuste::valor).reduce(Dinero.CERO, Dinero::mas);
        return totalCargos.mas(totalAjustes);
    }

    public void cerrar(String autorizacion) {
        verificarQueNoEsteCerrado();
        // RN-17: el cierre con saldo exige autorización explícita registrada
        if (!saldo().esCero() && (autorizacion == null || autorizacion.isBlank())) {
            throw new ReglaDominioException("No se puede cerrar un folio con saldo pendiente sin autorización registrada");
        }
        this.autorizacionCierre = autorizacion;
        this.cerrado = true;
    }

    private void verificarQueNoEsteCerrado() {
        if (cerrado) {
            throw new ReglaDominioException("Un folio cerrado no admite movimientos");
        }
    }

    public List<Cargo> obtenerCargos() { return Collections.unmodifiableList(cargos); }
    public List<Ajuste> obtenerAjustes() { return Collections.unmodifiableList(ajustes); }
    public List<Pago> obtenerPagos() { return Collections.unmodifiableList(pagos); }
    public boolean estaCerrado() { return cerrado; }
    public CodigoReserva getReserva() { return reserva; }
    public String getAutorizacionCierre() { return autorizacionCierre; }

}
