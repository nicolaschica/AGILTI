export const ESTADOS = ['ABIERTO', 'EN_ATENCION', 'CERRADO'];
export const PRIORIDADES = ['BAJA', 'MEDIA', 'ALTA'];

export const ESTADO_LABEL = {
  ABIERTO: 'Abierto',
  EN_ATENCION: 'En atención',
  CERRADO: 'Cerrado',
};

export const PRIORIDAD_LABEL = {
  BAJA: 'Baja',
  MEDIA: 'Media',
  ALTA: 'Alta',
};

// Mismas reglas que Estado.java: solo se avanza, sin retroceso.
export const SIGUIENTE_ESTADO = {
  ABIERTO: 'EN_ATENCION',
  EN_ATENCION: 'CERRADO',
  CERRADO: null,
};

export const ACCION_TRANSICION = {
  EN_ATENCION: 'Iniciar atención',
  CERRADO: 'Cerrar ticket',
};

export const PAGE_SIZES = [5, 10, 20, 50];

export function formatearFecha(iso) {
  if (!iso) return '—';
  const fecha = new Date(iso);
  if (Number.isNaN(fecha.getTime())) return iso;
  return fecha.toLocaleString('es-CO', { dateStyle: 'medium', timeStyle: 'short' });
}
