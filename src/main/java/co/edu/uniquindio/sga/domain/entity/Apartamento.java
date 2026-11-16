package co.edu.uniquindio.sga.domain.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import co.edu.uniquindio.sga.domain.exception.ReglaDominioException;
import co.edu.uniquindio.sga.domain.valueobject.Caracteristica;
import co.edu.uniquindio.sga.domain.valueobject.EstadoOperativo;
import co.edu.uniquindio.sga.domain.valueobject.IdentificacionApartamento;
import co.edu.uniquindio.sga.domain.valueobject.ImagenApartamento;
import co.edu.uniquindio.sga.domain.valueobject.ReferenciaImagen;

/**
 * Raíz de Agregado que representa una unidad autónoma que se alquila.
 * Protege sus propias reglas de capacidad y estado físico, y la coherencia de su descripción, dotación
 * y características (L-06).
 */

public class Apartamento {

    /** Longitudes máximas de la descripción y de la dotación (L-06). */
    public static final int MAX_DESCRIPCION = 500;
    public static final int MAX_ITEMS_DOTACION = 30;
    public static final int MAX_LONGITUD_ITEM_DOTACION = 60;
    /** 7.3: un apartamento tiene entre 1 y 10 imágenes (las retiradas no cuentan). */
    public static final int MAX_IMAGENES = 10;

    private final IdentificacionApartamento identificacion; // Identidad única

    private String nombre;
    private String descripcion;
    private int dormitorios;
    private int capacidad;
    private List<String> dotacion;                // lista ordenada, sin repetidos
    private Set<Caracteristica> caracteristicas;  // catálogo cerrado
    private EstadoOperativo estadoOperativo;
    private boolean activo;
    /** 7.3: todas las imágenes, en el orden en que se agregaron, incluidas las retiradas (el retiro es lógico). */
    private List<ImagenApartamento> imagenes = new ArrayList<>();

    public Apartamento(IdentificacionApartamento identificacion, String nombre, String descripcion,
                       int dormitorios, int capacidad, Collection<String> dotacion,
                       Collection<Caracteristica> caracteristicas) {
        if (identificacion == null) {
            throw new ReglaDominioException("El apartamento debe tener una identificación");
        }
        validarNombre(nombre);
        String descripcionValida = validarDescripcion(descripcion);
        validarDormitorios(dormitorios);
        validarCapacidad(capacidad);
        List<String> dotacionValida = validarDotacion(dotacion);
        Set<Caracteristica> caracteristicasValidas = validarCaracteristicas(caracteristicas);

        this.identificacion = identificacion;
        this.nombre = nombre;
        this.descripcion = descripcionValida;
        this.dormitorios = dormitorios;
        this.capacidad = capacidad;
        this.dotacion = dotacionValida;
        this.caracteristicas = caracteristicasValidas;
        this.estadoOperativo = EstadoOperativo.PREPARADO; // Estado inicial por defecto
        // 7.4: nace inactivo; solo se activa con tarifas completas (ActivacionApartamentoService)
        this.activo = false;
    }

    /**
     * Edición de los datos del apartamento (7.3: cambiar la capacidad no afecta las reservas ya creadas).
     * Es atómica: valida todos los datos antes de aplicar alguno, así que si algo falla no se modifica nada.
     */
    public void actualizarDatos(String nuevoNombre, String nuevaDescripcion, int nuevosDormitorios,
                                int nuevaCapacidad, Collection<String> nuevaDotacion,
                                Collection<Caracteristica> nuevasCaracteristicas) {
        validarNombre(nuevoNombre);
        String descripcionValida = validarDescripcion(nuevaDescripcion);
        validarDormitorios(nuevosDormitorios);
        validarCapacidad(nuevaCapacidad);
        List<String> dotacionValida = validarDotacion(nuevaDotacion);
        Set<Caracteristica> caracteristicasValidas = validarCaracteristicas(nuevasCaracteristicas);

        this.nombre = nuevoNombre;
        this.descripcion = descripcionValida;
        this.dormitorios = nuevosDormitorios;
        this.capacidad = nuevaCapacidad;
        this.dotacion = dotacionValida;
        this.caracteristicas = caracteristicasValidas;
    }

    /**
     * Restaura un apartamento que ya existía, en el estado operativo y de actividad en que se guardó (la usa el
     * adaptador de persistencia). Aplica las mismas validaciones de datos que el constructor; no es una operación de
     * negocio, así que no revisa transiciones.
     */
    public static Apartamento reconstruir(IdentificacionApartamento identificacion, String nombre, String descripcion,
                                          int dormitorios, int capacidad, Collection<String> dotacion,
                                          Collection<Caracteristica> caracteristicas,
                                          EstadoOperativo estadoOperativo, boolean activo) {
        if (estadoOperativo == null) {
            throw new ReglaDominioException("El apartamento debe tener un estado operativo");
        }
        Apartamento apartamento = new Apartamento(identificacion, nombre, descripcion, dormitorios, capacidad,
                dotacion, caracteristicas);
        apartamento.estadoOperativo = estadoOperativo;
        apartamento.activo = activo;
        return apartamento;
    }

