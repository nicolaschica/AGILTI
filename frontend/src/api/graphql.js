// Cliente GraphQL minimo sobre fetch. El frontend SOLO habla con el
// graphql-gateway (POST {VITE_GATEWAY_URL}/graphql); no conoce las URLs de
// ticket-service ni de notification-service.
//
// Los errores GraphQL llegan con HTTP 200 en el arreglo "errors"; se
// convierten en un ApiError con el extensions.code del gateway
// (NOT_FOUND, BAD_REQUEST, SERVICE_UNAVAILABLE, INTERNAL_ERROR), o con
// NETWORK_ERROR / TIMEOUT si no hubo respuesta.
//
// ApiError.message es siempre un texto generico y amigable. El texto
// original del servidor queda en ApiError.detalle (solo para consola y
// para ubicar errores de validacion bajo su campo): las paginas NO deben
// mostrarlo; usan mensajeDeError() de ./errores.

const GATEWAY_URL = import.meta.env.VITE_GATEWAY_URL || 'http://localhost:8082';
const ENDPOINT = `${GATEWAY_URL}/graphql`;
const TIMEOUT_MS = 15000;
const MENSAJE_GENERICO = 'Estamos teniendo un problema. Inténtalo de nuevo en unos segundos.';

export class ApiError extends Error {
  constructor(code, detalle) {
    super(MENSAJE_GENERICO);
    this.name = 'ApiError';
    this.code = code;
    this.detalle = detalle;
  }
}

export async function graphqlRequest(query, variables = {}) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), TIMEOUT_MS);

  let response;
  try {
    response = await fetch(ENDPOINT, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify({ query, variables }),
      signal: controller.signal,
    });
  } catch (err) {
    clearTimeout(timer);
    const code = err?.name === 'AbortError' ? 'TIMEOUT' : 'NETWORK_ERROR';
    console.warn(`[api] ${code}`, err);
    throw new ApiError(code, String(err?.message ?? err));
  }

  let body = null;
  try {
    body = await response.json();
  } catch {
    // cuerpo vacio o no JSON
  } finally {
    clearTimeout(timer);
  }

  if (body?.errors?.length) {
    const [first] = body.errors;
    const code = first.extensions?.code || 'INTERNAL_ERROR';
    console.warn(`[api] ${code}`, first.message);
    throw new ApiError(code, first.message);
  }
  if (!response.ok || !body?.data) {
    console.warn('[api] respuesta inesperada', response.status);
    throw new ApiError('INTERNAL_ERROR', `status ${response.status}`);
  }
  return body.data;
}
