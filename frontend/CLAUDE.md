# SGA Hospeda — Frontend (Entrega 3)

Interfaz web del Sistema de Gestión de Alojamiento **Hospeda**, de Programación Avanzada (Universidad del Quindío). Consume la API del backend Spring Boot que vive en la raíz del repositorio (`../`). El contrato es `../docs/api/openapi.yaml`; la seguridad, `../docs/seguridad.md`; las reglas del negocio, `../CLAUDE.md` y `../docs/matriz-trazabilidad.md`.

## Reglas de trabajo (obligatorias)
- **Una rama y un commit por paso** (plan en `../CLAUDE.md`, sección "Entrega 3 (final)"): cada paso sale de `main` actualizado en su propia rama `feature/<tema>`, se rebasa con `git pull --rebase origin main` antes del commit y termina con push, Pull Request y fusión. No hagas commit sin que el usuario lo pida; nunca commit directo en `main`.
- **Equipo: Jhon Mendez (J5 a J10) y Miguel Angel Villegas (M1 a M4).** Cada paso lo hace la persona que lo lleva. Al rebasar, los archivos que ambos tocan (`App.tsx`, `rutas/rutas.ts`, `rutas/acceso.ts`, `Encabezado.tsx`) se resuelven conservando los cambios de los dos lados.
- Toda **integración** (correo, imágenes, mapa, IA, canal externo) cumple I-01 a I-08 (`../CLAUDE.md`): el frontend nunca depende de que una integración esté disponible; si falla, muestra un aviso y la pantalla sigue funcionando.
- No versionar secretos ni `.env.local`. Las variables `VITE_*` terminan dentro del JavaScript que descarga el navegador: nunca van claves.
- Explicar brevemente cada pieza creada: el equipo debe poder sustentarla.

## Stack
- **Vite 8 + React 19 + TypeScript 6** (Node 22.22 o superior; se recomienda 24 LTS).
- **react-router 8** (no existe `react-router-dom`: todo se importa de `react-router`). Rutas con `lazy`, salvo `Inicio`.
- **Bootstrap 5** (solo sus clases CSS, sin su JavaScript: los menús se controlan con estado de React) y **Axios** (un solo cliente).
- **react-hook-form + zod + @hookform/resolvers** para formularios; **sweetalert2** para notificaciones y confirmaciones.
- Lint con **oxlint**. TypeScript estricto con `verbatimModuleSyntax` (los tipos se importan con `import type`) y `erasableSyntaxOnly` (**no hay `enum`**: los enums del backend son uniones de texto).

```
npm install          # una vez
npm run dev          # http://localhost:5173 (el origen que el backend autoriza por CORS; el puerto es fijo)
npm run build        # tsc -b + vite build
npm run lint         # oxlint
```
`VITE_EDAD_ADULTO_RESPONSABLE` (18) solo sirve para AVISAR del adulto responsable (RP-03) en los formularios de reserva: la regla real vive en el backend.

El backend se arranca aparte (`../gradlew bootRun`, puerto 8080). La API se configura en `.env.development` (`VITE_API_URL=http://localhost:8080/api`); `.env.example` es la plantilla.

## Estructura de `src/`
| Carpeta | Contenido |
|---|---|
| `modelos/` | Interfaces y uniones de texto, **espejo exacto de los DTOs** del `openapi.yaml` (`apartamento`, `cotizacion`, `reserva`, `folio`, `tarifario`, `rechazo`, `auth`, `ocupante`, `paginacion`, `error`, `comun`) |
| `servicios/` | `api.ts` (el cliente Axios único, con los interceptores del token y del 401/403) y, por entidad, las funciones que llaman a la API (`authServicio`, `apartamentoServicio`, `disponibilidadServicio`, `cotizacionServicio`, `reservaServicio`, `folioServicio`) y `preferencias` (la fecha de nacimiento del huésped, en `sessionStorage`). Reciben un `AbortSignal` opcional para cancelar la consulta |
| `esquemas/` | Esquemas Zod de los formularios (solo validan la **forma**; ver las reglas) |
| `contexto/` | Contextos de React: la sesión y los filtros de la búsqueda y del listado de reservas de recepción (cada uno con su **reductor puro**, para que los filtros sobrevivan a cambiar de página) |
| `hooks/` | Hooks propios: `useAuth`, `useDebounce`, `useConsulta` (consulta con `AbortController`, `recargar()` y "cargando" derivado), `useApartamentos`, `useAutor`, `useFiltros`, `useFiltrosReservas` |
| `componentes/` | Piezas reutilizables sin lógica de negocio: `Layout`, `Encabezado`, `MenuDesplegable`, `PiePagina`, `Cargando`, `Paginacion`, `Campo`, `EstadoReservaBadge`, `EstadoOperativoBadge` |
| `paginas/` | Una carpeta por rol (`huesped/`, `recepcion/`, `servicio/`, `administracion/`) más las páginas comunes (`Inicio`, `Login`, `Registro`, `NoAutorizado`, `NoEncontrada`) |
| `rutas/` | `rutas.ts` (todas las rutas y patrones en un solo lugar) y, desde J2, `RutaProtegida` y `RutaPublica` |
| `utilidades/` | `formato.ts` (pesos COP sin decimales, fechas sin corrimiento de zona horaria) y `fechas.ts` |

