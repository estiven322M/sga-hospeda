# SGA — Sistema de gestión de alojamiento

> **Proyecto:** Sistema de Gestión de Alojamiento (SGA)
> **Alojamiento:** Hospeda
> **Curso:** Programación Avanzada
> **Institución:** Universidad del Quindío

---

## 1. Descripción del alojamiento
Sistema de gestión integral para el alojamiento turístico **Hospeda**, ubicado en Salento, Quindío. La plataforma permite administrar habitaciones, huéspedes, disponibilidad, reservas, pagos y servicios asociados al hospedaje, bajo una arquitectura hexagonal que favorece la separación de responsabilidades y la mantenibilidad del sistema.


---

## 2. Integrantes del equipo
* **Miguel Angel Villegas** — miguela.villegasv@uqvirtual.edu.co
* **Jhon Mendez** — jhons.mendeza@uqvirtual.edu.co

---

## 3. Requisitos para ejecutar
Para compilar y ejecutar este backend se requiere:
* **JDK 25** (OpenJDK / Temurin / Corretto 25).
* **Git** (para el control de versiones).
* **Gradle Wrapper** (incluido en el proyecto; no requiere instalación manual de Gradle).

---

## 4. Instrucciones de ejecución

### Desde la línea de comandos (Terminal)
1. Clonar el repositorio:
   ```bash
   git clone https://github.com/estiven322M/sga-hospeda.git
   cd sga-hospeda

---

## 5. Cómo probar la API

- **Arrancar:** `.\gradlew.bat bootRun` (Windows) o `./gradlew bootRun`. Puerto 8080, JDK 25.
- **Pruebas automáticas:** `.\gradlew.bat test`.
- **Clave del JWT (obligatoria para arrancar):** copiar `src/main/resources/application-secrets.properties.example` como `application-secrets.properties` (no se versiona) y poner una clave aleatoria de al menos 32 caracteres, o definir la variable de entorno `SGA_JWT_SECRET`.
- **Iniciar sesión:** `POST /api/auth/login` con un usuario inicial (`admin@sga.local` / `Admin2026`, `recepcion@sga.local` / `Recepcion2026`, `servicio@sga.local` / `Servicio2026`, `huesped1@sga.local` y `huesped2@sga.local` / `Huesped2026`) y enviar el token como `Authorization: Bearer <token>`.
- **Swagger UI:** http://localhost:8080/swagger-ui.html (botón **Authorize** para pegar el token)
- **Contrato de la API:** `docs/api/openapi.yaml` (coincide con los endpoints implementados).
- **Consola H2:** http://localhost:8080/h2-console, URL `jdbc:h2:mem:sgadb`, usuario `sa`, sin contraseña.
- **Datos de prueba:** se cargan solos al iniciar (apartamentos de ejemplo, temporadas BASE, MEDIA y ALTA, una política de cancelación, reservas en todos los estados y una por canal). Se desactivan con `sga.datos-prueba.habilitado=false` en `application.properties`. Los valores de la Ficha del Alojamiento están en `CatalogoDatosPrueba` y se ajustan en esa única clase.
- **CORS (para el frontend):** la API acepta peticiones del navegador desde los orígenes de la propiedad `sga.cors.origenes-permitidos` de `application.properties` (por defecto `http://localhost:5173`, el servidor de Vite; varios, separados por comas). Métodos GET, POST, PUT y OPTIONS; encabezados `Authorization` y `Content-Type`; `Location` expuesta. Un origen no autorizado recibe 403 en el preflight.
- **Endpoints para el frontend:** `GET /api/reservas/mias` (solo HUESPED: sus reservas por el claim `documento` del token, de 10 en 10, con filtro `estado`); `GET /api/reservas` admite además `salidaDesde`, `salidaHasta` (llegadas y salidas previstas del día) y `nombreTitular` (contiene, sin distinguir mayúsculas; `titular` sigue siendo el documento exacto); `GET /api/apartamentos` admite `caracteristicas` (deben tenerse todas) y `estadoOperativo`; `GET /api/caracteristicas` publica el catálogo.
- **Notificaciones, usuarios y recuperación de contraseña (J7):** `GET /api/notificaciones` (administrador: bandeja de avisos); `GET/POST /api/usuarios`, `PUT /api/usuarios/{id}/rol` y `PUT /api/usuarios/{id}/desactivar` (administrador); `GET/PUT /api/usuarios/yo` y `PUT /api/usuarios/yo/contrasena` (cualquier rol: el perfil propio); `POST /api/auth/recuperacion/solicitar` y `/confirmar` (públicos). `POST /api/reservas` admite el campo opcional `correoTitular`.
- **Imágenes (J8):** `POST /api/apartamentos/{id}/imagenes` (multipart, parte `imagenes`, hasta 10 por apartamento, JPG, PNG o WEBP de hasta 5 MB), `PUT .../imagenes/{idImagen}/principal` y `PUT .../imagenes/{idImagen}/retirar` (solo administrador); `PUT /api/usuarios/yo/foto` (multipart, parte `foto`, hasta 2 MB, cualquier rol). Los archivos del almacén local se sirven en `/imagenes/**`. Las respuestas del apartamento y de la disponibilidad traen `imagenPrincipal` e `imagenes`.
- **Colección de pruebas:** `docs/http/sga.http` (cliente HTTP de IntelliJ, entorno `local`). Se ejecuta **una sola vez, de arriba hacia abajo, con la aplicación recién arrancada**; si se repite en la misma sesión, las reservas de 2028 ya existen y varias solicitudes darán 409. Todos sus pasos llevan `client.test` sobre el cuerpo; para ejecutarla sin IntelliJ: `node docs/http/ejecutar-coleccion.js docs/http/sga.http http://localhost:8080`. El resultado de la última corrida está en `docs/http/resultado-corrida.md`.

