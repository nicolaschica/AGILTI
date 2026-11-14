// Mensajes que ve el usuario cuando algo falla. NUNCA se muestra el texto
// crudo que devuelve el servidor: se elige el mensaje segun el contexto de
// la pantalla y el codigo del error (ApiError.code).

const REINTENTA = 'Inténtalo de nuevo en unos segundos.';

export const MENSAJES_ERROR = {
  listado: {
    titulo: 'No pudimos mostrar los tickets',
    texto: `Estamos teniendo un problema para cargar la información. ${REINTENTA}`,
  },
  crear: {
    titulo: 'No pudimos crear tu ticket',
    texto: `Hubo un problema al guardar tu solicitud. ${REINTENTA}`,
  },
  crearDatosInvalidos: {
    titulo: 'No pudimos crear tu ticket',
    texto: 'Algunos datos no son válidos. Revisa el formulario y vuelve a intentarlo.',
  },
  notificaciones: {
    titulo: 'No pudimos cargar las notificaciones',
    texto: `Estamos teniendo un problema para mostrarlas. ${REINTENTA}`,
  },
  detalle: {
    titulo: 'No pudimos mostrar el ticket',
    texto: `Estamos teniendo un problema para cargar la información. ${REINTENTA}`,
  },
  detalleNoEncontrado: {
    titulo: 'No encontramos este ticket',
    texto: 'Puede que el enlace esté incompleto o que el ticket ya no exista. Vuelve al listado para buscarlo.',
  },
  cambioEstado: {
    titulo: 'No pudimos cambiar el estado',
    texto: `Hubo un problema al guardar el cambio. ${REINTENTA}`,
  },
  cambioEstadoNoPermitido: {
    titulo: 'No pudimos cambiar el estado',
    texto: 'El ticket ya había cambiado de estado. Actualizamos la información para que veas su estado actual.',
  },
  cambioEstadoNoEncontrado: {
    titulo: 'No pudimos cambiar el estado',
    texto: 'Este ticket ya no está disponible. Vuelve al listado para ver los tickets actuales.',
  },
  notificacionesTicket: {
    titulo: 'No pudimos cargar las notificaciones de este ticket',
    texto: `Estamos teniendo un problema para mostrarlas. ${REINTENTA}`,
  },
};

// SERVICE_UNAVAILABLE, INTERNAL_ERROR, NETWORK_ERROR, TIMEOUT o cualquier
// codigo desconocido caen en el mensaje general de cada contexto.
export function mensajeDeError(err, contexto) {
  const code = err?.code;
  switch (contexto) {
    case 'detalle':
      return code === 'NOT_FOUND' || code === 'BAD_REQUEST'
        ? MENSAJES_ERROR.detalleNoEncontrado
        : MENSAJES_ERROR.detalle;
    case 'cambioEstado':
      if (code === 'BAD_REQUEST') return MENSAJES_ERROR.cambioEstadoNoPermitido;
      if (code === 'NOT_FOUND') return MENSAJES_ERROR.cambioEstadoNoEncontrado;
      return MENSAJES_ERROR.cambioEstado;
    case 'crear':
      return code === 'BAD_REQUEST' ? MENSAJES_ERROR.crearDatosInvalidos : MENSAJES_ERROR.crear;
    default:
      return MENSAJES_ERROR[contexto] ?? MENSAJES_ERROR.listado;
  }
}
