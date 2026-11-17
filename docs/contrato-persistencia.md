# Contrato de persistencia - lo que debe implementar `infrastructure/persistence/jpa`

Este documento es el acuerdo entre la capa de aplicación/API y la de persistencia. Las firmas están copiadas de
`domain/repository/` (la fuente de verdad es el código). **Si falta un método o una firma no te sirve, no la cambies
por tu lado: avisa**, porque los casos de uso y los mocks de las pruebas dependen de ellas.

Reglas del proyecto que aplican aquí: entidades JPA **separadas** del dominio, `@Embeddable` para objetos de valor,
mapper persistencia ↔ dominio, repos Spring Data + **adaptadores que implementan los puertos**, **eliminación lógica**
(nunca física) y datos de prueba que se cargan solos al iniciar. Persistir no debe cambiar el dominio.

## 1. Puertos que debes implementar (12 con JPA; los de notificación son en memoria)

```java
// ApartamentoRepository
Apartamento guardar(Apartamento apartamento);                       // crea o actualiza; devuelve la versión persistida
Optional<Apartamento> buscarPorIdentificacion(IdentificacionApartamento identificacion);
List<Apartamento> listarActivos();                                  // todos los activos, sin paginar (CU-02)
Pagina<Apartamento> listarActivos(SolicitudPagina pagina);
Pagina<Apartamento> listarTodos(SolicitudPagina pagina);            // activos o no, ordenados por identificación
Pagina<Apartamento> listarTodos(FiltroApartamentos filtro, SolicitudPagina pagina); // solo los que tienen TODAS las características del filtro (L-06) y, si se pide, el estadoOperativo

// ReservaRepository
List<Reserva> buscarActivasPorApartamento(IdentificacionApartamento apartamento); // PENDIENTE, CONFIRMADA y EN_CURSO
Reserva guardar(Reserva reserva);                                   // crea o actualiza (estancia, ocupantes, valor, historial)
Optional<Reserva> buscarPorCodigo(CodigoReserva codigo);
Pagina<Reserva> buscar(FiltroReservas filtro, SolicitudPagina pagina);
List<Reserva> buscarPendientesCreadasAntesDe(LocalDateTime limite);  // PENDIENTE con creadaEn ANTERIOR al límite (RN-21)
Optional<Reserva> buscarPorCanalEIdentificadorExterno(CanalOrigen canal, IdentificadorExterno id); // RN-19

// FolioRepository
Folio guardar(Folio folio);                                         // cargos, ajustes, pagos, cerrado y autorización
Optional<Folio> buscarPorReserva(CodigoReserva reserva);

// BloqueoRepository
List<Bloqueo> buscarVigentesPorApartamento(IdentificacionApartamento apartamento);
Bloqueo guardar(Bloqueo bloqueo);
Pagina<Bloqueo> listarPorApartamento(IdentificacionApartamento apartamento, SolicitudPagina pagina);

// TarifarioRepository
Tarifa tarifaDe(IdentificacionApartamento apartamento, LocalDate noche);
void guardarTarifa(IdentificacionApartamento apartamento, Tarifa tarifa);   // crea o REEMPLAZA la de esa temporada
List<Tarifa> listarTarifas(IdentificacionApartamento apartamento);

// TemporadaRepository
Temporada guardar(Temporada temporada);
List<Temporada> listarTodas();

// PoliticaCancelacionRepository
PoliticaCancelacion guardar(PoliticaCancelacion politica);
Optional<PoliticaCancelacion> vigenteEn(LocalDate fecha);           // la de mayor vigenteDesde que no supere la fecha
Optional<PoliticaCancelacion> ultimaVersion();                      // la de mayor número
Optional<PoliticaCancelacion> porVersion(VersionPolitica version);

// GeneradorCodigoReserva
CodigoReserva generar(LocalDate fecha);                             // formato RES-AAAA-NNNNN; único y seguro con hilos
```

**Canal externo (J9, 7.8).** Tres puertos nuevos, con adaptador JPA:

