import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { listarNotificaciones } from '../api/notificationsApi';
import { mensajeDeError } from '../api/errores';
import ErrorMessage from '../components/ErrorMessage';
import Loading from '../components/Loading';
import Pagination from '../components/Pagination';
import { formatearFecha } from '../constants';

const PAGE_SIZE = 20;

export default function NotificacionesPage() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const cargar = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setData(await listarNotificaciones({ page, size: PAGE_SIZE }));
    } catch (err) {
      setError(mensajeDeError(err, 'notificaciones'));
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  return (
    <section>
      <div className="page-header">
        <div>
          <h1>Notificaciones</h1>
          <p className="muted">
            Se generan automáticamente cada vez que se crea un ticket.
          </p>
        </div>
        <button type="button" className="btn btn-primary" onClick={cargar} disabled={loading}>
          {loading ? 'Actualizando…' : 'Actualizar'}
        </button>
      </div>

      {error && <ErrorMessage titulo={error.titulo} mensaje={error.texto} onRetry={cargar} />}

      {loading && !data && !error && <Loading texto="Cargando notificaciones…" />}

      {data && !error && (
        <div className={`card${loading ? ' is-loading' : ''}`}>
          {data.content.length === 0 ? (
            <p className="empty">Todavía no hay notificaciones. Crea un ticket para generar una.</p>
          ) : (
            <ul className="notif-list">
              {data.content.map((n) => (
                <li key={n.id}>
                  <p className="texto-largo">{n.mensaje}</p>
                  <div className="notif-meta">
                    <Link to={`/tickets/${n.ticketId}`} className="mono">Ticket {n.ticketId}</Link>
                    <small className="muted">{formatearFecha(n.fechaEnvio)}</small>
                  </div>
                </li>
              ))}
            </ul>
          )}
          <Pagination pageData={data} disabled={loading} onPageChange={setPage} />
        </div>
      )}
    </section>
  );
}
