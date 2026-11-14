export default function Loading({ texto = 'Cargando…' }) {
  return (
    <div className="loading" role="status">
      <span className="spinner" aria-hidden="true" />
      {texto}
    </div>
  );
}
