package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import co.edu.uniquindio.sga.infrastructure.security.RolUsuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Usuario del sistema (Guía 12): concepto de ACCESO, no del negocio, por eso vive en infraestructura (3.6).
 * El único puente con el negocio es el documento: si coincide con el del titular de una reserva, el usuario
 * es su propietario. La contraseña se guarda siempre como hash ({bcrypt}...), nunca en texto plano.
 */
@Entity
@Table(name = "usuario")
public class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "correo", nullable = false, unique = true, length = 120)
    private String correo;

    @Column(name = "contrasena", nullable = false, length = 100)
    private String contrasena;          // hash BCrypt, nunca texto plano

    @Column(name = "telefono", nullable = false, length = 20)
    private String telefono;

    @Column(name = "documento", nullable = false, unique = true, length = 15)
    private String documento;           // puente con el titular de la reserva (3.6)

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 20)
    private RolUsuario rol;

    @Column(name = "activo", nullable = false)
    private boolean activo;             // eliminación lógica (7.2 y 12.4): un usuario inactivo no inicia sesión

    protected UsuarioEntity() {
    }

    /** Crea un usuario activo. La contraseña llega ya cifrada por el PasswordEncoder. */
    public UsuarioEntity(String nombre, String correo, String contrasenaCifrada, String telefono, String documento,
                         LocalDate fechaNacimiento, RolUsuario rol) {
        this.nombre = nombre;
        this.correo = correo;
        this.contrasena = contrasenaCifrada;
        this.telefono = telefono;
        this.documento = documento;
        this.fechaNacimiento = fechaNacimiento;
        this.rol = rol;
        this.activo = true;
    }

    /** Eliminación lógica: el registro se conserva, pero el usuario ya no puede iniciar sesión. */
    public void desactivar() {
        this.activo = false;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getContrasena() { return contrasena; }
    public String getTelefono() { return telefono; }
    public String getDocumento() { return documento; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public RolUsuario getRol() { return rol; }
    public boolean isActivo() { return activo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UsuarioEntity otro)) return false;
        return id != null && Objects.equals(id, otro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
