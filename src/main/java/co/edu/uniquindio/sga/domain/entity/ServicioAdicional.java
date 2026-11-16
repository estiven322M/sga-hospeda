package co.edu.uniquindio.sga.domain.entity;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.Dinero;

/**
 * Servicio adicional que el alojamiento ofrece (L-17, 7.3): desayuno, lavandería, transporte... Indica si genera cargo y
 * su valor en COP. Su identidad es el código, único y estable. No se borra: se da de baja (baja lógica, regla 11) y
 * se puede volver a dar de alta; mientras esté de baja no se puede cargar a un folio.
 */
public class ServicioAdicional {

    public static final int MAX_CODIGO = 30;
    public static final int MAX_NOMBRE = 80;
    public static final int MAX_DESCRIPCION = 300;

    private final String codigo; // identidad
    private String nombre;
    private String descripcion;
    private boolean generaCargo;
    private Dinero valor;
    private boolean activo;

    /** Un servicio nuevo nace activo. */
    public ServicioAdicional(String codigo, String nombre, String descripcion, boolean generaCargo, Dinero valor) {
        if (codigo == null || codigo.isBlank()) {
            throw new ReglaDominioException("El servicio adicional debe tener un código");
        }
        String codigoValido = codigo.trim().toUpperCase();
        if (codigoValido.length() > MAX_CODIGO) {
            throw new ReglaDominioException("El código del servicio adicional no puede superar los " + MAX_CODIGO + " caracteres");
        }
        this.codigo = codigoValido;
        aplicar(nombre, descripcion, generaCargo, valor);
        this.activo = true;
    }

    /** Restaura un servicio ya guardado, con su estado de actividad (la usa el adaptador de persistencia). */
    public static ServicioAdicional reconstruir(String codigo, String nombre, String descripcion, boolean generaCargo,
                                                Dinero valor, boolean activo) {
        ServicioAdicional servicio = new ServicioAdicional(codigo, nombre, descripcion, generaCargo, valor);
        servicio.activo = activo;
        return servicio;
    }

    /** Edición atómica de los datos del servicio: si algo es inválido no se modifica nada. */
    public void actualizarDatos(String nuevoNombre, String nuevaDescripcion, boolean nuevoGeneraCargo, Dinero nuevoValor) {
        aplicar(nuevoNombre, nuevaDescripcion, nuevoGeneraCargo, nuevoValor);
    }

    private void aplicar(String nuevoNombre, String nuevaDescripcion, boolean nuevoGeneraCargo, Dinero nuevoValor) {
        if (nuevoNombre == null || nuevoNombre.isBlank()) {
            throw new ReglaDominioException("El servicio adicional debe tener un nombre");
        }
        String nombreValido = nuevoNombre.trim();
        if (nombreValido.length() > MAX_NOMBRE) {
            throw new ReglaDominioException("El nombre del servicio adicional no puede superar los " + MAX_NOMBRE + " caracteres");
        }
        String descripcionValida = nuevaDescripcion == null ? "" : nuevaDescripcion.trim();
        if (descripcionValida.length() > MAX_DESCRIPCION) {
            throw new ReglaDominioException("La descripción del servicio adicional no puede superar los " + MAX_DESCRIPCION + " caracteres");
        }
        if (nuevoValor == null || nuevoValor.esNegativo()) {
            throw new ReglaDominioException("El valor del servicio adicional no puede ser negativo");
        }
        // Coherencia: un servicio que genera cargo cuesta algo; uno que no lo genera (cortesía) no lleva valor
        if (nuevoGeneraCargo && nuevoValor.esCero()) {
            throw new ReglaDominioException("Un servicio adicional que genera cargo debe tener un valor mayor que cero");
        }
        if (!nuevoGeneraCargo && !nuevoValor.esCero()) {
            throw new ReglaDominioException("Un servicio adicional que no genera cargo no puede tener valor");
        }
        this.nombre = nombreValido;
        this.descripcion = descripcionValida;
        this.generaCargo = nuevoGeneraCargo;
        this.valor = nuevoValor;
    }

    /** Baja lógica: deja de poderse cargar, pero el registro y los cargos ya hechos se conservan. */
    public void darDeBaja() {
        if (!activo) {
            throw new ReglaDominioException("El servicio adicional " + codigo + " ya está dado de baja");
        }
        this.activo = false;
    }

    public void activar() {
        if (activo) {
            throw new ReglaDominioException("El servicio adicional " + codigo + " ya está activo");
        }
        this.activo = true;
    }

    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public boolean generaCargo() { return generaCargo; }
    public Dinero getValor() { return valor; }
    public boolean isActivo() { return activo; }

    // Igualdad basada EXCLUSIVAMENTE en la identidad
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServicioAdicional otro)) return false;
        return codigo.equals(otro.codigo);
    }

    @Override
    public int hashCode() {
        return codigo.hashCode();
    }
}
