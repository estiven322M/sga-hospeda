# SGA — Sistema de Gestión de Alojamiento (Hospeda)

Proyecto final de Programación Avanzada, Universidad del Quindío. Backend en Spring Boot con arquitectura hexagonal. Estamos en la **Entrega 2: codificación del backend**.

## Stack
- Java 25, Spring Boot 4.1.1, Gradle (usar siempre `./gradlew`; en Windows `gradlew.bat`)
- H2 en memoria, Spring Data JPA, Bean Validation, MapStruct, springdoc-openapi (Swagger UI)
- Pruebas: JUnit 5 + Mockito. `./gradlew test` debe quedar en verde antes de cada commit.

## Alcance de esta entrega
- **Seguridad con JWT (Guía 12) incorporada.** Antes el profe indicó entregar el backend sin JWT; con la Guía 12 se agregan Spring Security 7 + servidor de recursos OAuth2 (HS256), login, registro público de huéspedes y autorización por rol y por propietario del recurso. Todo endpoint exige token salvo `/api/auth/login`, `/api/auth/registro`, `/error`, Swagger y la consola H2.
- Alcance del corte 2: alojamiento y apartamentos, bloqueos, temporadas, tarifas y política de cancelación, disponibilidad, cotización y reservas. Operación diaria, folio y canal externo pueden avanzar pero no son exigidos.
- Los 11 casos de uso documentados en la Entrega 1: CU-01 Cotizar estancia, CU-02 Consultar disponibilidad, CU-03 Crear reserva, CU-04 Modificar reserva, CU-05 Cancelar reserva, CU-06 Registrar llegada, CU-07 Registrar salida, CU-08 Registrar pago en folio, CU-09 Registrar bloqueo, CU-10 Preparar apartamento, CU-11 Autorizar cierre de folio con saldo pendiente.

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
16. **Lenguaje ubicuo obligatorio** (apartamento, estancia, titular, ocupante facturable, folio, cargo, pago, saldo, bloqueo, canal...). No usar room, booking, user, request.

## Documentación exigida
- `docs/api/openapi.yaml` coherente con lo implementado (un endpoint en Swagger que no esté en el yaml, o al revés, cuenta como incoherencia). Esquema `bearerAuth` global, login y registro con `security: []`, y 401/403 en cada endpoint protegido.
- Colección de pruebas (Postman, Bruno o `.http`) en el repo: flujo completo de una reserva, un 409 por cada regla que exponga la API y los casos 3 a 14 de la sección 9 de la Guía 12 (PARTE 0 inicia sesión; PARTE 6 seguridad).
- Matriz de Trazabilidad ampliada con el endpoint que expone cada regla.
- Diagrama de clases actualizado si el dominio cambió.

## Referencia de clase
El repo del profe (https://github.com/JhanCarlosMC/PR-AV-20262) es solo **guía**: se lee para copiar el *patrón* (casos de uso, DTOs, controller, mapper, handler de excepciones, persistencia JPA, `openapi.yaml`). No se copia su dominio: nuestro dominio es el de nuestra Entrega 1 y tiene nombres distintos (por ejemplo `CotizadorEstancia`). Las guías están en `Guias/` de ese repo.

## Estado actual (ya existe en este repo)
- Dominio: 18 objetos de valor, entidades `Reserva`, `Apartamento`, `Folio`, `Bloqueo`, `Ocupante`, servicios `DisponibilidadApartamentoService` y `CotizadorEstancia`, excepción `ReglaDominioException`, y sus pruebas unitarias.
- Puertos presentes: `ReservaRepository`, `BloqueoRepository`, `TarifarioRepository`, `ConfiguracionAlojamiento`.
- **Faltan:** puertos `ApartamentoRepository` y `FolioRepository`; todo `application` y `infrastructure`; parámetros de la Ficha en `application.properties`; dependencias MapStruct y springdoc; `openapi.yaml`; colección de pruebas.

## Cómo trabajar
- Una capa a la vez, en este orden: dependencias/config → puertos y casos de uso → persistencia → API → documentación.
- Antes de crear algo, leer el código existente y respetar sus nombres.
- Cada regla del dominio lleva prueba unitaria, incluyendo el caso en que se viola.
- No hacer commit directo en `main`: rama `feature/<tema>`, commits pequeños y Pull Request. Los tres integrantes deben contribuir de forma verificable.
- No versionar secretos ni archivos `.env`.
- Cualquier cambio en un endpoint se refleja en `openapi.yaml` en el mismo commit.
- Explicar brevemente cada pieza creada: el equipo debe poder sustentarla.