    /**
     * Igual que {@link #reconstruir(IdentificacionApartamento, String, String, int, int, Collection, Collection, EstadoOperativo,
     * boolean)} pero con las imágenes guardadas. Verifica que sean coherentes: a lo sumo {@value #MAX_IMAGENES} sin retirar y,
     * si hay alguna, exactamente una principal (7.3).
     */
    public static Apartamento reconstruir(IdentificacionApartamento identificacion, String nombre, String descripcion,
                                          int dormitorios, int capacidad, Collection<String> dotacion,
                                          Collection<Caracteristica> caracteristicas,
                                          EstadoOperativo estadoOperativo, boolean activo,
                                          Collection<ImagenApartamento> imagenes) {
        Apartamento apartamento = reconstruir(identificacion, nombre, descripcion, dormitorios, capacidad, dotacion,
                caracteristicas, estadoOperativo, activo);
        List<ImagenApartamento> restauradas = imagenes == null ? List.of() : List.copyOf(imagenes);
        validarCoherenciaDeImagenes(restauradas);
        apartamento.imagenes = new ArrayList<>(restauradas);
        return apartamento;
    }

    // ---------- Imágenes (7.3) ----------

    /** Cuántas imágenes tiene en la galería (las retiradas no cuentan). */
    public int cantidadImagenes() {
        return (int) imagenes.stream().filter(imagen -> !imagen.retirada()).count();
    }

    /**
     * 7.3: máximo {@value #MAX_IMAGENES} imágenes. Se revisa ANTES de subir nada al almacén externo, para no dejar archivos
     * huérfanos cuando el lote no cabe.
     */
    public void verificarCupoImagenes(int nuevas) {
        if (nuevas < 1) {
            throw new ReglaDominioException("Debe enviar al menos una imagen");
        }
        int actuales = cantidadImagenes();
        if (actuales + nuevas > MAX_IMAGENES) {
            throw new ReglaDominioException("Un apartamento admite como máximo " + MAX_IMAGENES + " imágenes: ya tiene "
                    + actuales + " y se intentan agregar " + nuevas);
        }
    }

    /**
     * Agrega imágenes ya guardadas en el almacén. Si el apartamento no tenía ninguna, la primera queda como principal (7.3:
     * exactamente una principal cuando hay al menos una). Devuelve las imágenes agregadas.
     */
    public List<ImagenApartamento> agregarImagenes(List<ReferenciaImagen> guardadas) {
        verificarCupoImagenes(guardadas == null ? 0 : guardadas.size());
        boolean sinPrincipal = cantidadImagenes() == 0;
        List<ImagenApartamento> agregadas = new ArrayList<>();
        for (ReferenciaImagen guardada : guardadas) {
            ImagenApartamento nueva = new ImagenApartamento(UUID.randomUUID().toString(), guardada.url(),
                    guardada.referencia(), sinPrincipal && agregadas.isEmpty(), false);
            agregadas.add(nueva);
            imagenes.add(nueva);
        }
        return List.copyOf(agregadas);
    }

    /** La imagen con ese identificador, si está en la galería (no retirada). */
    public Optional<ImagenApartamento> buscarImagen(String id) {
        return imagenes.stream().filter(imagen -> !imagen.retirada() && imagen.id().equals(id)).findFirst();
    }

    /** Cambia cuál es la principal: la anterior deja de serlo, así que siempre hay exactamente una. */
    public void establecerImagenPrincipal(String id) {
        ImagenApartamento elegida = buscarImagen(id).orElseThrow(() ->
                new ReglaDominioException("El apartamento no tiene una imagen con el identificador " + id));
        imagenes.replaceAll(imagen -> imagen.retirada() ? imagen
                : imagen.id().equals(elegida.id()) ? imagen.comoPrincipal() : imagen.sinSerPrincipal());
    }

    /**
     * Retiro LÓGICO de una imagen: sale de la galería pero el registro se conserva. Si era la principal, la primera de las que
     * quedan pasa a serlo. Un apartamento ACTIVO debe conservar al menos una imagen (7.3), así que no se le retira la última.
     */
    public void retirarImagen(String id) {
        ImagenApartamento retirada = buscarImagen(id).orElseThrow(() ->
                new ReglaDominioException("El apartamento no tiene una imagen con el identificador " + id));
        if (activo && cantidadImagenes() == 1) {
            throw new ReglaDominioException("Un apartamento activo debe conservar al menos una imagen: "
                    + "agregue otra antes de retirar la última, o dé de baja el apartamento");
        }
        imagenes.replaceAll(imagen -> imagen.id().equals(retirada.id()) ? imagen.retirar() : imagen);
        boolean sinPrincipal = imagenes.stream().noneMatch(imagen -> !imagen.retirada() && imagen.principal());
        if (sinPrincipal) {
            imagenes.stream().filter(imagen -> !imagen.retirada()).findFirst()
                    .ifPresent(primera -> imagenes.replaceAll(imagen -> imagen.id().equals(primera.id()) ? imagen.comoPrincipal() : imagen));
        }
    }

