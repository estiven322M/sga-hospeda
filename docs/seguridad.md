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
| `POST /api/auth/login`, `POST /api/auth/registro`, `POST /api/auth/recuperacion/solicitar`, `POST /api/auth/recuperacion/confirmar` | público | público | público | público |
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
| `GET /api/alojamiento` (datos públicos: Inicio, detalle y mapa) | público | público | público | público |
| `PUT /api/alojamiento`, `GET /api/alojamiento/parametros` (solo lectura) | ✘ | ✘ | ✔ | ✘ |
| `GET /api/servicios-adicionales` | ✘ | ✔ | ✔ | ✘ |
| `POST /api/servicios-adicionales`, `PUT .../{codigo}`, `PUT .../baja`, `PUT .../activar` | ✘ | ✘ | ✔ | ✘ |
| `POST /api/reservas/{codigo}/folio/cargos` (cargo de servicio adicional) | ✘ | ✔ | ✔ | ✘ |
| `GET /api/usuarios` (filtro `rol`), `POST /api/usuarios` (usuario interno), `PUT /api/usuarios/{id}/rol`, `PUT /api/usuarios/{id}/desactivar` | ✘ | ✘ | ✔ | ✘ |
| `GET /api/usuarios/yo`, `PUT /api/usuarios/yo`, `PUT /api/usuarios/yo/contrasena` (siempre el propio, por el sujeto del token) | ✔ | ✔ | ✔ | ✔ |
| `GET /api/notificaciones` (bandeja de avisos) | ✘ | ✘ | ✔ | ✘ |
| `POST /api/apartamentos/{id}/imagenes` (multipart), `PUT .../imagenes/{idImagen}/principal`, `PUT .../imagenes/{idImagen}/retirar` | ✘ | ✘ | ✔ | ✘ |
| `PUT /api/usuarios/yo/foto` (multipart; siempre la del dueño del token) | ✔ | ✔ | ✔ | ✔ |
| `GET /imagenes/**` (archivos del almacén local: los pide `<img>`, que no envía el token) | público | público | público | público |
| `GET /api/apartamentos/{id}/calendario` | ✔ solo libre o no disponible | ✔ con el código y el estado de la reserva | ✔ con el código y el estado de la reserva | ✘ |

## 3.0 Usuarios, perfil y recuperación de contraseña (7.2, J7)

El usuario sigue fuera del dominio (3.6): `UsuarioEntity` y `RolUsuario` viven en infraestructura y la gestión está detrás de los puertos
`GestionUsuariosService` y `RecuperacionContrasenaService` (`application/security`), implementados en `infrastructure/security`.

- **Una sola regla viene del negocio:** no se desactiva a un usuario que sea titular de reservas PENDIENTE, CONFIRMADA o EN_CURSO. La decide el
  servicio de dominio `BajaUsuarioService`, que consulta las reservas **por documento** (409). La baja es lógica: el usuario no inicia sesión, pero
  su titular y sus reservas históricas se conservan.
- **Reglas de acceso (409, no 403):** un administrador no puede cambiar su propio rol ni desactivar su propia cuenta; el cambio de contraseña exige
  la actual correcta y una nueva distinta; correo y documento son únicos (sin distinguir mayúsculas en el correo).
- **`/yo` nunca recibe un identificador:** el perfil es el del sujeto del JWT (el correo). Solo se editan nombre y teléfono; el correo, el documento
  y el rol se ignoran aunque se envíen. La contraseña nueva sigue las mismas reglas del registro (`ReglasContrasena`).
- **Recuperación:** el código es de 6 dígitos, vale 15 minutos (`sga.recuperacion.vigencia-minutos`), sirve **una sola vez** y se bloquea tras 5 intentos
  fallidos (`sga.recuperacion.intentos-maximos`). Se guarda solo su **hash BCrypt** (`CodigoRecuperacion`, entidad de dominio). Un código nuevo reemplaza
  al anterior. `solicitar` responde SIEMPRE lo mismo (exista o no el correo, o esté desactivado el usuario) y `confirmar` responde el mismo 409
  ("El código no es válido o ya venció") ante un correo desconocido, un código incorrecto, vencido, usado o bloqueado: no se revela el motivo.
