# SGA — Sistema de Gestión de Alojamiento (Hospeda)

Proyecto final de Programación Avanzada, Universidad del Quindío. Backend en Spring Boot con arquitectura hexagonal. El backend de la **Entrega 2 está completo y corregido** (observaciones del profesor atendidas). La **Entrega 3 es la final**: incluye el frontend y todo lo que el proyecto pide y aún no existe (alojamiento, servicios adicionales, imágenes, usuarios, notificaciones, canal externo completo, reportes, IA no bloqueante) más el despliegue en la nube. Ver la sección "Entrega 3 (final)".

**Equipo: dos integrantes, Jhon Mendez y Miguel Angel Villegas.** Los dos deben contribuir de forma verificable en cada entrega.

## Stack
- Java 25, Spring Boot 4.1.1, Gradle (usar siempre `./gradlew`; en Windows `gradlew.bat`)
- H2 en memoria, Spring Data JPA, Bean Validation, MapStruct, springdoc-openapi (Swagger UI)
- Pruebas: JUnit 5 + Mockito. `./gradlew test` debe quedar en verde antes de cada commit.

## Alcance de esta entrega (Entrega 2, backend, ya implementado)
- **Seguridad con JWT (Guía 12):** Spring Security 7 + servidor de recursos OAuth2 (HS256), login, registro público de huéspedes y autorización por rol y por propietario del recurso. Todo endpoint exige token salvo `/api/auth/login`, `/api/auth/registro`, `/error`, Swagger y la consola H2.
- **Los 11 casos de uso** de la Entrega 1 (CU-01 a CU-11) están implementados y expuestos por la API, además de la operación diaria (llegada, salida, no-show, estado operativo), el folio y el canal externo (reservas EXTERNO, rechazos por conflicto) y el vencimiento automático de reservas pendientes.
- **L-06:** descripción, dotación y características de cada apartamento (enum cerrado `Caracteristica`) y su filtro (deben tenerse TODAS) en la búsqueda de disponibilidad y en el listado de apartamentos. `GET /api/caracteristicas` publica el catálogo.
- **L-20, reglas propias** (aprobación del docente solicitada el 15-nov-2026): RP-01 estancia mínima por temporada (ALTA 3 noches, MEDIA 2), RP-02 descuento por estancia larga (10 % desde 7 noches, una sola vez y congelado en la reserva) y RP-03 adulto responsable (al menos un ocupante de 18 años o más al reservar y al modificar; NO se valida al cotizar). Parámetros en `application.properties`.
- **Soporte del frontend:** CORS (propiedad `sga.cors.origenes-permitidos`, por defecto `http://localhost:5173`), `GET /api/reservas/mias` (solo HUESPED, por el claim `documento`), filtros `salidaDesde`, `salidaHasta` y `nombreTitular` en `GET /api/reservas`, y filtro `estadoOperativo` en `GET /api/apartamentos`.
- Falta por decidir fuera del backend actual (pendientes aceptados): L-01/L-02 (no hay entidad Alojamiento), L-16 mascotas, L-17 servicios adicionales, L-19 reportes, L-21 caso de uso de IA y L-23 funcionalidad opcional; L-18 medios de pago es texto libre. Ver la matriz de trazabilidad.

## Seguridad (Guía 12)
- El usuario NO es del dominio (3.6): `UsuarioEntity` y `RolUsuario` viven en `infrastructure`; `LoginRequest`, `RegistroUsuarioRequest`, `TokenResponse` en `application/dto`; el puerto `SecurityService` en `application/security`. El puente con el negocio es el **documento**: claim `documento` del JWT = documento del titular.
- Clases en `infrastructure/security`: `JwtConfig` (encoder/decoder HS256, emisor validado), `JwtTokenProvider`, `SecurityConfig` (cadena de filtros, BCrypt), `UserConfig`, `CustomUserDetails`, `SecurityServiceImpl`, `ManejadorErroresSeguridad` (401/403 de los filtros con `ErrorResponse`) y los verificadores usados en `@PreAuthorize`: `@propietarioReserva` (titular), `@canalReserva` (rol y canal al crear) y `@cierreFolio` (CU-11 solo administrador).
- Matriz de roles: cada método de controller lleva `@PreAuthorize` con `hasAuthority`/`hasAnyAuthority` (nunca `hasRole`: los roles van sin prefijo `ROLE_`). Cualquier endpoint nuevo DEBE declarar su `@PreAuthorize` y sus respuestas 401/403 en `openapi.yaml`.
- La clave `sga.jwt.secret` NUNCA se versiona: va en `src/main/resources/application-secrets.properties` (ignorado; plantilla `.example`) o en la variable `SGA_JWT_SECRET`. Las pruebas usan una clave pública solo de pruebas en `src/test/resources/config/application.properties`.
- Usuarios iniciales (uno por rol y dos huéspedes) en `application.properties`; `DefaultUserInitializer` los carga con BCrypt.

