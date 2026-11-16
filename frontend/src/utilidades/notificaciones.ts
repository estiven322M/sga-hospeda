import Swal from 'sweetalert2';

// Un "toast": aviso pequeño en una esquina que se cierra solo. Swal.mixin crea una versión de Swal con estas
// opciones ya puestas.
const Toast = Swal.mixin({
  toast: true,
  position: 'top-end',
  showConfirmButton: false,
  timer: 3500,
  timerProgressBar: true,
});

// Los diálogos usan los botones de Bootstrap en lugar de los colores propios de SweetAlert2
const Dialogo = Swal.mixin({
  buttonsStyling: false,
  customClass: {
    confirmButton: 'btn btn-primary mx-1',
    cancelButton: 'btn btn-outline-secondary mx-1',
  },
});

// Funciones globales: se pueden llamar desde un componente, un servicio o un interceptor, porque SweetAlert2 pinta
// fuera del árbol de React. Usan `title` (texto plano): nunca `html`, para no inyectar contenido que venga del servidor.

export function notificarExito(mensaje: string): void {
  void Toast.fire({ icon: 'success', title: mensaje });
}

export function notificarError(mensaje: string): void {
  void Toast.fire({ icon: 'error', title: mensaje, timer: 6000 });
}

export function notificarAviso(mensaje: string): void {
  void Toast.fire({ icon: 'warning', title: mensaje, timer: 5000 });
}

/** Diálogo Sí/No antes de una acción que no se puede deshacer. Devuelve true si la persona confirma. */
export async function confirmarAccion(titulo: string, texto: string): Promise<boolean> {
  const resultado = await Dialogo.fire({
    title: titulo,
    text: texto,
    icon: 'question',
    showCancelButton: true,
    confirmButtonText: 'Sí, continuar',
    cancelButtonText: 'No',
  });
  return resultado.isConfirmed;
}

interface OpcionesPedirDato {
  titulo: string;
  texto: string;
  tipo: 'text' | 'textarea' | 'time';
  etiqueta: string;
  /** Texto del botón que confirma. */
  confirmar: string;
  /** Longitud máxima, si el backend la limita. */
  maximo?: number;
}

/**
 * Diálogo que pide UN dato obligatorio (el motivo de una cancelación, la hora estimada de llegada...). Devuelve el
 * texto escrito, o null si la persona cancela el diálogo.
 */
export async function pedirDato({ titulo, texto, tipo, etiqueta, confirmar, maximo }: OpcionesPedirDato): Promise<string | null> {
  const resultado = await Dialogo.fire({
    title: titulo,
    text: texto,
    input: tipo,
    inputLabel: etiqueta,
    inputAttributes: maximo ? { maxlength: String(maximo) } : {},
    showCancelButton: true,
    confirmButtonText: confirmar,
    cancelButtonText: 'Volver',
    inputValidator: (valor) => (valor.trim() === '' ? 'Este dato es obligatorio' : null),
  });
  return resultado.isConfirmed && typeof resultado.value === 'string' ? resultado.value.trim() : null;
}
