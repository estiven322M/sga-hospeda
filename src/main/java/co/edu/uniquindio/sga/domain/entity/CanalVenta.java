package co.edu.uniquindio.sga.domain.entity;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.IdentificadorExterno;
import co.edu.uniquindio.sga.domain.valueobject.TipoCanalVenta;

/**
 * Canal de venta externo (7.8): nombre, tipo, credencial de acceso (solo su hash) y baja lógica. Su código es su identidad y es la
 * parte del canal en "canal + identificador externo" (RN-19): el identificador con el que se guarda una reserva externa lleva el
 * código del canal por delante ({@code DEMO:RES-9001}), así dos canales distintos pueden usar el mismo identificador propio sin
 * chocar, y un canal solo puede cancelar las reservas que él mismo creó.
 * Un canal desactivado conserva su historial y sus reservas, pero su credencial deja de servir.
 */
public class CanalVenta {

    private static final Pattern FORMATO_CODIGO = Pattern.compile("^[A-Z0-9][A-Z0-9_-]{1,11}$");
    public static final int MAXIMO_NOMBRE = 80;
    private static final String SEPARADOR = ":";

    private final String codigo;
    private String nombre;
    private final TipoCanalVenta tipo;
    private final String credencialHash;
    private boolean activo;

    public CanalVenta(String codigo, String nombre, TipoCanalVenta tipo, String credencialHash) {
        this(codigo, nombre, tipo, credencialHash, true);
    }

    private CanalVenta(String codigo, String nombre, TipoCanalVenta tipo, String credencialHash, boolean activo) {
        String codigoNormalizado = codigo == null ? "" : codigo.trim().toUpperCase(Locale.ROOT);
        if (!FORMATO_CODIGO.matcher(codigoNormalizado).matches()) {
            throw new ReglaDominioException("El código del canal debe tener de 2 a 12 caracteres: letras, números, guion o guion bajo");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDominioException("El canal debe tener un nombre");
        }
        if (nombre.trim().length() > MAXIMO_NOMBRE) {
            throw new ReglaDominioException("El nombre del canal no puede superar " + MAXIMO_NOMBRE + " caracteres");
        }
        if (tipo == null) {
            throw new ReglaDominioException("El canal debe tener un tipo");
        }
        if (credencialHash == null || credencialHash.isBlank()) {
            throw new ReglaDominioException("El canal debe tener una credencial");
        }
        this.codigo = codigoNormalizado;
        this.nombre = nombre.trim();
        this.tipo = tipo;
        this.credencialHash = credencialHash;
        this.activo = activo;
    }

    /** Restaura un canal guardado (persistencia), en el estado de actividad en que se guardó. */
    public static CanalVenta reconstruir(String codigo, String nombre, TipoCanalVenta tipo, String credencialHash, boolean activo) {
        return new CanalVenta(codigo, nombre, tipo, credencialHash, activo);
    }

    /** Baja lógica: la credencial deja de servir; el canal, su historial y sus reservas se conservan. */
    public void desactivar() {
        if (!activo) {
            throw new ReglaDominioException("El canal " + codigo + " ya está desactivado");
        }
        this.activo = false;
    }

    public void activar() {
        if (activo) {
            throw new ReglaDominioException("El canal " + codigo + " ya está activo");
        }
        this.activo = true;
    }

    /** ¿Coincide el hash de la credencial recibida con el de este canal? */
    public boolean aceptaCredencial(String hashRecibido) {
        return activo && credencialHash.equals(hashRecibido);
    }

    /**
     * El identificador con el que se guarda una reserva de este canal: su código + el identificador propio del canal. Debe caber en
     * los 60 caracteres de IdentificadorExterno (RN-19).
     */
    public IdentificadorExterno identificadorPara(String identificadorPropio) {
        if (identificadorPropio == null || identificadorPropio.isBlank()) {
            throw new ReglaDominioException("El identificador externo es obligatorio");
        }
        String propio = identificadorPropio.trim();
        int maximo = maximoIdentificadorPropio();
        if (propio.length() > maximo) {
            throw new ReglaDominioException("El identificador externo de este canal no puede superar " + maximo + " caracteres");
        }
        return new IdentificadorExterno(codigo + SEPARADOR + propio);
    }

    /** Lo contrario: el identificador tal como lo conoce el canal (sin el código por delante). */
    public String identificadorPropio(IdentificadorExterno guardado) {
        String valor = guardado.valor();
        return valor.startsWith(codigo + SEPARADOR) ? valor.substring(codigo.length() + SEPARADOR.length()) : valor;
    }

    /** ¿Es una reserva de este canal? Un canal solo cancela las suyas. */
    public boolean esDeEsteCanal(IdentificadorExterno guardado) {
        return guardado != null && guardado.valor().startsWith(codigo + SEPARADOR);
    }

    public int maximoIdentificadorPropio() {
        return IdentificadorExterno.LONGITUD_MAXIMA - codigo.length() - SEPARADOR.length();
    }

    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public TipoCanalVenta getTipo() { return tipo; }
    public String getCredencialHash() { return credencialHash; }
    public boolean isActivo() { return activo; }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof CanalVenta otro && codigo.equals(otro.codigo));
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo);
    }
}