    private static void validarCoherenciaDeImagenes(List<ImagenApartamento> imagenes) {
        List<ImagenApartamento> activas = imagenes.stream().filter(imagen -> !imagen.retirada()).toList();
        if (activas.size() > MAX_IMAGENES) {
            throw new ReglaDominioException("Un apartamento admite como máximo " + MAX_IMAGENES + " imágenes");
        }
        long principales = activas.stream().filter(ImagenApartamento::principal).count();
        if (!activas.isEmpty() && principales != 1) {
            throw new ReglaDominioException("Un apartamento con imágenes debe tener exactamente una principal");
        }
        if (imagenes.stream().map(ImagenApartamento::id).distinct().count() != imagenes.size()) {
            throw new ReglaDominioException("Las imágenes de un apartamento no pueden repetir su identificador");
        }
    }

    /** RN-02: Verifica si el apartamento puede alojar a un grupo específico. */
    public boolean admite(int totalOcupantes) {
        return totalOcupantes > 0 && totalOcupantes <= this.capacidad;
    }

    /**
     * L-06, filtro de búsqueda (7.5): ¿tiene el apartamento TODAS las características pedidas?
     * Sin características pedidas no se filtra, así que cualquier apartamento cumple.
     */
    public boolean tieneTodas(Collection<Caracteristica> pedidas) {
        return pedidas == null || caracteristicas.containsAll(pedidas);
    }

    /** Registro del grupo: PREPARADO → OCUPADO. */
    public void marcarOcupado() {
        verificarTransicionOperativa(EstadoOperativo.OCUPADO);
        this.estadoOperativo = EstadoOperativo.OCUPADO;
    }

    /** Salida del grupo: OCUPADO → PENDIENTE_PREPARACION. */
    public void liberar() {
        verificarTransicionOperativa(EstadoOperativo.PENDIENTE_PREPARACION);
        this.estadoOperativo = EstadoOperativo.PENDIENTE_PREPARACION;
    }

    /** Personal de servicio: PENDIENTE_PREPARACION → EN_PREPARACION. */
    public void iniciarPreparacion() {
        verificarTransicionOperativa(EstadoOperativo.EN_PREPARACION);
        this.estadoOperativo = EstadoOperativo.EN_PREPARACION;
    }

    /** Personal de servicio: EN_PREPARACION → PREPARADO. */
    public void marcarPreparado() {
        verificarTransicionOperativa(EstadoOperativo.PREPARADO);
        this.estadoOperativo = EstadoOperativo.PREPARADO;
    }

    /** Vuelve al servicio tras una baja operativa: FUERA_DE_SERVICIO → PENDIENTE_PREPARACION. */
    public void reincorporarAlServicio() {
        if (this.estadoOperativo != EstadoOperativo.FUERA_DE_SERVICIO) {
            throw new ReglaDominioException("Solo se reincorpora un apartamento que está fuera de servicio");
        }
        verificarTransicionOperativa(EstadoOperativo.PENDIENTE_PREPARACION);
        this.estadoOperativo = EstadoOperativo.PENDIENTE_PREPARACION;
    }

    /** Decisión administrativa: desde cualquier estado no ocupado. */
    public void declararFueraDeServicio() {
        verificarTransicionOperativa(EstadoOperativo.FUERA_DE_SERVICIO);
        this.estadoOperativo = EstadoOperativo.FUERA_DE_SERVICIO;
    }

    /** 
     * 7.3: cambiar la capacidad NO afecta las reservas ya creadas. 
     */
    public void cambiarCapacidad(int nuevaCapacidad) {
        if (nuevaCapacidad < 1) {
            throw new ReglaDominioException("La capacidad debe ser al menos 1");
        }
        this.capacidad = nuevaCapacidad;
    }

    /**
     * Alta lógica. La regla "solo con tarifas completas" (7.4) la verifica ActivacionApartamentoService,
     * porque requiere conocer temporadas y tarifas, que son otros agregados.
     */
    public void activar() {
        if (activo) {
            throw new ReglaDominioException("El apartamento ya está activo");
        }
        this.activo = true;
    }