```java
// CanalVentaRepository  (tabla canal_venta; credencial_hash único)
CanalVenta guardar(CanalVenta canal);                                // crea o actualiza (desactivar/activar); nunca borra
Optional<CanalVenta> buscarPorCodigo(String codigo);                 // sin distinguir mayúsculas
Optional<CanalVenta> buscarPorCredencialHash(String hash);           // encuentra también al canal desactivado
Pagina<CanalVenta> listar(SolicitudPagina pagina);                   // de 10 en 10, por código
// BitacoraCanalRepository  (tabla evento_canal; solo se agrega)
void registrar(EventoCanal evento);                                  // en su propia transacción (REQUIRES_NEW): sobrevive al rollback del mensaje rechazado
Pagina<EventoCanal> listar(String canal, LocalDate desde, LocalDate hasta, SolicitudPagina pagina);   // del más reciente al más antiguo; filtros opcionales, fechas inclusivas
// NovedadRepository  (M1; tabla novedad; solo se agrega)
Novedad registrar(Novedad novedad);
Pagina<Novedad> listarPorApartamento(IdentificacionApartamento apartamento, SolicitudPagina pagina);   // de la más reciente a la más antigua (fecha y, si empatan, id)
// RechazoReservaExternaRepository  (ya existía; J9 le suma la revisión)
Optional<RechazoReservaExterna> buscarPorId(long id);
Pagina<RechazoReservaExterna> listar(Boolean revisado, SolicitudPagina pagina);   // null = todos
```

`rechazo_reserva_externa` gana `revisado`, `nota_revision`, `revisado_por` y `revisado_en` (los rechazos anteriores quedan sin revisar). El identificador externo de una reserva de canal se guarda como `CODIGO:idPropio` (el límite de 55 caracteres del identificador no cambia).

`ConfiguracionAlojamiento` **no lo implementas tú**: lo implementa `DomainServiceConfig` leyendo `application.properties`.

## 2. Semántica que los casos de uso suponen

| Puerto / método | Lo que debe cumplir |
|---|---|
| `ReservaRepository.buscar` | Filtros **opcionales** (campo nulo = no filtrar): `entradaDesde`/`entradaHasta` sobre la **fecha de entrada** y `salidaDesde`/`salidaHasta` sobre la **fecha de salida** (ambos extremos inclusive; con desde = hasta, las llegadas o salidas previstas de un día, 7.6), `estado`, `apartamento`, `canal`, `titular` (documento EXACTO) y `nombreTitular` (texto que el nombre del titular CONTIENE, sin distinguir mayúsculas; los `%` y `_` del texto se toman literalmente; une con el ocupante cuyo documento es el del titular, sin duplicar filas). Orden **de la reserva más reciente a la más antigua** (`creadaEn` descendente). `Pagina.numero` parte de 0; el tamaño es `SolicitudPagina.TAMANIO` (10); `totalElementos` es el total sin paginar. |
| `ReservaRepository.buscarPorCanalEIdentificadorExterno` | Devuelve la reserva ya creada para ese canal e identificador (RN-19). La base de datos lo respalda con una restricción única `(canal_origen, identificador_externo)`; los NULL de los canales sin identificador no chocan. |
| `RechazoReservaExternaRepository` | Solo **inserta** y **lista** (10 por página, del más reciente al más antiguo): el rechazo (RN-18) es una constancia inmutable. Debe guardarse **aunque la petición termine en 409**: el caso de uso no revierte la transacción por `ReglaDominioException`. |
| `ReservaRepository.buscarPendientesCreadasAntesDe` | Solo estado **PENDIENTE** y `creadaEn` **estrictamente anterior** al límite (una creada justo en el límite no entra). Lo consulta el proceso programado del vencimiento automático (RN-21) cada 5 minutos. No necesita orden ni paginación. |
| `ReservaRepository.buscarActivasPorApartamento` | Solo estados PENDIENTE, CONFIRMADA y EN_CURSO. De esto depende que **una cancelación libere las noches de inmediato** (RN-12). |
| `ReservaRepository.guardar` / `FolioRepository.guardar` / `ApartamentoRepository.guardar` | Se usan para **actualizar** (no solo crear): modificación, cancelación, llegada, salida, pagos, cierre, estado operativo. Si ya existe, actualizar; no duplicar. Devolver la entidad guardada. |
| `FolioRepository.buscarPorReserva` | Un folio por reserva. El orden de cargos, ajustes y pagos debe ser el de registro. |
| `BloqueoRepository.buscarVigentesPorApartamento` | Hoy el bloqueo no tiene fecha de baja: son "vigentes" todos los del apartamento. |
| `TarifarioRepository.tarifaDe` | Debe resolver la **temporada de la noche** con `CalendarioTemporadas` (la temporada cuyo periodo incluye la noche o, si ninguna, la base) y devolver la tarifa de esa temporada. Devolver `null` si no existe tarifa (el cotizador lo traduce en un 409). |
| `TemporadaRepository.guardar` | Identidad por **nombre**. |
| `PoliticaCancelacionRepository` | Las versiones **no se modifican ni se borran** (las reservas las tienen congeladas). |
| `AlojamientoRepository` (J6) | Hay UN solo alojamiento: `obtener` devuelve vacío hasta que se siembra y `guardar` inserta la primera vez y reemplaza después (la fila lleva siempre el identificador 1). Lo siembra `InicializadorAlojamiento` desde `application.properties` y es idempotente: no pisa lo que el administrador editó. |
| `ServicioAdicionalRepository` (J6) | Identidad por **código**. `guardar` crea o actualiza (la baja lógica es el campo `activo`, nunca se borra). `listarTodos` y `listarActivos` de 10 en 10 ordenados por código. El valor se guarda en pesos COP enteros. |
| `ContactoReservaRepository` (J7) | Un correo de contacto por reserva (clave: código de la reserva). `guardar` crea o reemplaza; `buscar` devuelve vacío si la reserva no tiene uno. No es un dato de la reserva: el agregado no lo conoce. |
| `CodigoRecuperacionRepository` (J7) | Un código vigente por correo (clave: correo): `guardar` crea o reemplaza (un código nuevo borra al anterior) y también persiste `usado` e `intentos_fallidos`. Guarda solo el **hash**. `buscarPorCorreo` lo reconstruye con `CodigoRecuperacion.reconstruir`. |
| `Notificador` y `BandejaNotificaciones` (J7) | **No usan JPA.** Los adaptadores viven en `infrastructure/notificaciones`: `NotificadorLocal` (por defecto) y `NotificadorCorreoSmtp` (con `sga.notificaciones.proveedor=smtp`) y `BandejaEnMemoria` (últimos 500 avisos, del más reciente al más antiguo, 10 por página; se vacía al reiniciar). |
| `Apartamento` (imágenes, J8) | Tabla `apartamento_imagen` (`@ElementCollection` con `@OrderColumn`): por apartamento, las imágenes **en el orden en que se agregaron**, con `id` (UUID), `url`, `referencia`, `principal` y `retirada`. Las **retiradas se conservan** (retiro lógico). El adaptador guarda `getTodasLasImagenes()` y restaura con `Apartamento.reconstruir(..., imagenes)`, que verifica la coherencia (a lo sumo 10 sin retirar y exactamente una principal si hay alguna). |
| `AlmacenImagenes` (J8) | **No usa JPA.** Los adaptadores viven en `infrastructure/imagenes`: `AlmacenImagenesLocal` (carpeta del disco, servida en `/imagenes/**`; por defecto) y `AlmacenImagenesCloudinary` (API de carga firmada, con tiempo de espera). `guardar(contenido, carpeta)` devuelve la URL pública y la referencia; no borra nada (el retiro es lógico). |
| `GeneradorCodigoReserva.generar` | Dos peticiones simultáneas no pueden obtener el mismo código. Usa un contador persistente por año: la fila se crea solo si no existe (`INSERT … WHERE NOT EXISTS`, nunca un `save`/`merge`, que la reiniciaría a 0) y se incrementa con un único `UPDATE ultimo = ultimo + 1`. |

