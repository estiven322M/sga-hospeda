package co.edu.uniquindio.sga.domain.entity;

import java.time.LocalTime;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.Ubicacion;

/**
 * Los datos del alojamiento (7.3, L-01, L-02, L-12, L-16): nombre e identidad, ciudad y ubicación, horas de entrada y
 * salida, normas de convivencia y la decisión explícita sobre las mascotas. Hay UN solo alojamiento por sistema, por
 * eso su identidad es la del propio alojamiento (todas las instancias son "el mismo"). Se siembra desde
 * application.properties y el administrador lo edita; no tiene setters: se cambia con {@link #actualizarDatos}.
 */
public class Alojamiento {

    public static final int MAX_NOMBRE = 80;
    public static final int MAX_DESCRIPCION = 1000;
    public static final int MAX_TEXTO_LARGO = 2000;

    private String nombre;
    private String descripcion;
    private String ciudad;
    private String direccion;
    private Ubicacion ubicacion;
    private LocalTime horaEntrada;
    private LocalTime horaSalida;
    private String normasConvivencia;
    private String politicaMascotas;

    public Alojamiento(String nombre, String descripcion, String ciudad, String direccion, Ubicacion ubicacion,
                       LocalTime horaEntrada, LocalTime horaSalida, String normasConvivencia, String politicaMascotas) {
        aplicar(nombre, descripcion, ciudad, direccion, ubicacion, horaEntrada, horaSalida, normasConvivencia,
                politicaMascotas);
    }

    /**
     * Edición de los datos del alojamiento. Es atómica: si algún dato es inválido no se modifica nada (las
     * validaciones corren antes de asignar).
     */
    public void actualizarDatos(String nombre, String descripcion, String ciudad, String direccion, Ubicacion ubicacion,
                                LocalTime horaEntrada, LocalTime horaSalida, String normasConvivencia,
                                String politicaMascotas) {
        aplicar(nombre, descripcion, ciudad, direccion, ubicacion, horaEntrada, horaSalida, normasConvivencia,
                politicaMascotas);
    }

    private void aplicar(String nuevoNombre, String nuevaDescripcion, String nuevaCiudad, String nuevaDireccion,
                         Ubicacion nuevaUbicacion, LocalTime nuevaEntrada, LocalTime nuevaSalida, String nuevasNormas,
                         String nuevaPoliticaMascotas) {
        String nombreValido = obligatorio(nuevoNombre, "El alojamiento debe tener un nombre", MAX_NOMBRE, "El nombre");
        String descripcionValida = obligatorio(nuevaDescripcion, "El alojamiento debe tener una descripción",
                MAX_DESCRIPCION, "La descripción");
        String ciudadValida = obligatorio(nuevaCiudad, "El alojamiento debe indicar su ciudad", MAX_NOMBRE, "La ciudad");
        String direccionValida = obligatorio(nuevaDireccion, "El alojamiento debe indicar su dirección", MAX_NOMBRE * 2,
                "La dirección");
        if (nuevaUbicacion == null) {
            throw new ReglaDominioException("El alojamiento debe indicar su ubicación (latitud y longitud)");
        }
        if (nuevaEntrada == null || nuevaSalida == null) {
            throw new ReglaDominioException("El alojamiento debe definir la hora de entrada y la hora de salida");
        }
        // Salida por la mañana, entrada por la tarde: el apartamento se prepara entre las dos (L-12, L-13)
        if (!nuevaSalida.isBefore(nuevaEntrada)) {
            throw new ReglaDominioException("La hora de salida debe ser anterior a la hora de entrada");
        }
        String normasValidas = obligatorio(nuevasNormas, "El alojamiento debe tener normas de convivencia",
                MAX_TEXTO_LARGO, "Las normas de convivencia");
        String mascotasValida = obligatorio(nuevaPoliticaMascotas,
                "El alojamiento debe definir su política de mascotas (aunque sea que no se aceptan)", MAX_TEXTO_LARGO,
                "La política de mascotas");

        this.nombre = nombreValido;
        this.descripcion = descripcionValida;
        this.ciudad = ciudadValida;
        this.direccion = direccionValida;
        this.ubicacion = nuevaUbicacion;
        this.horaEntrada = nuevaEntrada;
        this.horaSalida = nuevaSalida;
        this.normasConvivencia = normasValidas;
        this.politicaMascotas = mascotasValida;
    }

    private static String obligatorio(String texto, String mensajeSiFalta, int maximo, String sujeto) {
        if (texto == null || texto.isBlank()) {
            throw new ReglaDominioException(mensajeSiFalta);
        }
        String limpio = texto.trim();
        if (limpio.length() > maximo) {
            throw new ReglaDominioException(sujeto + " no puede superar los " + maximo + " caracteres");
        }
        return limpio;
    }

    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getCiudad() { return ciudad; }
    public String getDireccion() { return direccion; }
    public Ubicacion getUbicacion() { return ubicacion; }
    public LocalTime getHoraEntrada() { return horaEntrada; }
    public LocalTime getHoraSalida() { return horaSalida; }
    public String getNormasConvivencia() { return normasConvivencia; }
    public String getPoliticaMascotas() { return politicaMascotas; }

    /** Hay un único alojamiento: dos instancias representan al mismo establecimiento. */
    @Override
    public boolean equals(Object o) {
        return o instanceof Alojamiento;
    }

    @Override
    public int hashCode() {
        return Alojamiento.class.hashCode();
    }
}