    /** Baja lógica (regla 11): el apartamento deja de venderse pero nunca se elimina físicamente. */
    public void darDeBaja() {
        if (!activo) {
            throw new ReglaDominioException("El apartamento ya está dado de baja");
        }
        this.activo = false;
    }

    private static void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDominioException("El apartamento debe tener un nombre");
        }
    }

    private static void validarDormitorios(int dormitorios) {
        if (dormitorios < 1) {
            throw new ReglaDominioException("El apartamento debe tener al menos un dormitorio");
        }
    }

    private static void validarCapacidad(int capacidad) {
        if (capacidad < 1) {
            throw new ReglaDominioException("La capacidad debe ser al menos de 1 persona");
        }
    }

    /** L-06 / 7.3: la descripción es obligatoria y tiene un tope de longitud. Se guarda sin espacios sobrantes. */
    private static String validarDescripcion(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            throw new ReglaDominioException("El apartamento debe tener una descripción");
        }
        String limpia = descripcion.trim();
        if (limpia.length() > MAX_DESCRIPCION) {
            throw new ReglaDominioException("La descripción no puede superar los " + MAX_DESCRIPCION + " caracteres");
        }
        return limpia;
    }

    /** L-06: la dotación puede estar vacía; sus ítems no pueden estar en blanco, repetirse ni ser demasiados. */
    private static List<String> validarDotacion(Collection<String> dotacion) {
        if (dotacion == null) {
            return List.of();
        }
        if (dotacion.size() > MAX_ITEMS_DOTACION) {
            throw new ReglaDominioException("La dotación no puede tener más de " + MAX_ITEMS_DOTACION + " elementos");
        }
        List<String> limpia = new ArrayList<>();
        Set<String> vistos = new HashSet<>();
        for (String item : dotacion) {
            if (item == null || item.isBlank()) {
                throw new ReglaDominioException("Un elemento de la dotación no puede estar en blanco");
            }
            String normalizado = item.trim();
            if (normalizado.length() > MAX_LONGITUD_ITEM_DOTACION) {
                throw new ReglaDominioException("Un elemento de la dotación no puede superar los "
                        + MAX_LONGITUD_ITEM_DOTACION + " caracteres");
            }
            if (!vistos.add(normalizado.toLowerCase())) {
                throw new ReglaDominioException("La dotación no puede repetir elementos: " + normalizado);
            }
            limpia.add(normalizado);
        }
        return Collections.unmodifiableList(limpia);
    }

    /** L-06: las características pueden estar vacías; no se admite un elemento nulo. */
    private static Set<Caracteristica> validarCaracteristicas(Collection<Caracteristica> caracteristicas) {
        EnumSet<Caracteristica> conjunto = EnumSet.noneOf(Caracteristica.class);
        if (caracteristicas == null) {
            return Collections.unmodifiableSet(conjunto);
        }
        for (Caracteristica caracteristica : caracteristicas) {
            if (caracteristica == null) {
                throw new ReglaDominioException("Una característica del apartamento no puede ser nula");
            }
            conjunto.add(caracteristica);
        }
        return Collections.unmodifiableSet(conjunto);
    }

    private void verificarTransicionOperativa(EstadoOperativo siguiente) {
        if (!this.estadoOperativo.puedeTransicionarA(siguiente)) {
            throw new ReglaDominioException(
                "No se puede pasar de " + this.estadoOperativo + " a " + siguiente);
        }
    }

    // Getters
    public IdentificacionApartamento getIdentificacion() { return identificacion; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    /** Copia inmutable: desde fuera no se puede alterar la dotación del agregado. */
    public List<String> getDotacion() { return dotacion; }
    /** Copia inmutable, ordenada según el catálogo. */
    public Set<Caracteristica> getCaracteristicas() { return caracteristicas; }
    /** Las imágenes de la galería (sin las retiradas), en el orden en que se agregaron. Copia inmutable. */
    public List<ImagenApartamento> getImagenes() { return imagenes.stream().filter(imagen -> !imagen.retirada()).toList(); }
    /** Todas las imágenes, también las retiradas (para persistir el registro completo). */
    public List<ImagenApartamento> getTodasLasImagenes() { return List.copyOf(imagenes); }
    /** La imagen principal, si el apartamento tiene alguna. */
    public Optional<ImagenApartamento> getImagenPrincipal() { return imagenes.stream().filter(imagen -> !imagen.retirada() && imagen.principal()).findFirst(); }
    public int getDormitorios() { return dormitorios; }
    public int getCapacidad() { return capacidad; }
    public EstadoOperativo getEstadoOperativo() { return estadoOperativo; }
    public boolean isActivo() { return activo; }

    // Igualdad basada EXCLUSIVAMENTE en la identidad
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Apartamento otro)) return false;
        return this.identificacion.equals(otro.identificacion);
    }

    @Override
    public int hashCode() {
        return identificacion.hashCode();
    }

}