## 3. Cómo reconstruir las entidades del dominio desde la base de datos

El dominio no tiene setters ni constructor vacío. Para restaurar entidades:

| Entidad | Cómo reconstruirla |
|---|---|
| `Reserva` | `Reserva.reconstruir(codigo, apartamento, estancia, titular, ocupantes, canalOrigen, valor, politica, creadaEn, estado, horaEstimadaLlegada, historial, identificadorExterno)`. El identificador es obligatorio si el canal es EXTERNO y debe ser nulo en los demás (RN-19). Restaura el **estado, la fecha de creación y el historial** tal como se guardaron. No valida RN-04 (no exige fecha futura). Una reserva en CONFIRMADA, EN_CURSO, FINALIZADA o NO_SHOW **debe** traer `horaEstimadaLlegada` (RN-09). |
| `Reserva` (descuento) | **Columna `porcentaje_descuento`** (entero, no nulo) en la tabla `reserva` (RP-02). Es el porcentaje con el que se congeló el valor; el descuento en pesos no se guarda porque se deriva del desglose (`reserva_cargo_noche`) y del porcentaje: `ValorCongelado(detalle, porcentaje)`. Una reserva sin descuento guarda 0. El mapper pasa `reserva.getValor().porcentajeDescuento()` al guardar y reconstruye `new ValorCongelado(desglose, porcentaje)` al leer. |
| `Apartamento` | `Apartamento.reconstruir(identificacion, nombre, descripcion, dormitorios, capacidad, dotacion, caracteristicas, estadoOperativo, activo)` (L-06). **No uses `new Apartamento(...)` para restaurar**: ese constructor deja el apartamento *inactivo* (7.4). La dotación se restaura **en el mismo orden** en que se guardó y las características como conjunto. |
| `Folio` | Reproduce sus movimientos: `new Folio(reserva, cargoAlojamiento)` (el primer cargo), luego `registrarCargo`, `registrarAjuste` y `registrarPago` en el orden guardado y, si estaba cerrado, `cerrar(autorizacion)`. |
| `Ocupante` | `new Ocupante(documento, nombre, fechaNacimiento, fechaActual)`; para restaurar pasa la fecha de hoy (`LocalDate.now(...)`) como `fechaActual`. |
| `Bloqueo`, `Temporada`, `PoliticaCancelacion` | Sus constructores públicos. |
| `CodigoRecuperacion` (J7) | `CodigoRecuperacion.reconstruir(correo, hash, expiraEn, usado, intentosFallidos)`: restaura el estado tal cual, sin validar la vigencia. |
| `Alojamiento`, `ServicioAdicional` (J6) | `new Alojamiento(...)` con `new Ubicacion(latitud, longitud)`; `ServicioAdicional.reconstruir(codigo, nombre, descripcion, generaCargo, valor, activo)` (el constructor deja el servicio activo). |
| Objetos de valor (`Dinero`, `Estancia`, `CodigoReserva`...) | Sus constructores; ya validan. `Dinero` se guarda como entero de pesos COP (sin decimales). |

