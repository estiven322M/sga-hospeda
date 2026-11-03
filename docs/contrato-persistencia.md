# Contrato de persistencia - lo que debe implementar `infrastructure/persistence/jpa`

Este documento es el acuerdo entre la capa de aplicación/API y la de persistencia. Las firmas están copiadas de
`domain/repository/` (la fuente de verdad es el código). **Si falta un método o una firma no te sirve, no la cambies
por tu lado: avisa**, porque los casos de uso y los mocks de las pruebas dependen de ellas.

Reglas del proyecto que aplican aquí: entidades JPA **separadas** del dominio, `@Embeddable` para objetos de valor,
mapper persistencia ↔ dominio, repos Spring Data + **adaptadores que implementan los puertos**, **eliminación lógica**
(nunca física) y datos de prueba que se cargan solos al iniciar. Persistir no debe cambiar el dominio.

## 1. Puertos que debes implementar (8)

```java
// ApartamentoRepository
Apartamento guardar(Apartamento apartamento);                       // crea o actualiza; devuelve la versión persistida
Optional<Apartamento> buscarPorIdentificacion(IdentificacionApartamento identificacion);
List<Apartamento> listarActivos();                                  // todos los activos, sin paginar (CU-02)
Pagina<Apartamento> listarActivos(SolicitudPagina pagina);
Pagina<Apartamento> listarTodos(SolicitudPagina pagina);            // activos o no, ordenados por identificación

// ReservaRepository
List<Reserva> buscarActivasPorApartamento(IdentificacionApartamento apartamento); // PENDIENTE, CONFIRMADA y EN_CURSO
Reserva guardar(Reserva reserva);                                   // crea o actualiza (estancia, ocupantes, valor, historial)
Optional<Reserva> buscarPorCodigo(CodigoReserva codigo);
Pagina<Reserva> buscar(FiltroReservas filtro, SolicitudPagina pagina);

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

`ConfiguracionAlojamiento` **no lo implementas tú**: lo implementa `DomainServiceConfig` leyendo `application.properties`.

## 2. Semántica que los casos de uso suponen

| Puerto / método | Lo que debe cumplir |
|---|---|
| `ReservaRepository.buscar` | Filtros **opcionales** (campo nulo = no filtrar): `entradaDesde`/`entradaHasta` sobre la **fecha de entrada** (ambos extremos inclusive), `estado`, `apartamento`, `canal` y `titular` (documento). Orden **de la reserva más reciente a la más antigua** (`creadaEn` descendente). `Pagina.numero` parte de 0; el tamaño es `SolicitudPagina.TAMANIO` (10); `totalElementos` es el total sin paginar. |
| `ReservaRepository.buscarActivasPorApartamento` | Solo estados PENDIENTE, CONFIRMADA y EN_CURSO. De esto depende que **una cancelación libere las noches de inmediato** (RN-12). |
| `ReservaRepository.guardar` / `FolioRepository.guardar` / `ApartamentoRepository.guardar` | Se usan para **actualizar** (no solo crear): modificación, cancelación, llegada, salida, pagos, cierre, estado operativo. Si ya existe, actualizar; no duplicar. Devolver la entidad guardada. |
| `FolioRepository.buscarPorReserva` | Un folio por reserva. El orden de cargos, ajustes y pagos debe ser el de registro. |
| `BloqueoRepository.buscarVigentesPorApartamento` | Hoy el bloqueo no tiene fecha de baja: son "vigentes" todos los del apartamento. |
| `TarifarioRepository.tarifaDe` | Debe resolver la **temporada de la noche** con `CalendarioTemporadas` (la temporada cuyo periodo incluye la noche o, si ninguna, la base) y devolver la tarifa de esa temporada. Devolver `null` si no existe tarifa (el cotizador lo traduce en un 409). |
| `TemporadaRepository.guardar` | Identidad por **nombre**. |
| `PoliticaCancelacionRepository` | Las versiones **no se modifican ni se borran** (las reservas las tienen congeladas). |
| `GeneradorCodigoReserva.generar` | Dos peticiones simultáneas no pueden obtener el mismo código. Usa una secuencia/contador persistente. |

## 3. Cómo reconstruir las entidades del dominio desde la base de datos

El dominio no tiene setters ni constructor vacío. Para restaurar entidades:

| Entidad | Cómo reconstruirla |
|---|---|
| `Reserva` | `Reserva.reconstruir(codigo, apartamento, estancia, titular, ocupantes, canalOrigen, valor, politica, creadaEn, estado, horaEstimadaLlegada, historial)`. Restaura el **estado, la fecha de creación y el historial** tal como se guardaron. No valida RN-04 (no exige fecha futura). Una reserva en CONFIRMADA, EN_CURSO, FINALIZADA o NO_SHOW **debe** traer `horaEstimadaLlegada` (RN-09). |
| `Apartamento` | `Apartamento.reconstruir(identificacion, nombre, dormitorios, capacidad, estadoOperativo, activo)`. **No uses `new Apartamento(...)` para restaurar**: ese constructor deja el apartamento *inactivo* (7.4). |
| `Folio` | Reproduce sus movimientos: `new Folio(reserva, cargoAlojamiento)` (el primer cargo), luego `registrarCargo`, `registrarAjuste` y `registrarPago` en el orden guardado y, si estaba cerrado, `cerrar(autorizacion)`. |
| `Ocupante` | `new Ocupante(documento, nombre, fechaNacimiento, fechaActual)`; para restaurar pasa la fecha de hoy (`LocalDate.now(...)`) como `fechaActual`. |
| `Bloqueo`, `Temporada`, `PoliticaCancelacion` | Sus constructores públicos. |
| Objetos de valor (`Dinero`, `Estancia`, `CodigoReserva`...) | Sus constructores; ya validan. `Dinero` se guarda como entero de pesos COP (sin decimales). |

Al **guardar**, el mapper lee el estado con los getters públicos (`getEstado()`, `getHistorial()`, `obtenerCargos()`,
`obtenerAjustes()`, `obtenerPagos()`, `estaCerrado()`, `getAutorizacionCierre()`, `isActivo()`, `getEstadoOperativo()`...).

## 4. Datos de prueba que se cargan al iniciar (regla 12)

Para que la colección `docs/http/sga.http` funcione de punta a punta, los datos iniciales deben incluir:

- La **temporada base** y al menos otras dos (sin solapes entre sí), registradas **antes** de las tarifas.
- **Todos los apartamentos** con tarifa en **todas** las temporadas, y **activos**. Incluidos `APT-101` y `APT-102`
  (con capacidad entre 2 y 10 personas), o ajustar las variables `@apt` y `@aptBloqueo` al inicio de `sga.http`.
- Una **política de cancelación** (mínimo dos tramos, uno de antelación 0) con `vigenteDesde` anterior a hoy.
- **Reservas en todos los estados** (PENDIENTE, CONFIRMADA, EN_CURSO, FINALIZADA, CANCELADA, NO_SHOW) y **una por cada canal**
  (PORTAL, DIRECTO, EXTERNO). Cada reserva con su **folio**; las CANCELADA y NO_SHOW con el reverso aplicado
  (consistente con `Folio.liquidarCancelacion` / `liquidarNoShow`).
- Al menos **un bloqueo vigente** y **un apartamento FUERA_DE_SERVICIO**.
- Las reservas sembradas **no deben ocupar fechas de 2028** (las usa la colección HTTP).

Un apartamento nuevo nace inactivo: en la carga inicial créalo, registra las tarifas y llama a `activar()`.

## 5. Para verificar tu trabajo

1. `gradle test` debe quedar en verde. Hoy `SgaApplicationTests.contextLoads` falla justamente porque faltan tus adaptadores.
2. Arranca la aplicación y abre `http://localhost:8080/swagger-ui.html` (debe coincidir con `docs/api/openapi.yaml`).
3. Ejecuta `docs/http/sga.http` en orden y compara los códigos HTTP con los comentarios de cada escenario.

## 6. Archivos que NO debes modificar (son de la capa de aplicación/API)

`application/**`, `infrastructure/rest/**`, `infrastructure/config/DomainServiceConfig.java`, `domain/**` y `src/test/**` de esas capas.
Si encuentras un error o necesitas algo del dominio, avisa antes de tocarlo.
