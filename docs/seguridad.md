# Seguridad del SGA — Hospeda (Guía 12)

Autenticación con **JWT (HS256)** emitido y validado por la propia aplicación con Spring Security 7 y su servidor de
recursos OAuth2. Autorización en dos niveles: **por URL** (qué es público y qué exige token, en `SecurityConfig`) y
**por método** (`@PreAuthorize` en cada endpoint: rol y, cuando aplica, propietario del recurso).

## 1. Piezas y dónde viven

| Pieza | Paquete | Por qué ahí |
|---|---|---|
| `UsuarioEntity`, `RolUsuario`, `UsuarioJpaDataRepository` | `infrastructure` | El usuario es un concepto de **acceso**, no del negocio (3.6). El dominio no lo conoce. |
| `LoginRequest`, `RegistroUsuarioRequest`, `TokenResponse`, `UsuarioRegistradoResponse` | `application/dto` | Son DTOs, como los demás. |
| `SecurityService` (interfaz) | `application/security` | El controller depende del puerto, no de Spring Security. |
| `UsuarioYaRegistradoException` | `application/exception` | Correo o documento repetidos en el registro → 409. |
| `JwtConfig`, `JwtTokenProvider`, `SecurityConfig`, `UserConfig`, `CustomUserDetails`, `SecurityServiceImpl` | `infrastructure/security` | Dependen de Spring Security. |
| `PropietarioReservaVerificador` (`@propietarioReserva`) | `infrastructure/security` | Un huésped solo accede a lo suyo (7.2). |
| `CanalReservaVerificador` (`@canalReserva`) | `infrastructure/security` | Rol y canal al crear una reserva (actividad 7.5.3). |
| `CierreFolioVerificador` (`@cierreFolio`) | `infrastructure/security` | Solo el administrador autoriza el cierre con saldo (CU-11). |
| `ManejadorErroresSeguridad` | `infrastructure/security` | Los 401/403 de la cadena de filtros también responden con `ErrorResponse` (desafío 7.5.6). |
| `DefaultUserProperties`, `DefaultUserInitializer` | `infrastructure/config/setup` | Un usuario por rol al iniciar (12.3), contraseñas con BCrypt. |
| `OpenApiConfig` | `infrastructure/config` | Botón **Authorize** en Swagger y 401/403 documentados en cada endpoint protegido. |

El puente entre el usuario y el negocio es el **documento de identidad**: el JWT lleva el claim `documento` y, si
coincide con el documento del titular de la reserva, el usuario es su propietario. `Reserva` no sabe que existen
usuarios.

## 2. El token

```json
{ "iss": "sga", "sub": "huesped1@sga.local", "iat": 1761750000, "exp": 1761751800,
  "nombre": "María Pérez", "roles": ["HUESPED"], "documento": "1094000111" }
```

- Algoritmo fijo **HS256** (`macAlgorithm`): un token con otro `alg` (incluido `none`) se rechaza.
- Se validan la **firma**, el **emisor** (`sga.jwt.emisor`) y la **expiración** (`sga.jwt.expiracion-minutos=30`).
- Está **firmado, no cifrado**: nunca lleva contraseña ni fecha de nacimiento.
- El claim `nombre` se agregó en la Entrega 3: el frontend lo muestra en el encabezado y precarga con él al titular de la reserva del huésped (`CrearReservaRequest.titular` exige documento, nombre y fecha de nacimiento; la fecha de nacimiento NO viaja en el token y el huésped la escribe en el formulario). El claim `documento` sigue siendo el único que decide quién es el propietario de una reserva: el nombre solo se muestra.
- La clave `sga.jwt.secret` (mínimo 32 caracteres) **no se versiona**: `application-secrets.properties` (ignorado por
  Git, con plantilla `.example`) o la variable de entorno `SGA_JWT_SECRET`, que tiene prioridad. Sin clave la aplicación
  no arranca.

## 3. Matriz de acceso (proyecto 7.1)

✔ permitido · ✘ 403 · **T** solo si es el titular de la reserva (si no lo es, o la reserva no existe, 403).

