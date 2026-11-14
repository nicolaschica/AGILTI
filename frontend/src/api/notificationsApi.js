import { graphqlRequest } from './graphql';

// Notificaciones contra el graphql-gateway (que delega en notification-service).

const NOTIF_FIELDS = 'id ticketId mensaje fechaEnvio';

const LISTAR = `
  query ListarNotificaciones($page: Int, $size: Int) {
    notificaciones(page: $page, size: $size) {
      content { ${NOTIF_FIELDS} }
      page size totalElements totalPages nextPage previousPage
    }
  }`;

const POR_TICKET = `
  query NotificacionesPorTicket($ticketId: ID!) {
    notificacionesPorTicket(ticketId: $ticketId) { ${NOTIF_FIELDS} }
  }`;

export async function listarNotificaciones({ page = 0, size = 20 } = {}) {
  const data = await graphqlRequest(LISTAR, { page, size });
  return data.notificaciones;
}

export async function historialPorTicket(ticketId) {
  const data = await graphqlRequest(POR_TICKET, { ticketId });
  return data.notificacionesPorTicket;
}
