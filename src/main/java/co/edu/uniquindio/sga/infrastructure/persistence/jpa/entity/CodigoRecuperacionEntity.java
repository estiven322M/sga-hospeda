package co.edu.uniquindio.sga.infrastructure.persistence.jpa.entity;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Código de recuperación de contraseña (7.2). Una fila por correo: un código nuevo reemplaza al anterior. Guarda el
 * HASH del código, nunca el código en claro.
 */
@Entity
@Table(name = "codigo_recuperacion")
public class CodigoRecuperacionEntity {

    @Id
    @Column(name = "correo", nullable = false, length = 120)
    private String correo;

    @Column(name = "hash", nullable = false, length = 100)
    private String hash;

    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    @Column(name = "usado", nullable = false)
    private boolean usado;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;

    protected CodigoRecuperacionEntity() {
    }

    public CodigoRecuperacionEntity(String correo, String hash, LocalDateTime expiraEn, boolean usado,
                                    int intentosFallidos) {
        this.correo = correo;
        this.hash = hash;
        this.expiraEn = expiraEn;
        this.usado = usado;
        this.intentosFallidos = intentosFallidos;
    }

    public String getCorreo() { return correo; }
    public String getHash() { return hash; }
    public LocalDateTime getExpiraEn() { return expiraEn; }
    public boolean isUsado() { return usado; }
    public int getIntentosFallidos() { return intentosFallidos; }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof CodigoRecuperacionEntity otro && Objects.equals(correo, otro.correo));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(correo);
    }
}