## Arquitectura hexagonal (paquete base `co.edu.uniquindio.sga`)
```
domain/
  entity/        entidades y raíces de agregado
  valueobject/   record o enum, inmutables, validan en el constructor
  service/       servicios de dominio (solo reglas que ningún agregado puede evaluar solo)
  repository/    puertos (interfaces)
  exception/     ReglaDominioException
application/
  usecase/       @Service + @Transactional; solo orquestan
  dto/request/   records
  dto/response/  records
  exception/     recursos no encontrados
infrastructure/
  rest/          controllers, mapper (MapStruct), exception (@RestControllerAdvice + ErrorResponse)
  persistence/jpa/   entidades JPA, @Embeddable, mappers de persistencia, repos Spring Data, adaptadores
  config/        @Configuration (servicios de dominio como @Bean), carga de datos de prueba
```

## Reglas que NO se pueden romper (la rúbrica las evalúa)
1. **El dominio es puro:** sin anotaciones de Spring, JPA ni Lombok. Sin setters, sin constructor vacío, sin `cambiarEstado` genérico. Transiciones con métodos de negocio.
2. **Entidades** con constructor controlado e igualdad por identidad. **Objetos de valor** como `record`/`enum`, validados en el constructor.
3. **Agregados** referencian a otros solo por identificador.
4. **Toda regla de negocio vive en el dominio** (RN-XX y L-XX). Nunca solo en un DTO, controller, caso de uso o JPQL. Bean Validation valida la *forma* (400); el dominio valida el *negocio* (409).
5. **Casos de uso** reciben tipos del dominio y orquestan, sin reglas de negocio.
6. **DTOs** como `record`; no exponer tipos del dominio salvo enums cerrados.
7. **Controllers delgados:** `@Valid`, MapStruct, delegar en el caso de uso. Un controller por recurso, exactamente los endpoints del `openapi.yaml`. Sin DELETE físicos ni endpoint genérico de cambio de estado.
8. **HTTP:** 201 con `Location` al crear; 200 en consultas y operaciones de negocio. Errores 400, 401, 403, 404, 409 con un único `ErrorResponse`. Nunca devolver trazas técnicas.
9. **Servicios de dominio** se registran como `@Bean` en una clase `@Configuration` (el dominio no conoce Spring).
10. **Nada del negocio quemado en el código:** umbral de edad, tiempo de preparación, plazo de confirmación, hora límite de no-show, horas de entrada/salida, anticipo y demás parámetros de la Ficha van en `application.properties`.
11. **Persistencia:** entidades JPA separadas del dominio, `@Embeddable` para objetos de valor, mapper persistencia↔dominio. Persistir no cambia el dominio. Eliminación lógica, nunca física. Los adaptadores en memoria pueden existir pero la app no los usa al arrancar.
12. **Datos de prueba** se cargan solos al iniciar: todos los apartamentos con tarifas completas en todas las temporadas, reservas en todos los estados, al menos un bloqueo vigente, un apartamento fuera de servicio y una reserva por cada canal.
13. **Paginación de 10 por página** en todos los listados. Reservas ordenadas de la más reciente a la más antigua.
14. **Dinero:** `BigDecimal`/tipo exacto, COP sin decimales, nunca `float`/`double`. Redondeo al final de cada cargo.
15. **Fechas:** estancia `[entrada, salida)`; zona horaria de Colombia.
16. **Lenguaje ubicuo obligatorio** (apartamento, estancia, titular, ocupante facturable, folio, cargo, pago, saldo, bloqueo, canal, estancia mínima, descuento por estancia larga, adulto responsable...). No usar room, booking, user, request.

## Documentación exigida
- `docs/api/openapi.yaml` coherente con lo implementado (un endpoint en Swagger que no esté en el yaml, o al revés, cuenta como incoherencia). Esquema `bearerAuth` global, login y registro con `security: []`, y 401/403 en cada endpoint protegido.
- Colección de pruebas (Postman, Bruno o `.http`) en el repo: flujo completo de una reserva, un 409 por cada regla que exponga la API y los casos 3 a 14 de la sección 9 de la Guía 12 (PARTE 0 inicia sesión; PARTE 6 seguridad).
- Matriz de Trazabilidad ampliada con el endpoint que expone cada regla.
- Diagrama de clases actualizado si el dominio cambió.

