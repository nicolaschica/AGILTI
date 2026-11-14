// Reglas de creacion de ticket. La fuente de verdad es ticket-service
// (TicketCreateRequest.java + NormalizadorTicket.java); aqui se replican
// las MISMAS reglas, mensajes y orden de prioridad para avisar al instante.

export const TITULO_MIN = 5;
export const TITULO_MAX = 80;
export const DESCRIPCION_MIN = 10;

// Al menos una letra; solo letras (con tildes, Ü/ü, Ñ/ñ), digitos, espacio
// y los signos . , : ; - _ ( ) / # ¿ ? ¡ !
const TITULO_REGEX = /^(?=.*[A-Za-zÁÉÍÓÚÜáéíóúüÑñ])[A-Za-zÁÉÍÓÚÜáéíóúüÑñ0-9 .,:;\-_()\/#¿?¡!]+$/;

export const MENSAJES_VALIDACION = {
  tituloVacio: 'El título es obligatorio.',
  tituloCorto: 'Escribe un título de al menos 5 caracteres.',
  tituloLargo:
    'El título es muy largo. Resúmelo en pocas palabras (máximo 80 caracteres) y deja los detalles en la descripción.',
  tituloCaracteres:
    'El título contiene caracteres no permitidos. Usa solo letras, números y signos básicos como . , - ( ).',
  descripcionVacia: 'La descripción es obligatoria.',
  descripcionCorta:
    'La descripción debe tener al menos 10 caracteres. Cuéntanos un poco más sobre el problema.',
  prioridadVacia: 'Selecciona una prioridad.',
};

// NFC + recorta espacios al inicio/fin + reduce espacios repetidos a uno.
// Solo toca el caracter espacio: tabs o saltos de linea se dejan para que
// la lista blanca los rechace (igual que el backend).
export function normalizarTitulo(titulo) {
  return (titulo ?? '').normalize('NFC').replace(/ {2,}/g, ' ').replace(/^ +| +$/g, '');
}

export function normalizarDescripcion(descripcion) {
  return (descripcion ?? '').trim();
}

// Orden igual al backend: obligatorio > caracteres > longitud.
export function validarTitulo(valor) {
  const titulo = normalizarTitulo(valor);
  if (!titulo) return MENSAJES_VALIDACION.tituloVacio;
  if (!TITULO_REGEX.test(titulo)) return MENSAJES_VALIDACION.tituloCaracteres;
  if (titulo.length < TITULO_MIN) return MENSAJES_VALIDACION.tituloCorto;
  if (titulo.length > TITULO_MAX) return MENSAJES_VALIDACION.tituloLargo;
  return undefined;
}

export function validarDescripcion(valor) {
  const descripcion = normalizarDescripcion(valor);
  if (!descripcion) return MENSAJES_VALIDACION.descripcionVacia;
  if (descripcion.length < DESCRIPCION_MIN) return MENSAJES_VALIDACION.descripcionCorta;
  return undefined;
}

export function validarPrioridad(valor, prioridadesValidas) {
  return prioridadesValidas.includes(valor) ? undefined : MENSAJES_VALIDACION.prioridadVacia;
}

const CAMPOS_BACKEND = ['titulo', 'descripcion', 'prioridad'];

// ticket-service responde 400 con "campo: mensaje". Devuelve { campo, mensaje }
// sin el prefijo tecnico, o null si el mensaje no corresponde a un campo.
export function errorDeCampoDelServidor(mensajeServidor) {
  const match = /^(\w+):\s*(.+)$/s.exec(mensajeServidor ?? '');
  if (!match || !CAMPOS_BACKEND.includes(match[1])) return null;
  return { campo: match[1], mensaje: match[2].trim() };
}
