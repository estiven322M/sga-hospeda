import type {
  ActualizarServicioAdicionalRequest,
  CrearServicioAdicionalRequest,
  ServicioAdicionalResponse,
} from '../modelos/alojamiento';
import type { PaginaResponse } from '../modelos/paginacion';
import { api } from './api';

const ruta = (codigo: string) => `/servicios-adicionales/${encodeURIComponent(codigo)}`;

/** GET /servicios-adicionales (RECEPCIONISTA y ADMINISTRADOR): de 10 en 10, por código; opcionalmente solo los activos. */
export async function listarServiciosAdicionales(
  soloActivos: boolean,
  pagina: number,
  signal?: AbortSignal,
): Promise<PaginaResponse<ServicioAdicionalResponse>> {
  const respuesta = await api.get<PaginaResponse<ServicioAdicionalResponse>>('/servicios-adicionales', {
    params: { soloActivos, pagina },
    signal,
  });
  return respuesta.data;
}

/** Todos los servicios activos (para llenar un <select>), pidiendo página por página al servidor. */
export async function listarTodosLosServiciosActivos(signal?: AbortSignal): Promise<ServicioAdicionalResponse[]> {
  const todos: ServicioAdicionalResponse[] = [];
  let pagina = 0;
  let totalPaginas = 1;
  while (pagina < totalPaginas) {
    const respuesta = await listarServiciosAdicionales(true, pagina, signal);
    todos.push(...respuesta.contenido);
    totalPaginas = respuesta.totalPaginas;
    pagina += 1;
  }
  return todos;
}

/** POST /servicios-adicionales (ADMINISTRADOR): 201; nace activo. 409 si el código existe o el valor no es coherente con "genera cargo". */
export async function crearServicioAdicional(solicitud: CrearServicioAdicionalRequest): Promise<ServicioAdicionalResponse> {
  const respuesta = await api.post<ServicioAdicionalResponse>('/servicios-adicionales', solicitud);
  return respuesta.data;
}

/** PUT /servicios-adicionales/{codigo} (ADMINISTRADOR): el código no cambia. */
export async function actualizarServicioAdicional(
  codigo: string,
  solicitud: ActualizarServicioAdicionalRequest,
): Promise<ServicioAdicionalResponse> {
  const respuesta = await api.put<ServicioAdicionalResponse>(ruta(codigo), solicitud);
  return respuesta.data;
}

/** PUT .../baja: baja lógica; el servicio deja de poderse cargar y los cargos ya hechos se conservan. */
export async function darDeBajaServicioAdicional(codigo: string): Promise<ServicioAdicionalResponse> {
  const respuesta = await api.put<ServicioAdicionalResponse>(`${ruta(codigo)}/baja`, null);
  return respuesta.data;
}

/** PUT .../activar: vuelve a dar de alta un servicio. */
export async function activarServicioAdicional(codigo: string): Promise<ServicioAdicionalResponse> {
  const respuesta = await api.put<ServicioAdicionalResponse>(`${ruta(codigo)}/activar`, null);
  return respuesta.data;
}