**`App.tsx` declara las rutas** (con `lazy`, salvo `Inicio`) a partir de `rutas/rutas.ts`; las rutas protegidas se agrupan por rol con `RutaProtegida`.

## Páginas y a quién le toca llenarlas
| Paso | Quién | Páginas y componentes |
|---|---|---|
| J1–J4 | Jhon | Hechos: base, `Login`, `Registro`, huésped (`Buscar`, `DetalleApartamento`, `NuevaReservaHuesped`, `MisReservas`, `DetalleReserva`) y recepción (`Reservas`, `NuevaReservaRecepcion`, `GestionReserva`) |
| J5 | Jhon | Administración: `Apartamentos`, `ApartamentoFormulario`, `Bloqueos`, `TemporadasTarifas`, `PoliticaCancelacion`, `RechazosExternos` |
| J6 | Jhon | Administración `Alojamiento` (datos, parámetros y servicios adicionales); recepción `CalendarioOcupacion`; cargo de servicio adicional y medios de pago en `GestionReserva`; calendario de disponibilidad en `DetalleApartamento`; horarios, normas y mascotas en `Inicio`; aviso de capacidad en `ApartamentoFormulario` |
| M1 | Miguel | Servicio: `Preparacion` y novedades (registrar e historial) |
| J7 | Jhon | `Perfil`, recuperación de contraseña desde `Login`, administración `Usuarios` y `Notificaciones` |
| M2 | Miguel | Administración `Reportes` (rango de fechas, tablas, barras con CSS, descarga de Excel) |
| J8 | Jhon | Administración `Imágenes del apartamento`, galería en `DetalleApartamento`, imagen principal en `TarjetaApartamento` y mapa (Leaflet con OpenStreetMap) |
| M3 | Miguel | Botón "Sugerir gravedad con IA" en el formulario de novedad (no bloqueante) |
| J9 | Jhon | Administración `Canales`, `Conflictos` y `Bitácora` |
| J10 | Jhon | `VITE_API_URL` por entorno y `vercel.json` para el despliegue |
| M4 | Miguel | Pulido y verificación por rol |

**Rutas nuevas:** se agregan en `rutas/rutas.ts` (`RUTAS` y `PATRONES`), en `App.tsx` (con `lazy`, dentro del grupo de su rol), en `rutas/acceso.ts` (la regla de rol) y en el menú de `Encabezado.tsx`.

## Reglas del código
1. **Lenguaje ubicuo** en español, igual que el backend: apartamento, estancia, titular, ocupante, folio, cargo, pago, saldo, bloqueo, canal, temporada, tarifa. Nada de `room`, `booking`, `user`, `request`.
2. **Los modelos son espejo EXACTO de los DTOs** del `openapi.yaml`: mismos nombres de campo, sin inventar ninguno. Si el contrato cambia, se cambia primero el yaml (backend) y después el modelo. `PaginaResponse` usa `pagina` y `tamanio` (no `paginaActual` ni `tamanoPagina`, como en la guía de referencia). `valorTotal` de una reserva es el valor **neto**; el desglose, el valor **bruto** (RP-02).
3. **El backend decide el negocio.** El frontend no recalcula tarifas, descuentos, disponibilidad, retenciones ni saldos: los pide y los muestra. Zod y las validaciones del formulario solo cubren la **forma** (campos obligatorios, formatos, rangos evidentes); si el negocio lo rechaza, responde 409.
4. **Mostrar el `mensaje` del `ErrorResponse`.** Un 409 se muestra tal cual al usuario (es la regla de negocio explicada); un 400 trae `errores` por campo y se pinta en cada campo; un 401 lleva al login; un 403 muestra "sin permiso"; un 404, "no existe". Nunca se muestran trazas ni se inventa el texto del error.
5. **Paginación de 10 del servidor.** Todos los listados pagan de 10 en 10 desde el backend: el cliente pide `pagina` (desde 0) y pinta lo que llega con `<Paginacion>`; no corta, ordena ni filtra listas en memoria. Las reservas llegan de la más reciente a la más antigua.
6. **El catálogo de características viene de `GET /api/caracteristicas`** (L-06): filtros y formularios pintan lo que responde el backend; la constante `CARACTERISTICAS` de `modelos/apartamento.ts` solo tipa y valida. Pedir varias características exige TODAS.
7. **Dinero y fechas:** dinero en pesos enteros (`formatearPesos`, nunca decimales ni `float`); fechas como texto `AAAA-MM-DD` (`formatearFecha`, `hoyISO`), sin `new Date("AAAA-MM-DD")` ni `toISOString()` (corren el día por la zona horaria de Colombia). La estancia es `[entrada, salida)`.
8. **Un solo cliente HTTP** (`servicios/api.ts`): ningún componente llama a `axios` directamente ni crea otra instancia. Los componentes llaman a los servicios.
9. **Componentes de presentación sin lógica de negocio.** Estados con `useState`/`useReducer`; datos del servidor con `useEffect` y un indicador `Cargando`; nada de `any`.
10. **Seguridad:** el menú y las rutas dependen del rol (RECEPCIONISTA, ADMINISTRADOR, HUESPED, PERSONAL_SERVICIO), pero **la autorización real es del backend**; ocultar un botón no protege nada.
