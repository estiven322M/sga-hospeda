package co.edu.uniquindio.sga.domain.entity;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.*;

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
	
	private final CodigoReserva codigo;                      
    private final CanalOrigen canalOrigen;                   
    private final LocalDateTime creadaEn;
    private final Ocupante titular;
    private IdentificacionApartamento apartamento;           // Referencia a OTRO agregado; solo cambia por RN-14
    private final VersionPolitica politica;                  // RN-13, RN-22: congelada de por vida
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
        this.ocupantes = List.copyOf(ocupantes); // Copia inmutable inicial
        this.canalOrigen = canalOrigen;
        this.valor = valor;
        this.politica = politica;
        this.creadaEn = creadaEn;
        this.estado = EstadoReserva.PENDIENTE;   // Toda reserva nace PENDIENTE
    }

    public static Reserva crear(CodigoReserva codigo, IdentificacionApartamento apartamento,
                                Estancia estancia, Ocupante titular, List<Ocupante> ocupantes,
                                CanalOrigen canalOrigen, ValorCongelado valor,
                                VersionPolitica politica, LocalDateTime ahora) {

        if (codigo == null) throw new ReglaDominioException("La reserva debe tener un código");
        if (apartamento == null) throw new ReglaDominioException("La reserva debe indicar el apartamento");
        if (estancia == null) throw new ReglaDominioException("La reserva debe tener una estancia");
        if (titular == null) throw new ReglaDominioException("La reserva debe tener un titular");
        if (canalOrigen == null) throw new ReglaDominioException("La reserva debe indicar su canal de origen");
        if (valor == null || politica == null) {
            throw new ReglaDominioException("La reserva debe nacer con su valor y su política congelados");
        }
        if (ocupantes == null || ocupantes.isEmpty()) {
            throw new ReglaDominioException("La reserva debe tener al menos un ocupante");
        }
        if (!ocupantes.contains(titular)) {
            throw new ReglaDominioException("El titular debe ser uno de los ocupantes");
        }
        if (ocupantes.stream().distinct().count() != ocupantes.size()) {
            throw new ReglaDominioException("No se puede repetir un ocupante en la reserva");
        }
        
        // RN-04: no se crean reservas hacia el pasado
        if (estancia.fechaEntrada().isBefore(ahora.toLocalDate())) {
            throw new ReglaDominioException("La fecha de entrada no puede ser anterior a la fecha actual");
        }
        
        if (valor.noches() != estancia.noches()) {
            throw new ReglaDominioException("El desglose del valor no corresponde al número de noches de la estancia");
        }

        Reserva reserva = new Reserva(codigo, apartamento, estancia, titular, ocupantes,
                                      canalOrigen, valor, politica, ahora);
        reserva.registrarEvento("CREACION", titular.getNombre(), null,
                EstadoReserva.PENDIENTE, "Reserva creada por el canal " + canalOrigen, ahora);
        return reserva;
    }

    /**
     * Restaura una reserva que ya existía (la usa el adaptador de persistencia para reconstruirla desde la base de
     * datos). No es una operación de negocio: no genera eventos ni valida RN-04, porque restaurar una reserva
     * histórica no es crearla. Sí protege las invariantes que deben cumplirse en cualquier momento de su vida:
     * datos obligatorios, titular entre los ocupantes, desglose acorde a las noches y hora estimada de llegada en
     * toda reserva que alguna vez estuvo confirmada (RN-09).
     */
    public static Reserva reconstruir(CodigoReserva codigo, IdentificacionApartamento apartamento,
                                      Estancia estancia, Ocupante titular, List<Ocupante> ocupantes,
                                      CanalOrigen canalOrigen, ValorCongelado valor, VersionPolitica politica,
                                      LocalDateTime creadaEn, EstadoReserva estado, LocalTime horaEstimadaLlegada,
                                      List<EventoReserva> historial) {
        if (codigo == null || apartamento == null || estancia == null || titular == null || canalOrigen == null
                || valor == null || politica == null || creadaEn == null || estado == null || historial == null) {
            throw new ReglaDominioException("Faltan datos para reconstruir la reserva");
        }
        if (ocupantes == null || ocupantes.isEmpty()) {
            throw new ReglaDominioException("La reserva debe tener al menos un ocupante");
        }
        if (!ocupantes.contains(titular)) {
            throw new ReglaDominioException("El titular debe ser uno de los ocupantes");
        }
        if (ocupantes.stream().distinct().count() != ocupantes.size()) {
            throw new ReglaDominioException("No se puede repetir un ocupante en la reserva");
        }
        if (valor.noches() != estancia.noches()) {
            throw new ReglaDominioException("El desglose del valor no corresponde al número de noches de la estancia");
        }
        boolean estuvoConfirmada = estado == EstadoReserva.CONFIRMADA || estado == EstadoReserva.EN_CURSO
                || estado == EstadoReserva.FINALIZADA || estado == EstadoReserva.NO_SHOW;
        if (estuvoConfirmada && horaEstimadaLlegada == null) {
            throw new ReglaDominioException(
                    "Una reserva en estado " + estado + " debe tener hora estimada de llegada");
        }

        Reserva reserva = new Reserva(codigo, apartamento, estancia, titular, ocupantes, canalOrigen, valor,
                politica, creadaEn);
        reserva.estado = estado;
        reserva.horaEstimadaLlegada = horaEstimadaLlegada;
        reserva.historial.addAll(historial);
        return reserva;
    }

    // --- MÉTODOS DE NEGOCIO (Comportamiento) ---

    public void indicarHoraEstimadaLlegada(LocalTime hora, String autor, LocalDateTime ahora) {
        verificarQueNoEsteTerminada();
        if (hora == null) throw new ReglaDominioException("Debe indicarse la hora estimada de llegada");
        
        this.horaEstimadaLlegada = hora;
        registrarEvento("HORA_LLEGADA", autor, this.estado, this.estado,
                "Hora estimada de llegada: " + hora, ahora);
    }

    public void confirmar(String autor, LocalDateTime ahora) {
        verificarQueNoEsteTerminada();
        if (this.horaEstimadaLlegada == null) {
            throw new ReglaDominioException("No se puede confirmar una reserva sin hora estimada de llegada");
        }

        EstadoReserva anterior = this.estado;
        verificarTransicion(EstadoReserva.CONFIRMADA);

        this.estado = EstadoReserva.CONFIRMADA;
        registrarEvento("CONFIRMACION", autor, anterior, this.estado, "Reserva confirmada", ahora);
    }

    public void cancelar(String motivo, String autor, LocalDateTime ahora) {
        verificarQueNoEsteTerminada();
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaDominioException("Toda cancelación debe registrar un motivo");
        }

        EstadoReserva anterior = this.estado;
        verificarTransicion(EstadoReserva.CANCELADA);

        this.estado = EstadoReserva.CANCELADA;
        registrarEvento("CANCELACION", autor, anterior, this.estado,
                motivo + " | política aplicada: versión " + politica.numero(), ahora);
    }

    /** RN-21: ¿esta reserva está PENDIENTE y ya superó el plazo de confirmación? Solo consulta; no cambia nada. */
    public boolean haVencido(PlazoConfirmacion plazo, LocalDateTime ahora) {
        return this.estado == EstadoReserva.PENDIENTE && plazo.venceAntesDe(this.creadaEn, ahora);
    }

    public void vencer(PlazoConfirmacion plazo, LocalDateTime ahora) {
        if (this.estado != EstadoReserva.PENDIENTE) {
            throw new ReglaDominioException("Solo vence una reserva PENDIENTE");
        }
        if (!plazo.venceAntesDe(this.creadaEn, ahora)) {
            throw new ReglaDominioException("La reserva todavía está dentro del plazo de confirmación");
        }

        EstadoReserva anterior = this.estado;
        this.estado = EstadoReserva.CANCELADA;
        registrarEvento("VENCIMIENTO", "SISTEMA", anterior, this.estado,
                "Cancelada por vencimiento del plazo de confirmación", ahora);
    }

    public void registrarLlegada(String autor, LocalDateTime ahora) {
        verificarQueNoEsteTerminada();
        if (ahora.toLocalDate().isBefore(estancia.fechaEntrada())) {
            throw new ReglaDominioException("No se puede registrar la llegada antes de la fecha de entrada");
        }

        EstadoReserva anterior = this.estado;
        verificarTransicion(EstadoReserva.EN_CURSO);

        this.estado = EstadoReserva.EN_CURSO;
        registrarEvento("REGISTRO", autor, anterior, this.estado, "El grupo tomó el apartamento", ahora);
    }

    public void registrarSalida(String autor, LocalDateTime ahora) {
        verificarQueNoEsteTerminada();
        EstadoReserva anterior = this.estado;
        verificarTransicion(EstadoReserva.FINALIZADA);

        this.estado = EstadoReserva.FINALIZADA;
        registrarEvento("SALIDA", autor, anterior, this.estado, "El grupo salió del apartamento", ahora);
    }

    public void declararNoShow(LocalTime horaLimite, String autor, LocalDateTime ahora) {
        verificarQueNoEsteTerminada();
        if (horaLimite == null) throw new ReglaDominioException("Debe indicarse la hora límite de no-show");
        
        LocalDateTime limite = estancia.fechaEntrada().atTime(horaLimite);
        if (ahora.isBefore(limite)) {
            throw new ReglaDominioException("No se puede declarar no-show antes de la hora límite del día de entrada");
        }

        EstadoReserva anterior = this.estado;
        verificarTransicion(EstadoReserva.NO_SHOW);

        this.estado = EstadoReserva.NO_SHOW;
        registrarEvento("NO_SHOW", autor, anterior, this.estado,
                "El titular no se presentó | política aplicada: versión " + politica.numero(), ahora);
    }

    /**
     * Modificación de fechas u ocupantes (7.4, RN-14). Solo mientras la reserva no haya iniciado
     * (PENDIENTE o CONFIRMADA) y repitiendo las invariantes de la creación. La política congelada no cambia.
     * Devuelve la diferencia de valor (positiva o negativa) para registrarla como ajuste en el folio.
     */
    public Dinero modificarEstancia(Estancia nuevaEstancia, ValorCongelado nuevoValor, List<Ocupante> nuevosOcupantes, String autor, LocalDateTime ahora) {
        return modificarEstancia(this.apartamento, nuevaEstancia, nuevoValor, nuevosOcupantes, autor, ahora);
    }

    /**
     * Modificación que además puede cambiar de apartamento (RN-14). Las mismas condiciones aplican: la reserva no
     * debe haber iniciado ni estar terminada. El valor que llega ya está calculado con las tarifas del apartamento
     * destino; la política congelada se conserva. El cambio queda registrado en el historial.
     */
    public Dinero modificarEstancia(IdentificacionApartamento nuevoApartamento, Estancia nuevaEstancia, ValorCongelado nuevoValor,
                                    List<Ocupante> nuevosOcupantes, String autor, LocalDateTime ahora) {
        verificarQueNoEsteTerminada();
        if (nuevoApartamento == null) throw new ReglaDominioException("La reserva debe indicar el apartamento");
        if (this.estado == EstadoReserva.EN_CURSO) {
            throw new ReglaDominioException("No se puede modificar una reserva cuya estancia ya inició");
        }
        if (nuevaEstancia == null) throw new ReglaDominioException("La reserva debe tener una estancia");
        if (nuevoValor == null) throw new ReglaDominioException("La modificación debe traer el nuevo valor");
        if (nuevosOcupantes == null || nuevosOcupantes.isEmpty()) {
            throw new ReglaDominioException("La reserva debe tener al menos un ocupante");
        }
        if (!nuevosOcupantes.contains(titular)) {
            throw new ReglaDominioException("El titular debe ser uno de los ocupantes");
        }
        if (nuevosOcupantes.stream().distinct().count() != nuevosOcupantes.size()) {
            throw new ReglaDominioException("No se puede repetir un ocupante en la reserva");
        }
        // RN-04 se revalida al modificar (RN-14)
        if (nuevaEstancia.fechaEntrada().isBefore(ahora.toLocalDate())) {
            throw new ReglaDominioException("La fecha de entrada no puede ser anterior a la fecha actual");
        }
        if (nuevoValor.noches() != nuevaEstancia.noches()) {
             throw new ReglaDominioException("El desglose del valor no corresponde al número de noches de la estancia");
        }
        
        Dinero valorAnterior = this.valor.total();
        IdentificacionApartamento apartamentoAnterior = this.apartamento;
        this.apartamento = nuevoApartamento;
        this.estancia = nuevaEstancia;
        this.valor = nuevoValor;
        this.ocupantes = List.copyOf(nuevosOcupantes);
        
        Dinero diferencia = nuevoValor.total().menos(valorAnterior);
        
        registrarEvento("MODIFICACION", autor, this.estado, this.estado, 
                "Estancia/Ocupantes modificados"
                        + (apartamentoAnterior.equals(nuevoApartamento) ? ""
                                : " | apartamento: " + apartamentoAnterior.valor() + " -> " + nuevoApartamento.valor())
                        + ". Ajuste generado: " + diferencia.valor(), ahora);
        
        return diferencia;
    }

    // --- UTILIDADES PRIVADAS ---

    private void verificarQueNoEsteTerminada() {
        if (this.estado.esTerminal()) {
            throw new ReglaDominioException("Una reserva en estado terminal (" + this.estado + ") no admite modificaciones");
        }
    }

    private void verificarTransicion(EstadoReserva siguiente) {
        if (!this.estado.puedeTransicionarA(siguiente)) {
            throw new ReglaDominioException("No se puede pasar de " + this.estado + " a " + siguiente);
        }
    }

    private void registrarEvento(String accion, String autor, EstadoReserva estadoAnterior, 
                                 EstadoReserva estadoNuevo, String observacion, LocalDateTime ahora) {
        this.historial.add(new EventoReserva(ahora, accion, autor, estadoAnterior, estadoNuevo, observacion));
    }

    // --- GETTERS PROTEGIDOS ---

    public CodigoReserva getCodigo() { return codigo; }
    public EstadoReserva getEstado() { return estado; }
    public Estancia getEstancia() { return estancia; }
    public IdentificacionApartamento getApartamento() { return apartamento; }
    public CanalOrigen getCanalOrigen() { return canalOrigen; }
    public LocalDateTime getCreadaEn() { return creadaEn; }
    public Ocupante getTitular() { return titular; }
    public VersionPolitica getPolitica() { return politica; }
    public ValorCongelado getValor() { return valor; }
    public LocalTime getHoraEstimadaLlegada() { return horaEstimadaLlegada; }

    public List<Ocupante> getOcupantes() {
        return List.copyOf(ocupantes); // Retorna copia inmutable
    }

    public List<EventoReserva> getHistorial() {
        return List.copyOf(historial); // Retorna copia inmutable
    }

}