Al **guardar**, el mapper lee el estado con los getters públicos (`getEstado()`, `getHistorial()`, `obtenerCargos()`,
`obtenerAjustes()`, `obtenerPagos()`, `estaCerrado()`, `getAutorizacionCierre()`, `isActivo()`, `getEstadoOperativo()`...).

## 4. Datos de prueba que se cargan al iniciar (regla 12)

Para que la colección `docs/http/sga.http` funcione de punta a punta, los datos iniciales deben incluir:

- La **temporada base** y al menos otras dos (sin solapes entre sí), registradas **antes** de las tarifas.
- **Todos los apartamentos** con tarifa en **todas** las temporadas, y **activos**. Incluidos `APT-101` y `APT-102`
  (con capacidad entre 2 y 10 personas), o ajustar las variables `@apt` y `@aptBloqueo` al inicio de `sga.http`.
- **Estancias sembradas de al menos 3 noches y cada reserva con un adulto** (L-20): RP-01 exige 3 noches en ALTA, y el
  sembrado usa fechas relativas a hoy, así que cualquier estancia más corta podría impedir el arranque según el día
  (`SembradorReservasPrueba.NOCHES`; lo comprueban `SembradoEnJunioTest` y `SembradoEnDiciembreTest` con un reloj fijo).
- Cada apartamento con **descripción, dotación y al menos una característica diferenciadora** (L-06), de modo que los
  filtros por característica de `sga.http` (1.8 a 1.14) devuelvan conjuntos distintos y verificables.
- Una **política de cancelación** (mínimo dos tramos, uno de antelación 0) con `vigenteDesde` anterior a hoy.
- **Reservas en todos los estados** (PENDIENTE, CONFIRMADA, EN_CURSO, FINALIZADA, CANCELADA, NO_SHOW) y **una por cada canal**
  (PORTAL, DIRECTO, EXTERNO). Cada reserva con su **folio**; las CANCELADA y NO_SHOW con el reverso aplicado
  (consistente con `Folio.liquidarCancelacion` / `liquidarNoShow`).
- Al menos **un bloqueo vigente** y **un apartamento FUERA_DE_SERVICIO**.
- Las reservas sembradas **no deben ocupar fechas de 2028** (las usa la colección HTTP).
- **Cuidado con las reservas PENDIENTE sembradas (RN-21):** el vencimiento automático cancela toda reserva PENDIENTE cuya
  `creadaEn` supere el plazo de confirmación (24 h hoy, `sga.alojamiento.plazo-confirmacion-horas`), y se ejecuta **al arrancar
  y luego cada 5 minutos**. Si siembras una PENDIENTE con una fecha de creación antigua, quedará CANCELADA a los pocos
  segundos y ya no habrá ninguna en ese estado. Siémbrala con `creadaEn` reciente (por ejemplo, `LocalDateTime.now()`).

Un apartamento nuevo nace inactivo: en la carga inicial créalo, registra las tarifas y llama a `activar()`.

## 5. Para verificar tu trabajo

1. `gradle test` debe quedar en verde. Hoy `SgaApplicationTests.contextLoads` falla justamente porque faltan tus adaptadores.
2. Arranca la aplicación y abre `http://localhost:8080/swagger-ui.html` (debe coincidir con `docs/api/openapi.yaml`).
3. Ejecuta `docs/http/sga.http` en orden y compara los códigos HTTP con los comentarios de cada escenario.

## 6. Archivos que NO debes modificar (son de la capa de aplicación/API)

`application/**`, `infrastructure/rest/**`, `infrastructure/config/DomainServiceConfig.java`, `domain/**` y `src/test/**` de esas capas.
Si encuentras un error o necesitas algo del dominio, avisa antes de tocarlo.