### Notificaciones por correo (integración de salida, 10.3.2-A)

El sistema avisa al titular cuando su reserva se **crea, confirma o cancela**, y envía el **código de recuperación de contraseña**. Detrás del puerto `Notificador` hay dos adaptadores y se elige con una propiedad:

| `sga.notificaciones.proveedor` | Qué hace |
|---|---|
| `local` (**por defecto**) | No sale a internet ni pide credenciales: registra el aviso en el log y en una bandeja en memoria. Los avisos (y el código de recuperación) se leen en **`GET /api/notificaciones`** o en la página *Notificaciones* del administrador. |
| `smtp` | Envía correos reales por SMTP (Gmail). Tiempo máximo de espera: `sga.notificaciones.tiempo-espera-ms` (5000 ms). |

A quién se escribe: el correo de la **cuenta** del titular (mismo documento); si no tiene cuenta, el `correoTitular` opcional dado al crear la reserva; si no hay ninguno, el aviso queda registrado como `SIN_CORREO`. **Un fallo del correo nunca impide reservar, confirmar, cobrar ni cerrar un folio:** se notifica después de confirmar la transacción, se captura toda excepción, se registra en el log y el aviso queda `FALLIDA` en la bandeja.

**Cómo activar Gmail (envío real):**
1. En la cuenta de Google active la **verificación en dos pasos** y cree una **contraseña de aplicación** (Cuenta de Google → Seguridad → Contraseñas de aplicaciones). Son 16 caracteres; no es la contraseña de la cuenta.
2. Copie `src/main/resources/application-secrets.properties.example` como `application-secrets.properties` (no se versiona) y complete:
   ```properties
   sga.notificaciones.proveedor=smtp
   spring.mail.username=su.cuenta@gmail.com
   spring.mail.password=la-contraseña-de-aplicación-de-16-caracteres
   ```
   o defina las variables de entorno `SGA_NOTIFICACIONES_PROVEEDOR=smtp`, `SPRING_MAIL_USERNAME` y `SPRING_MAIL_PASSWORD`.
