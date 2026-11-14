import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { listarTickets } from '../api/ticketsApi';
import { mensajeDeError } from '../api/errores';
import { EstadoBadge, PrioridadBadge } from '../components/Badges';
import ErrorMessage from '../components/ErrorMessage';
import Loading from '../components/Loading';
import Pagination from '../components/Pagination';
import {
  ESTADOS, ESTADO_LABEL, PAGE_SIZES, PRIORIDADES, PRIORIDAD_LABEL, formatearFecha,
} from '../constants';

export default function TicketsListPage() {
  const navigate = useNavigate();
  // Filtros y pagina en la URL: se conservan al volver desde el detalle.
  const [searchParams, setSearchParams] = useSearchParams();
  const estado = searchParams.get('estado') || '';
  const prioridad = searchParams.get('prioridad') || '';
  const page = Number(searchParams.get('page') || 0);
  const size = Number(searchParams.get('size') || 10);

  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const cargar = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setData(await listarTickets({ estado, prioridad, page, size }));
    } catch (err) {
      setError(mensajeDeError(err, 'listado'));
    } finally {
      setLoading(false);
    }
  }, [estado, prioridad, page, size]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  const actualizarParams = (cambios) => {
    const next = new URLSearchParams(searchParams);
    Object.entries(cambios).forEach(([k, v]) => {
      if (v === '' || v === null || v === undefined) next.delete(k);
      else next.set(k, String(v));
    });
    setSearchParams(next);
  };

  const hayFiltros = Boolean(estado || prioridad);

  return (
    <section>
      <div className="page-header">
        <div>
          <h1>Tickets</h1>
          <p className="muted">Solicitudes de soporte registradas.</p>
        </div>
        <Link to="/tickets/nuevo" className="btn btn-primary">+ Nuevo ticket</Link>
      </div>

      <div className="card filters">
        <label>
          Estado
          <select value={estado} onChange={(e) => actualizarParams({ estado: e.target.value, page: '' })}>
            <option value="">Todos</option>
            {ESTADOS.map((e) => <option key={e} value={e}>{ESTADO_LABEL[e]}</option>)}
          </select>
        </label>
        <label>
          Prioridad
          <select value={prioridad} onChange={(e) => actualizarParams({ prioridad: e.target.value, page: '' })}>
            <option value="">Todas</option>
            {PRIORIDADES.map((p) => <option key={p} value={p}>{PRIORIDAD_LABEL[p]}</option>)}
          </select>
        </label>
        <label>
          Por página
          <select value={size} onChange={(e) => actualizarParams({ size: e.target.value, page: '' })}>
            {PAGE_SIZES.map((s) => <option key={s} value={s}>{s}</option>)}
          </select>
        </label>
        <div className="filters-actions">
          {hayFiltros && (
            <button
              type="button"
              className="btn btn-link"
              onClick={() => actualizarParams({ estado: '', prioridad: '', page: '' })}
            >
              Limpiar filtros
            </button>
          )}
          <button type="button" className="btn btn-secondary" onClick={cargar} disabled={loading}>
            Actualizar
          </button>
        </div>
      </div>

      {error && <ErrorMessage titulo={error.titulo} mensaje={error.texto} onRetry={cargar} />}

      {loading && !data && !error && <Loading texto="Cargando tickets…" />}

      {data && !error && (
        <div className={`card table-card${loading ? ' is-loading' : ''}`}>
          {data.content.length === 0 ? (
            <p className="empty">
              {hayFiltros ? 'No hay tickets que coincidan con los filtros.' : 'Aún no hay tickets registrados.'}
            </p>
          ) : (
            <div className="table-wrapper">
              <table className="table">
                <thead>
                  <tr>
                    <th>Título</th>
                    <th>Prioridad</th>
                    <th>Estado</th>
                    <th>Técnico</th>
                    <th>Fecha de creación</th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((t) => (
                    <tr key={t.id} className="row-link" onClick={() => navigate(`/tickets/${t.id}`)}>
                      <td data-label="Título" className="col-titulo">
                        <Link
                          to={`/tickets/${t.id}`}
                          className="titulo-recortado"
                          title={t.titulo}
                          onClick={(e) => e.stopPropagation()}
                        >
                          {t.titulo}
                        </Link>
                      </td>
                      <td data-label="Prioridad"><PrioridadBadge prioridad={t.prioridad} /></td>
                      <td data-label="Estado"><EstadoBadge estado={t.estado} /></td>
                      <td data-label="Técnico">
                        {t.tecnicoAsignado || <span className="muted">Sin asignar</span>}
                      </td>
                      <td data-label="Fecha">{formatearFecha(t.fechaCreacion)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <Pagination
            pageData={data}
            disabled={loading}
            onPageChange={(p) => actualizarParams({ page: p > 0 ? p : '' })}
          />
        </div>
      )}
    </section>
  );
}