- **El código llega por el puerto Notificador.** Con el proveedor local se lee en `GET /api/notificaciones` (solo administrador); con `smtp` viaja por correo
  y en la bandeja queda "(contenido sensible omitido)". La respuesta de la solicitud nunca lo trae.

## 3.0b Imágenes (7.3) y foto de perfil (7.2, J8)

- **Subida:** `multipart/form-data`, solo ADMINISTRADOR para las imágenes de apartamentos; la foto de perfil es de cualquier rol y siempre la del sujeto del token.
- **El formato se reconoce por el contenido** (firma de los primeros bytes: JPG, PNG o WEBP), nunca por la extensión ni por el tipo que declare el cliente: un texto
  renombrado a `.jpg` se rechaza (409). El tamaño máximo (5 MB y 2 MB) es un parámetro de `application.properties`; Spring tiene un techo propio más alto
  (`spring.servlet.multipart.max-file-size`) para que la regla del negocio responda con su mensaje claro.
- **Archivos públicos:** `GET /imagenes/**` es público porque el navegador pide las imágenes con `<img>`, que no envía el token. Solo sirve la carpeta del almacén local
  (`sga.imagenes.local.carpeta`); un nombre con `..` no sale de ella. Los nombres de archivo que genera el sistema son UUID: no se pueden adivinar ni los pone el cliente.
- **Credenciales de Cloudinary:** `sga.imagenes.cloudinary.cloud-name`, `api-key` y `api-secret` nunca se versionan (`application-secrets.properties` o variables de
  entorno) y nunca se escriben en el log; con el proveedor `cloudinary` y sin ellas la aplicación no arranca.

## 3.0c Canal externo: una segunda cadena de seguridad (7.8, J9)

- **Dos cadenas de filtros.** `/api/canal/**` tiene su propia cadena (`@Order(1)`, `SecurityConfig.canalSecurityFilterChain`): sin sesión, sin CSRF, **sin JWT y sin CORS** (lo llama un servidor, no un navegador) y con un solo filtro, `CredencialCanalFilter`. El resto de la API sigue en la cadena principal (`@Order(2`)). Las dos credenciales **no son intercambiables**: un JWT en `/api/canal/**` da 401 y la credencial del canal en el resto de la API también.
- **La credencial** se envía en `X-Canal-Credencial`. Se genera con `SecureRandom` (256 bits, `sgc_` + base64url), se muestra **una sola vez** al crear el canal y solo se guarda su hash SHA-256 (`CredencialCanal`, en el dominio). El hash es determinista, lo que permite buscar al canal por él; al ser una clave aleatoria de 256 bits no necesita sal ni BCrypt (BCrypt se usa en las contraseñas, que las elige una persona). `CredencialCanal.toString()` no imprime el valor y la credencial nunca va al log, a la bitácora ni a la respuesta del listado.
- **Mismo 401 para todo.** Credencial ausente, desconocida o de un canal desactivado responden el mismo mensaje con `ErrorResponse`, para no revelar si una credencial existió. El filtro escribe ese 401 a mano (como `ManejadorErroresSeguridad`).
- **Autorización:** el filtro deja al canal autenticado con la autoridad `CANAL_EXTERNO` y la cadena exige esa autoridad en `/api/canal/**`. La gestión de canales (`/api/canales/**`), la bitácora y la resolución de conflictos son de **ADMINISTRADOR** con `@PreAuthorize`.
- **Constancia:** un canal desactivado queda en la bitácora como `NO_AUTORIZADA`; una credencial desconocida solo en el log (no hay a qué canal atribuirla y persistirla permitiría llenar la base con intentos).
- **Canal de demostración:** la credencial `DEMO` es pública a propósito (simulador y colección). Para producción se desactivan los datos de prueba o se desactiva el canal.

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
