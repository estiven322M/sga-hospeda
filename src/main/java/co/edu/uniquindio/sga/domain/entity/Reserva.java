package co.edu.uniquindio.sga.domain.entity;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.ValueObject.*;

/**
 * Raíz del agregado Reserva.
 *
 * Invariantes que garantiza:
 *  - Toda reserva nace en estado PENDIENTE.
 *  - Solo se transita entre los estados permitidos por la sección 8 (RN-08).
 *  - Una reserva en estado terminal no admite ninguna modificación.
 *  - No se confirma una reserva sin hora estimada de llegada (RN-09).
 *  - No se registra la llegada antes de la fecha de entrada (RN-10).
 *  - El valor y la política quedan congelados y solo cambian por modificación explícita (RN-22).
 *  - Toda modificación revalida y recalcula, produciendo ajuste (RN-14).
 *  - El grupo de ocupantes no puede alterarse desde fuera del agregado.
 *  - Toda acción que cambia el estado queda registrada en el historial.
 */

public class Reserva {
	
	private final CodigoReserva codigo;                      // Identidad
    private final CanalOrigen canalOrigen;                   // Por dónde entró: no cambia nunca
    private final LocalDateTime creadaEn;
    private final Ocupante titular;
    private final IdentificacionApartamento apartamento;     // Referencia a OTRO agregado (por ID)
    private final VersionPolitica politica;                  // RN-13: congelada de por vida
    private final List<EventoReserva> historial = new ArrayList<>();

    private Estancia estancia;
    private List<Ocupante> ocupantes;
    private EstadoReserva estado;
    private LocalTime horaEstimadaLlegada;
    private ValorCongelado valor;

    private Reserva(CodigoReserva codigo, IdentificacionApartamento apartamento,
                    Estancia estancia, Ocupante titular, List<Ocupante> ocupantes,
                    CanalOrigen canalOrigen, ValorCongelado valor,
                    VersionPolitica politica, LocalDateTime creadaEn) {
        this.codigo = codigo;
        this.apartamento = apartamento;
        this.estancia = estancia;
        this.titular = titular;
        this.ocupantes = List.copyOf(ocupantes); // Copia inmutable: nadie la altera por fuera
        this.canalOrigen = canalOrigen;
        this.valor = valor;
        this.politica = politica;
        this.creadaEn = creadaEn;
        this.estado = EstadoReserva.PENDIENTE;   // Toda reserva nace PENDIENTE
    }

    /**
     * Única puerta de entrada para crear una reserva.
     * El valor ya llega calculado y la disponibilidad ya fue verificada externamente.
     */
    public static Reserva crear(CodigoReserva codigo, IdentificacionApartamento apartamento,
                                Estancia estancia, Ocupante titular, List<Ocupante> ocupantes,
                                CanalOrigen canalOrigen, ValorCongelado valor,
                                VersionPolitica politica, LocalDateTime ahora) {
        
        if (codigo == null || apartamento == null || estancia == null || titular == null 
            || canalOrigen == null || valor == null || politica == null) {
            throw new ReglaDominioException("Faltan datos obligatorios para crear la reserva");
        }
        if (ocupantes == null || ocupantes.isEmpty()) {
            throw new ReglaDominioException("La reserva debe tener al menos un ocupante");
        }
        if (!ocupantes.contains(titular)) {
            throw new ReglaDominioException("El titular debe ser uno de los ocupantes");
        }
        // RN-04: No se crean reservas hacia el pasado
        if (estancia.fechaEntrada().isBefore(ahora.toLocalDate())) {
            throw new ReglaDominioException("La fecha de entrada no puede ser anterior a la actual");
        }

        Reserva reserva = new Reserva(codigo, apartamento, estancia, titular, ocupantes, canalOrigen, valor, politica, ahora);
        reserva.registrarEvento("Creación", titular.getNombre(), ahora, null, EstadoReserva.PENDIENTE, "Reserva creada");
        return reserva;
    }

