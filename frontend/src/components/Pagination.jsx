// Usa los booleanos previousPage / nextPage que devuelve el backend.
export default function Pagination({ pageData, onPageChange, disabled }) {
  if (!pageData) return null;
  const { page, totalPages, totalElements, previousPage, nextPage } = pageData;
  return (
    <div className="pagination">
      <button
        type="button"
        className="btn btn-secondary"
        disabled={disabled || !previousPage}
        onClick={() => onPageChange(page - 1)}
      >
        ← Anterior
      </button>
      <span className="pagination-info">
        Página {totalPages === 0 ? 0 : page + 1} de {totalPages} · {totalElements} en total
      </span>
      <button
        type="button"
        className="btn btn-secondary"
        disabled={disabled || !nextPage}
        onClick={() => onPageChange(page + 1)}
      >
        Siguiente →
      </button>
    </div>
  );
}
