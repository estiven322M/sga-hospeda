package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Hay un solo alojamiento: su fila tiene siempre el identificador {@link #ID_UNICO}. */
@Entity
@Table(name = "alojamiento")
public class AlojamientoEntity {

    public static final long ID_UNICO = 1L;

    @Id
    private Long id = ID_UNICO;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @Column(name = "descripcion", nullable = false, length = 1000)
    private String descripcion;

    @Column(name = "ciudad", nullable = false, length = 80)
    private String ciudad;

    @Column(name = "direccion", nullable = false, length = 160)
    private String direccion;

    @Column(name = "latitud", nullable = false)
    private double latitud;

    @Column(name = "longitud", nullable = false)
    private double longitud;

    @Column(name = "hora_entrada", nullable = false)
    private LocalTime horaEntrada;

    @Column(name = "hora_salida", nullable = false)
    private LocalTime horaSalida;

    @Column(name = "normas_convivencia", nullable = false, length = 2000)
    private String normasConvivencia;

    @Column(name = "politica_mascotas", nullable = false, length = 2000)
    private String politicaMascotas;

    protected AlojamientoEntity() {
    }

    public AlojamientoEntity(String nombre, String descripcion, String ciudad, String direccion, double latitud,
                             double longitud, LocalTime horaEntrada, LocalTime horaSalida, String normasConvivencia,
                             String politicaMascotas) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.ciudad = ciudad;
        this.direccion = direccion;
        this.latitud = latitud;
        this.longitud = longitud;
        this.horaEntrada = horaEntrada;
        this.horaSalida = horaSalida;
        this.normasConvivencia = normasConvivencia;
        this.politicaMascotas = politicaMascotas;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getCiudad() { return ciudad; }
    public String getDireccion() { return direccion; }
    public double getLatitud() { return latitud; }
    public double getLongitud() { return longitud; }
    public LocalTime getHoraEntrada() { return horaEntrada; }
    public LocalTime getHoraSalida() { return horaSalida; }
    public String getNormasConvivencia() { return normasConvivencia; }
    public String getPoliticaMascotas() { return politicaMascotas; }

    @Override
    public boolean equals(Object o) {
        return o instanceof AlojamientoEntity;
    }

    @Override
    public int hashCode() {
        return AlojamientoEntity.class.hashCode();
    }
}
