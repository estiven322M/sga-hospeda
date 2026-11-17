# Despliegue en la nube: Render (backend) y Vercel (frontend)

El backend (Spring Boot, raíz del repositorio) se empaqueta con el `Dockerfile` y corre en **Render**. El frontend (`frontend/`, React + Vite) corre en **Vercel**. Ningún valor secreto se escribe en el repositorio: todo entra por variables de entorno (requisito I-06).

> **Lo que hay que saber antes de la demostración**
> - Render gratuito **duerme** el servicio tras 15 minutos sin tráfico; la primera petición tarda hasta ~1 minuto (por eso el cliente HTTP del frontend espera 70 s).
> - La base es **H2 en memoria**: cada arranque parte de cero y `DefaultUserInitializer` y los sembradores de datos de prueba recrean usuarios, apartamentos, tarifas y reservas.
> - La memoria es de 512 MB: el `Dockerfile` limita el heap (`-XX:MaxRAMPercentage=70`, recolector serial).

## 1. Variables de entorno de Render

Perfil de la aplicación: `SPRING_PROFILES_ACTIVE=prod` (ya viene en el `Dockerfile`; se repite en Render por claridad). Render inyecta `PORT` solo: **no se define**.

**Obligatorias** (sin ellas la aplicación no arranca y el error nombra la variable):

| Variable | Para qué sirve | Valor sugerido / de dónde sale |
|---|---|---|
| `SGA_JWT_SECRET` | Clave HS256 con la que se firman los JWT (mínimo 32 caracteres) | **Generada** con el comando de la sección 2. No es la de `application-secrets.properties` (esa es solo local) |
| `SGA_CORS_ALLOWED_ORIGINS` | Orígenes del frontend autorizados, separados por comas, sin barra final | La URL de Vercel, p. ej. `https://sga-hospeda.vercel.app`. Se define cuando Vercel ya dio la URL; vacía = ningún origen autorizado (el API arranca, el navegador no puede llamarla) |
| `SGA_ADMIN_CORREO` | Correo del usuario ADMINISTRADOR | Inventada (p. ej. un correo del equipo) |
| `SGA_ADMIN_CONTRASENA` | Contraseña del administrador | Inventada, larga; no se escribe en el README ni en el repositorio |
| `SGA_RECEPCION_CORREO` | Correo del usuario RECEPCIONISTA | Inventada |
| `SGA_RECEPCION_CONTRASENA` | Contraseña del recepcionista | Inventada |
| `SGA_SERVICIO_CORREO` | Correo del usuario PERSONAL_SERVICIO | Inventada |
| `SGA_SERVICIO_CONTRASENA` | Contraseña del personal de servicio | Inventada |
| `SGA_HUESPED_CORREO` | Correo del usuario HUESPED (documento fijo 1094000111: las reservas sembradas son suyas) | Inventada |
| `SGA_HUESPED_CONTRASENA` | Contraseña del huésped | Inventada |
| `SGA_USUARIOS_INICIALES_HABILITADOS` | Crea los usuarios anteriores en cada arranque | **`true`**. Debe ir en `true`: la base se borra cuando Render duerme el servicio y, sin esto, tras cada reinicio no existiría ningún usuario para iniciar sesión. El inicializador no duplica usuarios que ya existan |

**Opcionales** (sin ellas se usa el adaptador local, que no necesita internet ni credenciales):

| Variable | Para qué sirve | Valor sugerido / de dónde sale |
|---|---|---|
| `SGA_CANAL_DEMO_HABILITADO` | Siembra el canal externo DEMO (su credencial es pública, está en el README) | `false` (por defecto en prod). `true` solo si se quiere ejecutar el simulador de canal contra el despliegue |
| `SGA_NOTIFICACIONES_PROVEEDOR` | Proveedor de avisos al titular: `local` o `brevo` | `brevo` para correos reales; Render bloquea el SMTP, por eso no `smtp` |
| `SGA_NOTIFICACIONES_BREVO_API_KEY` | Clave de la API de Brevo | Del panel de Brevo (SMTP y API > Claves API); también está en `application-secrets.properties` si ya se probó local |
| `SGA_NOTIFICACIONES_BREVO_REMITENTE` | Correo remitente verificado en Brevo | El correo verificado en Brevo > Remitentes |
| `SGA_IMAGENES_PROVEEDOR` | Almacén de imágenes: `local` o `cloudinary` | `cloudinary` (el disco de Render es efímero) |
| `SGA_IMAGENES_CLOUDINARY_CLOUD_NAME` | Cloud name de Cloudinary | Del panel de Cloudinary (o de `application-secrets.properties`) |
| `SGA_IMAGENES_CLOUDINARY_API_KEY` | API Key de Cloudinary | Del panel de Cloudinary (o de `application-secrets.properties`) |
| `SGA_IMAGENES_CLOUDINARY_API_SECRET` | API Secret de Cloudinary | Del panel de Cloudinary (o de `application-secrets.properties`) |
| `SGA_IMAGENES_LOCAL_URL_BASE` | URL pública con la que el navegador pide las imágenes de ejemplo sembradas (almacén local) | `https://<servicio>.onrender.com/imagenes` (URL del backend + `/imagenes`) |
| `SGA_IA_HABILITADA` | Activa la sugerencia de gravedad con IA (L-21, paso M3) | `false`. Hoy la rama principal no tiene consumidor de esta variable |
| `SGA_IA_CLAVE` | Clave de la API de IA (Gemini de AI Studio) | Del panel de AI Studio; solo cuando exista M3 |

