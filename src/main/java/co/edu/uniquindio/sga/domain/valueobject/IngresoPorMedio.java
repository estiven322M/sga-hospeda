package co.edu.uniquindio.sga.domain.valueobject;

/** Total de los pagos recibidos por un medio de pago dentro de un periodo (7.9, L-19). */
public record IngresoPorMedio(String medio, Dinero total) {
}