| Endpoint | HUESPED | RECEPCIONISTA | ADMINISTRADOR | PERSONAL_SERVICIO |
|---|:-:|:-:|:-:|:-:|
| `POST /api/auth/login`, `POST /api/auth/registro` | público | público | público | público |
| `GET /api/apartamentos` (con filtros `caracteristicas` y `estadoOperativo`), `GET /api/apartamentos/{id}`, `GET /api/caracteristicas` | ✔ | ✔ | ✔ | ✔ |
| `POST /api/apartamentos`, `PUT /api/apartamentos/{id}`, `PUT .../activar`, `PUT .../baja` | ✘ | ✘ | ✔ | ✘ |
| `PUT .../estado-operativo/iniciar-preparacion`, `.../marcar-preparado` | ✘ | ✘ | ✔ | ✔ |
| `PUT .../estado-operativo/declarar-fuera-de-servicio`, `.../reincorporar` | ✘ | ✘ | ✔ | ✘ |
| `POST /api/apartamentos/{id}/bloqueos` | ✘ | ✘ | ✔ | ✘ |
| `GET /api/apartamentos/{id}/bloqueos` | ✘ | ✔ | ✔ | ✘ |
| `POST /api/temporadas`, `PUT .../tarifas/{temporada}`, `POST /api/politicas-cancelacion` | ✘ | ✘ | ✔ | ✘ |
| `GET /api/temporadas`, `GET .../tarifas` | ✘ | ✔ | ✔ | ✘ |
| `GET /api/politicas-cancelacion/vigente`, `GET .../{version}` | ✔ | ✔ | ✔ | ✘ |
| `POST /api/cotizaciones`, `POST /api/disponibilidad` | ✔ | ✔ | ✔ | ✘ |
| `POST /api/reservas` | ✔ PORTAL, a su nombre | ✔ DIRECTO | ✔ DIRECTO o EXTERNO | ✘ |
| `GET /api/reservas` (listado con filtros: fechas de entrada y de salida, estado, apartamento, canal, `titular` por documento y `nombreTitular` por texto) | ✘ | ✔ | ✔ | ✘ |
| `GET /api/reservas/mias` (las del propio huésped, por el claim `documento`) | ✔ | ✘ | ✘ | ✘ |
| `GET /api/reservas/{codigo}` | **T** | ✔ | ✔ | ✘ |
| `PUT /api/reservas/{codigo}/cancelar` | **T** | ✔ | ✔ | ✘ |
| `PUT .../modificar`, `.../confirmar`, `.../registrar-llegada`, `.../registrar-salida`, `.../declarar-no-show` | ✘ | ✔ | ✔ | ✘ |
| `GET /api/reservas/{codigo}/folio` | **T** | ✔ | ✔ | ✘ |
| `POST /api/reservas/{codigo}/folio/pagos` | ✘ | ✔ | ✔ | ✘ |
| `PUT /api/reservas/{codigo}/folio/cerrar` | ✘ | ✔ sin autorización | ✔ | ✘ |
| `GET /api/reservas-externas/rechazos` | ✘ | ✘ | ✔ | ✘ |

## 3.1 CORS (para el frontend, Guía 15 4.2)

El navegador solo deja que una página servida desde otro origen (el servidor de Vite, `http://localhost:5173`) lea las respuestas de la API si la
API lo autoriza. `SecurityConfig` registra una `CorsConfigurationSource` para `/api/**` y la activa con `.cors(...)` ANTES de la autenticación, porque el
preflight `OPTIONS` del navegador no lleva token.

| Aspecto | Decisión | Por qué |
|---|---|---|
| Orígenes | solo los de la propiedad `sga.cors.origenes-permitidos` (separados por comas; por defecto `http://localhost:5173`) | nunca `*`: el token viaja en un encabezado y no se debe autorizar a cualquier sitio |
| Métodos | GET, POST, PUT y OPTIONS | no hay DELETE: la eliminación es lógica |
| Encabezados que envía el cliente | `Authorization` y `Content-Type` | el JWT y el cuerpo JSON |
| Encabezado que lee el cliente | `Location` | la URL de lo recién creado (201) |
| Credenciales | no | el token no va en una cookie |
| Origen no autorizado | 403 en el preflight, sin `Access-Control-Allow-Origin` | el navegador bloquea la lectura |

