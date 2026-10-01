package co.edu.uniquindio.sga.domain.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.Cargo;
import co.edu.uniquindio.sga.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;
import co.edu.uniquindio.sga.domain.valueobject.Pago;

public class Folio {
	
	private final CodigoReserva reserva; 
    private final List<Cargo> cargos = new ArrayList<>();
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

    public void registrarPago(Pago pago) {
        verificarQueNoEsteCerrado();
        if (pago == null) throw new ReglaDominioException("El pago es obligatorio");
        this.pagos.add(pago);
    }

    public Dinero saldo() {
        Dinero totalCargos = cargos.stream().map(Cargo::valor).reduce(Dinero.CERO, Dinero::mas);
        Dinero totalPagos = pagos.stream().map(Pago::valor).reduce(Dinero.CERO, Dinero::mas);
        return totalCargos.menos(totalPagos);
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
    public List<Pago> obtenerPagos() { return Collections.unmodifiableList(pagos); }
    public boolean estaCerrado() { return cerrado; }
    public CodigoReserva getReserva() { return reserva; }
    public String getAutorizacionCierre() { return autorizacionCierre; }

}