3. Reinicie la aplicación. Cree una reserva con `correoTitular` (o recupere una contraseña) y revise la bandeja del administrador: `ENVIADA` si salió, `FALLIDA` con la causa si no (el contenido del código de recuperación nunca queda en la bandeja con el proveedor real).

Las pruebas automáticas **nunca** llaman a un servidor de correo real: usan el adaptador local o un cliente simulado (el fallo se prueba contra un puerto cerrado de la misma máquina).

### Imágenes de apartamentos (integración de salida, 7.3)

Cada apartamento tiene de 1 a 10 imágenes, con una principal; **activar un apartamento exige al menos una**. Las imágenes se guardan detrás del puerto `AlmacenImagenes`:

| `sga.imagenes.proveedor` | Qué hace |
|---|---|
| `local` (**por defecto**) | Guarda en la carpeta `./imagenes` (`sga.imagenes.local.carpeta`) y la aplicación la sirve en `/imagenes/**`. No necesita internet ni credenciales. Los datos de prueba siembran 2 imágenes de ejemplo por apartamento aquí. |
| `cloudinary` | Sube a Cloudinary (plan gratuito, sin tarjeta) con tiempo máximo de espera `sga.imagenes.tiempo-espera-ms` (10000 ms). |

Si el almacén externo falla, el administrador ve un aviso claro (503), no se registra ninguna imagen del lote y **nada más se afecta**: las imágenes ya guardadas se siguen mostrando y reservar, cobrar y cerrar un folio no usan el almacén. Los tamaños máximos son parámetros: `sga.imagenes.tamano-maximo-mb` (5) y `sga.imagenes.tamano-maximo-perfil-mb` (2). El formato (JPG, PNG o WEBP) se reconoce por el contenido del archivo, no por su extensión.

**Cómo activar Cloudinary:**
1. Cree una cuenta gratuita en cloudinary.com y copie del panel el *Cloud name*, la *API Key* y el *API Secret*.
2. Copie `src/main/resources/application-secrets.properties.example` como `application-secrets.properties` (no se versiona) y complete `sga.imagenes.cloudinary.cloud-name`, `api-key` y `api-secret`; luego agregue `sga.imagenes.proveedor=cloudinary` (o use las variables de entorno `SGA_IMAGENES_PROVEEDOR`, `SGA_IMAGENES_CLOUDINARY_CLOUD_NAME`, `..._API_KEY` y `..._API_SECRET`).
3. Reinicie. Si faltan credenciales, la aplicación no arranca y dice cuáles. Las imágenes subidas quedan en la carpeta `sga/` de su cuenta de Cloudinary.

### Mapa del alojamiento (frontend)
El frontend muestra la ubicación del alojamiento (latitud y longitud de `GET /api/alojamiento`) con **Leaflet y OpenStreetMap** (no necesitan cuenta ni clave) en la búsqueda y en el detalle del apartamento. Si el mapa no carga (sin internet), se ve la dirección con las coordenadas y un enlace, sin romper la página.

---

## 6. Documentación

- `docs/matriz-trazabilidad.md`: regla de negocio, su implementación, su prueba y el endpoint que la expone.
- `docs/diagrama-clases-dominio.md`: diagrama de clases del dominio.
- `docs/contrato-persistencia.md`: contrato entre la capa de aplicación y la persistencia.
- `docs/seguridad.md`: seguridad con JWT, CORS, matriz de roles por endpoint y decisiones (Guía 12).
- `docs/http/resultado-corrida.md`: evidencia de la ejecución completa de `sga.http` con aserciones de cuerpo.

---

## 7. Alcance de esta entrega

Con la Guía 12 el backend incorpora **seguridad con JWT** (Spring Security 7, HS256): todo endpoint exige token salvo el login y el registro público, cada endpoint se protege por rol y un huésped solo accede a sus propias reservas y folios (propietario del recurso). Respuestas `401` (sin token, token inválido o vencido, credenciales erradas) y `403` (rol o propietario). Detalle en `docs/seguridad.md`.