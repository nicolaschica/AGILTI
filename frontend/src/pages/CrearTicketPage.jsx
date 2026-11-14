import { useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { crearTicket } from '../api/ticketsApi';
import { mensajeDeError } from '../api/errores';
import { EstadoBadge, PrioridadBadge } from '../components/Badges';
import ErrorMessage from '../components/ErrorMessage';
import { PRIORIDADES, PRIORIDAD_LABEL, formatearFecha } from '../constants';
import {
  MENSAJES_VALIDACION, TITULO_MAX, errorDeCampoDelServidor, normalizarDescripcion,
  normalizarTitulo, validarDescripcion, validarPrioridad, validarTitulo,
} from '../validation/ticketRules';

const FORM_INICIAL = { titulo: '', descripcion: '', prioridad: '' };
const CAMPOS = Object.keys(FORM_INICIAL);

// tituloExcedido: el usuario intento escribir o pegar mas de 80 caracteres;
// el input (maxLength) lo recorta, pero se le avisa con el mensaje de "largo".
function validar(form, tituloExcedido) {
  const errores = {
    titulo: validarTitulo(form.titulo)
      || (tituloExcedido ? MENSAJES_VALIDACION.tituloLargo : undefined),
    descripcion: validarDescripcion(form.descripcion),
    prioridad: validarPrioridad(form.prioridad, PRIORIDADES),
  };
  return Object.fromEntries(Object.entries(errores).filter(([, v]) => v));
}

// Longitud que tendria el valor si se inserta `texto` en la seleccion actual.
function longitudTrasInsertar(input, texto) {
  const { value, selectionStart, selectionEnd } = input;
  const seleccion = (selectionEnd ?? value.length) - (selectionStart ?? value.length);
  return value.length - seleccion + texto.length;
}

export default function CrearTicketPage() {
  const [form, setForm] = useState(FORM_INICIAL);
  const [tocados, setTocados] = useState({});
  const [erroresServidor, setErroresServidor] = useState({});
  const [tituloExcedido, setTituloExcedido] = useState(false);
  const excesoPendiente = useRef(false);
  const [enviando, setEnviando] = useState(false);
  const [errorServidor, setErrorServidor] = useState(null);
  const [creado, setCreado] = useState(null);

  const erroresLocales = validar(form, tituloExcedido);
  const hayErrores = Object.keys(erroresLocales).length > 0
    || Object.values(erroresServidor).some(Boolean);
  // Error visible por campo: el del servidor (si llego) o el local si el campo ya se toco.
  const errorDe = (campo) => erroresServidor[campo] || (tocados[campo] ? erroresLocales[campo] : undefined);

  const marcarTocado = (campo) => setTocados((t) => (t[campo] ? t : { ...t, [campo]: true }));

  const onChange = (e) => {
    const { name, value } = e.target;
    setForm((f) => ({ ...f, [name]: value }));
    marcarTocado(name);
    if (erroresServidor[name]) setErroresServidor((errs) => ({ ...errs, [name]: undefined }));
    if (name === 'titulo') {
      setTituloExcedido(excesoPendiente.current);
      excesoPendiente.current = false;
    }
  };

  const avisarSiExcede = (input, texto) => {
    if (texto && longitudTrasInsertar(input, texto) > TITULO_MAX) {
      excesoPendiente.current = true;
      setTituloExcedido(true);
      marcarTocado('titulo');
    }
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    setTocados(Object.fromEntries(CAMPOS.map((c) => [c, true])));
    if (hayErrores) return;

    setEnviando(true);
    setErrorServidor(null);
    try {
      const ticket = await crearTicket({
        titulo: normalizarTitulo(form.titulo),
        descripcion: normalizarDescripcion(form.descripcion),
        prioridad: form.prioridad,
      });
      setCreado(ticket);
      setForm(FORM_INICIAL);
      setTocados({});
      setTituloExcedido(false);
    } catch (err) {
      // Se conserva lo escrito. Si es una regla de validacion, su mensaje
      // va bajo el campo (sin el prefijo "titulo: "/"descripcion: ").
      const deCampo = err.code === 'BAD_REQUEST' ? errorDeCampoDelServidor(err.detalle) : null;
      if (deCampo) {
        setErroresServidor({ [deCampo.campo]: deCampo.mensaje });
      } else {
        setErrorServidor(mensajeDeError(err, 'crear'));
      }
    } finally {
      setEnviando(false);
    }
  };

  if (creado) {
    return (
      <section>
        <div className="alert alert-success" role="status">
          <div>
            <strong>¡Ticket creado!</strong>
            <p>
              Se asignó automáticamente a <b>{creado.tecnicoAsignado || 'ningún técnico'}</b>.
            </p>
          </div>
        </div>
        <div className="card">
          <h2 className="texto-largo">{creado.titulo}</h2>
          <dl className="details">
            <dt>Técnico asignado</dt>
            <dd>{creado.tecnicoAsignado || 'Sin asignar'}</dd>
            <dt>Prioridad</dt>
            <dd><PrioridadBadge prioridad={creado.prioridad} /></dd>
            <dt>Estado</dt>
            <dd><EstadoBadge estado={creado.estado} /></dd>
            <dt>Fecha de creación</dt>
            <dd>{formatearFecha(creado.fechaCreacion)}</dd>
            <dt>Descripción</dt>
            <dd className="pre texto-largo">{creado.descripcion}</dd>
            <dt>ID</dt>
            <dd className="mono">{creado.id}</dd>
          </dl>
          <div className="actions">
            <Link to={`/tickets/${creado.id}`} className="btn btn-primary">Ver detalle</Link>
            <button type="button" className="btn btn-secondary" onClick={() => setCreado(null)}>
              Crear otro ticket
            </button>
            <Link to="/" className="btn btn-link">Ir al listado</Link>
          </div>
        </div>
      </section>
    );
  }

  const errTitulo = errorDe('titulo');
  const errDescripcion = errorDe('descripcion');
  const errPrioridad = errorDe('prioridad');

  return (
    <section>
      <div className="page-header">
        <div>
          <h1>Crear ticket</h1>
          <p className="muted">El técnico se asigna automáticamente al guardar.</p>
        </div>
      </div>

      <form className="card form" onSubmit={onSubmit} noValidate>
        {errorServidor && (
          <ErrorMessage titulo={errorServidor.titulo} mensaje={errorServidor.texto} />
        )}

        <label className={errTitulo ? 'has-error' : ''}>
          Título *
          <input
            name="titulo"
            value={form.titulo}
            onChange={onChange}
            onBlur={() => marcarTocado('titulo')}
            onBeforeInput={(e) => avisarSiExcede(e.target, e.data)}
            onPaste={(e) => avisarSiExcede(e.target, e.clipboardData.getData('text'))}
            maxLength={TITULO_MAX}
            placeholder="Ej: No enciende el computador"
            aria-invalid={Boolean(errTitulo)}
            aria-describedby="titulo-ayuda"
          />
          <span id="titulo-ayuda" className="field-hint">
            <span>Entre 5 y 80 caracteres.</span>
            <span>{normalizarTitulo(form.titulo).length}/{TITULO_MAX}</span>
          </span>
          {errTitulo && <span className="field-error">{errTitulo}</span>}
        </label>

        <label className={errDescripcion ? 'has-error' : ''}>
          Descripción *
          <textarea
            name="descripcion"
            rows={5}
            value={form.descripcion}
            onChange={onChange}
            onBlur={() => marcarTocado('descripcion')}
            placeholder="Describe el problema con el mayor detalle posible"
            aria-invalid={Boolean(errDescripcion)}
          />
          {errDescripcion && <span className="field-error">{errDescripcion}</span>}
        </label>

        <label className={errPrioridad ? 'has-error' : ''}>
          Prioridad *
          <select
            name="prioridad"
            value={form.prioridad}
            onChange={onChange}
            onBlur={() => marcarTocado('prioridad')}
            aria-invalid={Boolean(errPrioridad)}
          >
            <option value="">Selecciona…</option>
            {PRIORIDADES.map((p) => <option key={p} value={p}>{PRIORIDAD_LABEL[p]}</option>)}
          </select>
          {errPrioridad && <span className="field-error">{errPrioridad}</span>}
        </label>

        <div className="actions">
          <button type="submit" className="btn btn-primary" disabled={enviando || hayErrores}>
            {enviando ? 'Creando…' : 'Crear ticket'}
          </button>
          <Link to="/" className="btn btn-link">Cancelar</Link>
          {hayErrores && !enviando && (
            <small className="muted">Completa los campos correctamente para poder crear el ticket.</small>
          )}
        </div>
      </form>
    </section>
  );
}
