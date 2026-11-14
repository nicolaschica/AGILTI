import { ESTADO_LABEL, PRIORIDAD_LABEL } from '../constants';

export function EstadoBadge({ estado }) {
  return (
    <span className={`badge badge-estado-${estado?.toLowerCase()}`}>
      {ESTADO_LABEL[estado] || estado}
    </span>
  );
}

export function PrioridadBadge({ prioridad }) {
  return (
    <span className={`badge badge-prioridad-${prioridad?.toLowerCase()}`}>
      {PRIORIDAD_LABEL[prioridad] || prioridad}
    </span>
  );
}
