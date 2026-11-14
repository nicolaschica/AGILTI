import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { actualizarEstado, obtenerTicket } from '../api/ticketsApi';
import { historialPorTicket } from '../api/notificationsApi';
import { mensajeDeError } from '../api/errores';
import { EstadoBadge, PrioridadBadge } from '../components/Badges';
import ErrorMessage from '../components/ErrorMessage';
import Loading from '../components/Loading';
import {
  ACCION_TRANSICION, ESTADOS, ESTADO_LABEL, SIGUIENTE_ESTADO, formatearFecha,
} from '../constants';

export default function TicketDetallePage() {
  const { id } = useParams();
  const [ticket, setTicket] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [notFound, setNotFound] = useState(false);

  const [actualizando, setActualizando] = useState(false);
  const [errorTransicion, setErrorTransicion] = useState(null);
  const [exito, setExito] = useState(null);

  const [notificaciones, setNotificaciones] = useState(null);
  const [errorNotif, setErrorNotif] = useState(null);

  const cargar = useCallback(async () => {
    setLoading(true);
    setError(null);
    setNotFound(false);
    try {
      setTicket(await obtenerTicket(id));
    } catch (err) {
      setTicket(null);
      setNotFound(err.code === 'NOT_FOUND' || err.code === 'BAD_REQUEST');
      setError(mensajeDeError(err, 'detalle'));
    } finally {
      setLoading(false);
    }
  }, [id]);

  const cargarNotificaciones = useCallback(async () => {
    setErrorNotif(null);
    try {
      setNotificaciones(await historialPorTicket(id));
    } catch (err) {
      setNotificaciones(null);
      setErrorNotif(mensajeDeError(err, 'notificacionesTicket'));
    }
  }, [id]);

  useEffect(() => {
    cargar();
    cargarNotificaciones();
  }, [cargar, cargarNotificaciones]);

  const siguiente = ticket ? SIGUIENTE_ESTADO[ticket.estado] : null;

  const avanzarEstado = async () => {
    if (!siguiente) return;
    setActualizando(true);
    setErrorTransicion(null);
    setExito(null);
    try {
      const actualizado = await actualizarEstado(ticket.id, siguiente);
      setTicket(actualizado);
      setExito(`Estado actualizado a "${ESTADO_LABEL[actualizado.estado]}".`);
    } catch (err) {
      setErrorTransicion(mensajeDeError(err, 'cambioEstado'));
      // Transicion rechazada: el estado cambio en otro lado, se recarga.
      if (err.code === 'BAD_REQUEST') cargar();
    } finally {
      setActualizando(false);
    }
  };

  if (loading && !ticket) return <Loading texto="Cargando ticket…" />;

  if (error) {
    return (
      <section>
        <ErrorMessage
          titulo={error.titulo}
          mensaje={error.texto}
          onRetry={notFound ? undefined : cargar}
        />
        <Link to="/" className="btn btn-secondary">← Volver al listado</Link>
      </section>
    );
  }

  if (!ticket) return null;

  return (
    <section>
      <Link to="/" className="back-link">← Volver al listado</Link>

      <div className="card">
        <div className="detail-header">
          <h1 className="texto-largo">{ticket.titulo}</h1>
          <div className="badges">
            <PrioridadBadge prioridad={ticket.prioridad} />
            <EstadoBadge estado={ticket.estado} />
          </div>
        </div>

        <dl className="details">
          <dt>Descripción</dt>
          <dd className="pre texto-largo">{ticket.descripcion}</dd>
          <dt>Técnico asignado</dt>
          <dd>{ticket.tecnicoAsignado || <span className="muted">Sin asignar</span>}</dd>
          <dt>Fecha de creación</dt>
          <dd>{formatearFecha(ticket.fechaCreacion)}</dd>
          <dt>ID</dt>
          <dd className="mono">{ticket.id}</dd>
        </dl>

        <ol className="stepper" aria-label="Progreso del ticket">
          {ESTADOS.map((estado) => (
            <li
              key={estado}
              className={ESTADOS.indexOf(estado) <= ESTADOS.indexOf(ticket.estado) ? 'done' : ''}
            >
              {ESTADO_LABEL[estado]}
            </li>
          ))}
        </ol>

        {exito && <div className="alert alert-success" role="status"><p>{exito}</p></div>}
        {errorTransicion && (
          <ErrorMessage titulo={errorTransicion.titulo} mensaje={errorTransicion.texto} />
        )}

        <div className="actions">
          {siguiente ? (
            <button type="button" className="btn btn-primary" onClick={avanzarEstado} disabled={actualizando}>
              {actualizando ? 'Actualizando…' : `${ACCION_TRANSICION[siguiente]} → ${ESTADO_LABEL[siguiente]}`}
            </button>
          ) : (
            <p className="muted">Este ticket está cerrado. No hay más transiciones disponibles.</p>
          )}
        </div>
      </div>

      <div className="card">
        <div className="section-header">
          <h2>Notificaciones de este ticket</h2>
          <button type="button" className="btn btn-secondary" onClick={cargarNotificaciones}>Actualizar</button>
        </div>
        {errorNotif && (
          <ErrorMessage titulo={errorNotif.titulo} mensaje={errorNotif.texto} onRetry={cargarNotificaciones} />
        )}
        {!errorNotif && notificaciones === null && <Loading texto="Cargando notificaciones…" />}
        {notificaciones && notificaciones.length === 0 && (
          <p className="empty">
            Aún no hay notificaciones. Se generan automáticamente unos segundos después de crear el ticket; prueba a actualizar.
          </p>
        )}
        {notificaciones && notificaciones.length > 0 && (
          <ul className="notif-list">
            {notificaciones.map((n) => (
              <li key={n.id}>
                <p className="texto-largo">{n.mensaje}</p>
                <small className="muted">{formatearFecha(n.fechaEnvio)}</small>
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  );
}
