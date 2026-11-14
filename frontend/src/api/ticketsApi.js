import { graphqlRequest } from './graphql';

// Operaciones de tickets contra el graphql-gateway (que delega en ticket-service).

const TICKET_FIELDS = 'id titulo descripcion prioridad estado tecnicoAsignado fechaCreacion';

const LISTAR = `
  query ListarTickets($estado: Estado, $prioridad: Prioridad, $page: Int, $size: Int) {
    tickets(estado: $estado, prioridad: $prioridad, page: $page, size: $size) {
      content { ${TICKET_FIELDS} }
      page size totalElements totalPages nextPage previousPage
    }
  }`;

const OBTENER = `
  query ObtenerTicket($id: ID!) {
    ticket(id: $id) { ${TICKET_FIELDS} }
  }`;

const CREAR = `
  mutation CrearTicket($titulo: String!, $descripcion: String!, $prioridad: Prioridad!) {
    crearTicket(titulo: $titulo, descripcion: $descripcion, prioridad: $prioridad) { ${TICKET_FIELDS} }
  }`;

const ACTUALIZAR_ESTADO = `
  mutation ActualizarEstadoTicket($id: ID!, $estado: Estado!) {
    actualizarEstadoTicket(id: $id, estado: $estado) { ${TICKET_FIELDS} }
  }`;

export async function listarTickets({ estado, prioridad, page = 0, size = 10 } = {}) {
  const data = await graphqlRequest(LISTAR, {
    estado: estado || null,
    prioridad: prioridad || null,
    page,
    size,
  });
  return data.tickets;
}

export async function obtenerTicket(id) {
  const data = await graphqlRequest(OBTENER, { id });
  return data.ticket;
}

export async function crearTicket({ titulo, descripcion, prioridad }) {
  const data = await graphqlRequest(CREAR, { titulo, descripcion, prioridad });
  return data.crearTicket;
}

export async function actualizarEstado(id, estado) {
  const data = await graphqlRequest(ACTUALIZAR_ESTADO, { id, estado });
  return data.actualizarEstadoTicket;
}