## Referencia de clase
El repo del profe (https://github.com/JhanCarlosMC/PR-AV-20262) es solo **guía**: se lee para copiar el *patrón* (casos de uso, DTOs, controller, mapper, handler de excepciones, persistencia JPA, `openapi.yaml`). No se copia su dominio: nuestro dominio es el de nuestra Entrega 1 y tiene nombres distintos (por ejemplo `CotizadorEstancia`). Las guías están en `Guias/` de ese repo.

## Estado actual (ya existe en este repo)
- **Dominio** (`domain/`): agregados `Reserva`, `Apartamento`, `Folio`, entidades `Bloqueo`, `Ocupante`, `Temporada`, `PoliticaCancelacion`, `RechazoReservaExterna`; objetos de valor (entre otros `Dinero`, `Estancia`, `ValorCongelado`, `Caracteristica`, `EstanciaMinimaPorTemporada`, `PoliticaDescuentoEstanciaLarga`, `EdadAdultoResponsable`); servicios de dominio (`CotizadorEstancia`, `DisponibilidadApartamentoService`, `ConfirmacionReservaService`, `RetencionCancelacionService`, `RecepcionReservaExternaService`...); puertos en `domain/repository`.
- **Aplicación** (`application/`): un caso de uso por operación (`` + ``), DTOs `record` y excepciones de recurso no encontrado.
- **Infraestructura** (`infrastructure/`): controllers REST con MapStruct y ``, `` con un único `ErrorResponse`, persistencia JPA (H2, entidades separadas del dominio, ``/``, mappers, adaptadores), seguridad JWT y CORS, y la carga de datos de prueba.
- **Documentación:** `docs/api/openapi.yaml` (verificado en cada build por `OpenApiCoherenciaTest`), `docs/matriz-trazabilidad.md`, `docs/diagrama-clases-dominio.md`, `docs/contrato-persistencia.md`, `docs/seguridad.md` y la colección `docs/http/sga.http` con su evidencia en `docs/http/resultado-corrida.md`.
- **Pruebas:** unitarias del dominio y de los casos de uso, de persistencia, de controllers, de seguridad y de integración con `` + MockMvc (login real y base H2 propia por clase) que verifican el CUERPO de las respuestas: `ReglasPropiasIntegracionTest`, `SoporteFrontendIntegracionTest` y los recorridos `Recorrido*IntegracionTest` (reloj movido por la prueba con `RelojMutable`).
- Toda respuesta 409 cumple la aserción común: `status` 409, `mensaje` con el texto de la regla, `errores` nulo y sin trazas técnicas.

## Entrega 3 (final): todo lo pendiente, el frontend y el despliegue
Plan completo: `../documentos-apoyo-proyecto/Plan-Final-SGA-Hospeda.docx`. Enunciado: `../documentos-apoyo-proyecto/Proyecto final.md` (secciones 6, 7, 10 y Anexo A). Reglas y estructura del frontend: `frontend/CLAUDE.md`.
- El frontend vive en `frontend/` (React + Vite, `http://localhost:5173`; la API en `http://localhost:8080`). La referencia de las guías está en `../documentos-apoyo-proyecto/React`.
- El token va en `Authorization: Bearer` (no en cookie). Un 401 lleva al login; un 403 muestra "sin permiso"; un 409 muestra el `mensaje` de la regla; un 400 trae `errores` por campo.
- No se cambia el contrato de la API sin actualizar `docs/api/openapi.yaml` en el mismo commit.

### Pasos y responsables (un commit por paso; es el único de su rama)
| Paso | Quién | Tema | Mensaje del commit |
|---|---|---|---|
| J1–J4 | Jhon | Hechos: proyecto base, autenticación y roles, huésped (búsqueda, cotización, reserva, mis reservas, cancelación) y recepción | — |
| J5 | Jhon | Administración: apartamentos, bloqueos, temporadas, tarifas, política, rechazos externos | `feat(front): administración de apartamentos, bloqueos, temporadas, tarifas y política` |
| J6 | Jhon | Alojamiento (L-01, L-02, L-16), servicios adicionales y su cargo (L-17), medios de pago (L-18), calendarios y aviso de capacidad | `feat: datos del alojamiento, servicios adicionales y su cargo, medios de pago y calendarios` |
| M1 | Miguel | Preparación de apartamentos y novedades del personal de servicio (7.1, 7.6) | `feat: preparación de apartamentos y novedades del personal de servicio` |
| J7 | Jhon | Notificaciones al titular (integración de salida, 10.3.2-A) y gestión de usuarios, perfil y recuperación de contraseña (7.2) | `feat: notificaciones al titular y gestión de usuarios, perfil y recuperación de contraseña` |
| M2 | Miguel | Reportes de ocupación, ingresos y reservas por canal; exportación a Excel opcional (7.9, L-19, L-23) | `feat: reportes de ocupación, ingresos y reservas por canal con exportación a Excel` |
| J8 | Jhon | Imágenes de apartamentos en almacenamiento externo, galería y mapa (7.3, 7.5) | `feat: imágenes de apartamentos en almacenamiento externo, galería y mapa del alojamiento` |
| M3 | Miguel | Sugerencia de gravedad de novedades con IA no bloqueante (10.2, L-21) | `feat: sugerencia de gravedad de novedades con IA no bloqueante` |
| J9 | Jhon | Canal externo completo: credencial, cancelación, bitácora, conflictos y simulador (7.8, 10.3.1) | `feat: canal externo con credencial, cancelación, bitácora y simulador de canal` |
| J10 | Jhon | Despliegue del backend en Render y del frontend en Vercel (Guía 22) | `build: despliegue del backend en Render y del frontend en Vercel` |
| M4 | Miguel | Verificación final, README, matriz completa y guía de sustentación | `docs: verificación final, README, matriz completa y guía de sustentación` |

### Requisitos de TODA integración externa (I-01 a I-08, sección 10.3.3)
Aplican al canal externo, a las notificaciones, al almacenamiento de imágenes, al mapa y a la IA. Cada integración debe cumplir:
| # | Requisito | Cómo se cumple aquí |
|---|---|---|
| I-01 | Detrás de un **puerto del dominio**; el dominio no conoce al proveedor | Interfaz en `domain/repository` (o `domain/service`); el adaptador vive en `infrastructure` |
| I-02 | **Adaptador local** (simulado o en memoria) que permita ejecutar y probar **sin internet y sin credenciales** | El adaptador local es el que viene **activo por defecto**; el real se elige solo con configuración (p. ej. `sga.notificaciones.proveedor=local\|smtp`) |
| I-03 | **Tiempo de espera máximo** explícito | Una propiedad con su valor (p. ej. 5 s) aplicada al cliente real |
| I-04 | **Comportamiento ante fallo** definido: qué ve el usuario, qué se registra y si la operación continúa | Se registra el fallo en el log y se muestra un aviso no bloqueante; la operación de negocio continúa |
| I-05 | El fallo **no puede impedir** reservar, registrar, cobrar ni cerrar un folio | Se notifica o se llama a la integración después de confirmar la transacción y se captura toda excepción |
| I-06 | Credenciales por **configuración externa**, sin versionar | `src/main/resources/application-secrets.properties` (ignorado; plantilla `.example`) o variables de entorno |
| I-07 | Alternativa con nivel **gratuito, de prueba o local**; nada que pida tarjeta | Gmail con contraseña de aplicación, Cloudinary gratuito, Gemini de AI Studio, OpenStreetMap |
| I-08 | Las pruebas **no invocan servicios reales** | Las pruebas usan el adaptador local o un adaptador simulado |

## Cómo trabajar
- Una capa a la vez, en este orden: dependencias/config → puertos y casos de uso → persistencia → API → documentación.
- Antes de crear algo, leer el código existente y respetar sus nombres.
- Cada regla del dominio lleva prueba unitaria, incluyendo el caso en que se viola.
- **Una rama y un commit por paso.** Cada paso empieza desde `main` actualizado (`git checkout main && git pull && git checkout -b feature/<tema>`) y termina con UN solo commit, push, Pull Request a `main` y fusión (`gh pr merge --merge --delete-branch`). Nunca commit directo en `main`.
- **Antes del commit:** `git pull --rebase origin main` para traer lo que el otro ya fusionó. Los archivos que ambos tocan (`openapi.yaml`, `docs/matriz-trazabilidad.md`, `docs/http/sga.http`, `frontend/src/App.tsx`, `frontend/src/rutas/rutas.ts`, `Encabezado.tsx`) se resuelven **conservando los cambios de los dos lados**; si hay conflicto de lógica del otro, avisarle antes de fusionar.
- Los dos integrantes (Jhon Mendez y Miguel Angel Villegas) deben contribuir de forma verificable, con commits repartidos a lo largo de la entrega y desde su propia cuenta de Git. Si el otro está disponible, que apruebe el PR antes de fusionar; si no, se fusiona igual y lo revisa después.
- No versionar secretos ni archivos `.env`.
- Cualquier cambio en un endpoint se refleja en `openapi.yaml` en el mismo commit.
- Explicar brevemente cada pieza creada: el equipo debe poder sustentarla.
