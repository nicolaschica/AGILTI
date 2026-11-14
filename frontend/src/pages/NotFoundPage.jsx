import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <section className="card empty-page">
      <h1>Página no encontrada</h1>
      <p className="muted">La dirección que buscas no existe.</p>
      <Link to="/" className="btn btn-primary">Ir al listado de tickets</Link>
    </section>
  );
}