Un detalle que costó encontrar: la configuración se inyecta por **parámetro del método** `securityFilterChain` con `@Qualifier`, no por constructor. Si
`SecurityConfig` la recibe por constructor, Spring trata el bean definido en su propia clase como autorreferencia y toma en su lugar el de Spring MVC, que no
tiene configuración CORS: el preflight responde 200 pero sin cabeceras. `CorsIntegracionTest` lo comprueba con la cadena de seguridad real.

## 4. Decisión: rol y canal (actividad 7.5, punto 3)

*Un huésped crea reservas solo por el canal PORTAL y solo a su nombre; el recepcionista, por el canal DIRECTO.*

**Es una regla de acceso, no de negocio**, y por eso vive en `infrastructure/security` (`CanalReservaVerificador`),
se evalúa en `@PreAuthorize` y su incumplimiento es **403**, no 409:

1. Depende de **quién** está autenticado y con qué rol, y el dominio no conoce al usuario (3.6). Llevarla al dominio
   obligaría a `Reserva` o a un servicio de dominio a recibir un "usuario" o un "rol", rompiendo la pureza del dominio.
2. Para el negocio una reserva por PORTAL a nombre de cualquier titular **es válida**; lo que se restringe es quién la
   puede registrar. Las reglas de negocio del canal (RN-18 rechazo registrado, RN-19 identificador externo único)
   siguen en el dominio y responden 409.
3. El verificador no decide reglas de negocio: solo responde "este usuario, con este rol, ¿puede usar este canal a
   nombre de este titular?".

El administrador puede además registrar reservas del canal **EXTERNO** mientras no exista la credencial por canal de la
sección 10.3.1; cuando exista, ese permiso pasará a la credencial del canal. Los canales permitidos por rol están en un
solo mapa (`CANALES_POR_ROL`), para ajustarlos sin tocar los controllers.

## 5. Códigos de respuesta

| Código | Significado |
|---|---|
| 400 | El mensaje está mal escrito (Bean Validation, JSON, formato) |
| 401 | No hay token, el token es inválido o expiró, o las credenciales del login son incorrectas (mensaje genérico) |
| 403 | Autenticado, pero el rol no lo permite, el canal no corresponde o no es el titular |
| 404 | El identificador no existe |
| 409 | El negocio no lo permite (o el correo/documento del registro ya existen) |

## 6. Usuarios iniciales (datos de prueba de H2, no credenciales reales)

| Correo | Contraseña | Rol | Documento |
|---|---|---|---|
| admin@sga.local | Admin2026 | ADMINISTRADOR | 1000000001 |
| recepcion@sga.local | Recepcion2026 | RECEPCIONISTA | 1000000002 |
| servicio@sga.local | Servicio2026 | PERSONAL_SERVICIO | 1000000003 |
| huesped1@sga.local | Huesped2026 | HUESPED | 1094000111 (titular de la reserva PENDIENTE sembrada por el PORTAL) |
| huesped2@sga.local | Huesped2026 | HUESPED | 1094000222 (titular de la reserva FINALIZADA sembrada por el PORTAL) |

## 7. Cómo verificar

- Pruebas automáticas: `JwtConfigTest` (firma, algoritmo, emisor, expiración), `PropietarioReservaVerificadorTest`,
  `CanalReservaVerificadorTest`, `CierreFolioVerificadorTest`, `SecurityServiceImplTest`, `SecurityControllerTest`,
  `GlobalExceptionHandlerSeguridadTest` y `SeguridadApiIntegracionTest` (aplicación completa, casos 3 a 13 de la
  sección 9 de la guía).
- Colección `docs/http/sga.http`: PARTE 0 (login de cada rol y registro) y PARTE 6 (casos 3 a 14 y rol/canal).