    public void indicarHoraEstimadaLlegada(LocalTime hora, String autor, LocalDateTime ahora) {
        verificarQueNoSeaTerminal();
        this.horaEstimadaLlegada = hora;
        registrarEvento("Indicar hora de llegada", autor, ahora, this.estado, this.estado, "Hora estimada: " + hora);
    }

    public void confirmar(String autor, LocalDateTime ahora) {
        // RN-09: No se confirma sin hora de llegada
        if (this.horaEstimadaLlegada == null) {
            throw new ReglaDominioException("No se puede confirmar una reserva sin hora estimada de llegada");
        }
        cambiarEstado(EstadoReserva.CONFIRMADA, "Confirmar reserva", autor, ahora, "Reserva confirmada");
    }

    public void registrarLlegada(String autor, LocalDateTime ahora) {
        // RN-10: No se registra llegada antes de la fecha de entrada
        if (ahora.toLocalDate().isBefore(this.estancia.fechaEntrada())) {
            throw new ReglaDominioException("No se puede registrar la llegada antes de la fecha de entrada");
        }
        cambiarEstado(EstadoReserva.EN_CURSO, "Check-in", autor, ahora, "Llegada registrada");
    }

    public void registrarSalida(String autor, LocalDateTime ahora) {
        cambiarEstado(EstadoReserva.FINALIZADA, "Check-out", autor, ahora, "Salida registrada");
    }

    public void cancelar(String motivo, String autor, LocalDateTime ahora) {
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaDominioException("Se requiere un motivo para cancelar la reserva");
        }
        cambiarEstado(EstadoReserva.CANCELADA, "Cancelar reserva", autor, ahora, motivo);
    }

    public void declararNoShow(String autor, LocalDateTime ahora) {
        cambiarEstado(EstadoReserva.NO_SHOW, "Declarar No-Show", autor, ahora, "El grupo no se presentó");
    }

    private void cambiarEstado(EstadoReserva nuevoEstado, String accion, String autor, LocalDateTime ahora, String observacion) {
        // RN-08: Verifica transiciones válidas antes de aplicar el cambio
        if (!this.estado.puedeTransicionarA(nuevoEstado)) {
            throw new ReglaDominioException("No se puede pasar de " + this.estado + " a " + nuevoEstado);
        }
        EstadoReserva estadoAnterior = this.estado;
        this.estado = nuevoEstado;
        registrarEvento(accion, autor, ahora, estadoAnterior, nuevoEstado, observacion);
    }

    private void registrarEvento(String accion, String autor, LocalDateTime ahora, EstadoReserva anterior, EstadoReserva nuevo, String obs) {
        this.historial.add(new EventoReserva(ahora, accion, autor, anterior, nuevo, obs));
    }

    private void verificarQueNoSeaTerminal() {
        if (this.estado.esTerminal()) {
            throw new ReglaDominioException("Una reserva en estado terminal no admite modificaciones");
        }
    }

    // Getters
    public CodigoReserva getCodigo() { return codigo; }
    public CanalOrigen getCanalOrigen() { return canalOrigen; }
    public LocalDateTime getCreadaEn() { return creadaEn; }
    public Ocupante getTitular() { return titular; }
    public IdentificacionApartamento getApartamento() { return apartamento; }
    public VersionPolitica getPolitica() { return politica; }
    public Estancia getEstancia() { return estancia; }
    public EstadoReserva getEstado() { return estado; }
    public LocalTime getHoraEstimadaLlegada() { return horaEstimadaLlegada; }
    public ValorCongelado getValor() { return valor; }

    public List<Ocupante> getOcupantes() {
        return Collections.unmodifiableList(ocupantes); // Protege la colección interna
    }

    public List<EventoReserva> obtenerHistorial() {
        return Collections.unmodifiableList(historial); // Protege el historial
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Reserva otra)) return false;
        return this.codigo.equals(otra.codigo);
    }

    @Override
    public int hashCode() {
        return codigo.hashCode();
    }

}