Variables propias de Render (no van en la tabla): `PORT` (la pone Render). Jackson, JPA y zona horaria ya van en `application.properties` / `JAVA_TOOL_OPTIONS`.

## 2. Generar `SGA_JWT_SECRET` (PowerShell)

```powershell
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
```

Copie la salida directamente al campo de Render; no la guarde en ningún archivo del repositorio. En Git Bash o Linux: `openssl rand -base64 48`.

## 3. Render (Public Git Repository)

1. En <https://render.com> > **New** > **Web Service** > **Public Git Repository**. Pegue la URL del repositorio de GitHub.
2. **Branch:** `main`. **Root Directory:** vacío (el backend está en la raíz).
3. **Runtime/Language:** **Docker** (Render detecta el `Dockerfile` de la raíz). **Instance Type:** Free.
4. **Advanced > Health Check Path:** `/actuator/health`.
5. **Environment:** agregue las variables de la sección 1 (al menos las obligatorias; deje `SGA_CORS_ALLOWED_ORIGINS` para el paso 7 si aún no tiene la URL de Vercel).
6. **Create Web Service.** El primer despliegue compila con Gradle y tarda varios minutos. Al terminar, `https://<servicio>.onrender.com/actuator/health` debe responder `{"status":"UP"}`.
7. Con la URL de Vercel (sección 4), vuelva a Render > Environment, ponga `SGA_CORS_ALLOWED_ORIGINS` y guarde (Render reinicia el servicio).
8. Anote la URL del backend en la tabla de la sección *Despliegue* del `README.md`.

## 4. Vercel (CLI)

Desde `frontend/` (requiere Node 22):

```powershell
cd frontend
npx vercel login
npx vercel link            # Project: sga-hospeda; Root: ./ (ya estamos en frontend/)
npx vercel env add VITE_API_URL production    # valor: https://<servicio>.onrender.com/api
npx vercel --prod
```

- `VITE_API_URL` es **obligatoria** en Vercel y lleva el prefijo `/api`, sin barra final. Vite la incrusta al compilar: si la cambia, vuelva a desplegar (`npx vercel --prod`).
- `vercel.json` reescribe cualquier ruta a `index.html` (la aplicación usa rutas del lado del cliente) y `package.json` fija Node `22.x`.
- La carpeta `.vercel/` que crea la CLI está en `.gitignore`.
- Con la URL de producción que muestra la CLI, complete el paso 7 de Render (CORS).

## 5. Comprobación rápida del despliegue

1. `GET https://<servicio>.onrender.com/actuator/health` → `UP` (la primera vez puede tardar un minuto).
2. Abra la URL de Vercel e inicie sesión con el usuario de una variable `SGA_*_CORREO`.
3. En el catálogo deben verse los apartamentos sembrados; `/swagger-ui.html` **no** existe en producción.

## 6. Problemas frecuentes

| Síntoma | Causa probable | Solución |
|---|---|---|
| En el navegador: *blocked by CORS policy* / el preflight falla | `SGA_CORS_ALLOWED_ORIGINS` vacía, con barra final o con otro dominio (el de preview de Vercel es distinto) | Poner exactamente el origen (`https://sga-hospeda.vercel.app`, sin `/` final); varios separados por comas; guardar y esperar el reinicio |
| Todas las llamadas dan **404** o devuelven el HTML de la página | `VITE_API_URL` no está definida en Vercel (el cliente usa `/api` en el propio Vercel) | Definirla y volver a desplegar |
| El login da **401** | Correo o contraseña distintos de los de las variables `SGA_*`; o el servicio reinició con `SGA_USUARIOS_INICIALES_HABILITADOS` distinto de `true` | Usar los valores de Render; dejar la variable en `true` |
| Tras un rato todo da 401 | El token vence a los 30 minutos | Volver a iniciar sesión |
| La primera petición tarda ~1 minuto o falla por tiempo | **Arranque en frío**: Render duerme el servicio tras 15 min | Esperar; abrir `/actuator/health` antes de la demostración. El cliente espera hasta 70 s |
| Los datos creados desaparecieron | La base es H2 en memoria y se reinicia con cada arranque | Es lo esperado en la demostración; los datos de prueba se siembran de nuevo |
| *Port scan timeout* / "No open ports detected" en Render | La aplicación no escucha en `PORT` o murió al arrancar | Revisar los logs; `server.port=${PORT:8080}` ya está en `application.properties`; no fije `PORT` a mano |
| El despliegue falla con `Could not resolve placeholder 'SGA_...'` | Falta una variable obligatoria | Definir la que nombra el error (sección 1) |
| `sga.jwt.secret debe tener al menos 32 caracteres` | `SGA_JWT_SECRET` demasiado corta | Generar una nueva con la sección 2 |
| Las imágenes de ejemplo no cargan | `SGA_IMAGENES_LOCAL_URL_BASE` apunta a `localhost` | Ponerla en `https://<servicio>.onrender.com/imagenes` |
| La compilación de Docker se queda sin memoria | Instancia gratuita de 512 MB al compilar | Reintentar el despliegue; si persiste, compilar con *Clear build cache & deploy* |
