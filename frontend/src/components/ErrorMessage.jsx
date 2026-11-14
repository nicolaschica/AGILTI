export default function ErrorMessage({ titulo = 'Ocurrió un error', mensaje, onRetry }) {
  if (!mensaje) return null;
  return (
    <div className="alert alert-error" role="alert">
      <div>
        <strong>{titulo}</strong>
        <p>{mensaje}</p>
      </div>
      {onRetry && (
        <button type="button" className="btn btn-secondary" onClick={onRetry}>
          Reintentar
        </button>
      )}
    </div>
  );
}
