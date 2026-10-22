// The ONLY place the frontend talks to the backend. Every page uses api() so that the login token,
// error handling and "please log in again" behaviour are identical everywhere.
import { API_BASE, BACKEND_CONFIGURED } from './config.js';

const TOKEN_KEY = 'cartly.token';
const USER_KEY = 'cartly.user';
const EXPIRES_KEY = 'cartly.expiresAt';

function read(key) {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

function write(key, value) {
  try {
    if (value === null) localStorage.removeItem(key);
    else localStorage.setItem(key, value);
  } catch {
    // storage blocked (private mode): the user simply has to log in on each page load
  }
}

/** The logged-in user's token and details, kept in the browser. The server re-checks the token on every call. */
export const session = {
  get token() {
    const expires = Number(read(EXPIRES_KEY) || 0);
    if (expires && Date.now() > expires) {
      this.clear();
      return null;
    }
    return read(TOKEN_KEY);
  },
  get user() {
    if (!this.token) return null;
    try {
      return JSON.parse(read(USER_KEY));
    } catch {
      return null;
    }
  },
  save(authResponse) {
    write(TOKEN_KEY, authResponse.token);
    write(USER_KEY, JSON.stringify(authResponse.user));
    write(EXPIRES_KEY, String(Date.now() + authResponse.expiresInSeconds * 1000));
  },
  clear() {
    write(TOKEN_KEY, null);
    write(USER_KEY, null);
    write(EXPIRES_KEY, null);
  },
  get isLoggedIn() {
    return Boolean(this.token);
  },
  get isAdmin() {
    return this.user?.role === 'ADMIN';
  },
};

export class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

/** Sends the visitor to the login page of the current site (customer/ or admin/), remembering where they were. */
export function redirectToLogin() {
  const here = window.location.pathname.split('/').pop() + window.location.search;
  const target = new URL('login.html', window.location.href);
  if (here && !here.startsWith('login.html')) target.searchParams.set('next', here);
  window.location.href = target.href;
}

/**
 * api('/api/products', { query: { q: 'shoe' } })
 * api('/api/cart/items', { method: 'POST', body: { productId: 1, quantity: 2 } })
 * Throws ApiError with the server's message. A 401 on a logged-in session logs the user out and goes to login.
 */
export async function api(path, { method = 'GET', body, query, headers = {}, auth = true } = {}) {
  if (!BACKEND_CONFIGURED) {
    throw new ApiError(0, 'The backend address is not set up yet for this website.');
  }
  const url = new URL(API_BASE + path);
  for (const [key, value] of Object.entries(query || {})) {
    if (value !== undefined && value !== null && value !== '') url.searchParams.set(key, value);
  }

  const requestHeaders = { Accept: 'application/json', ...headers };
  const token = auth ? session.token : null;
  if (token) requestHeaders.Authorization = `Bearer ${token}`;
  if (body !== undefined) requestHeaders['Content-Type'] = 'application/json';

  let response;
  try {
    response = await fetch(url, {
      method,
      headers: requestHeaders,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, 'Cannot reach the server. Check that the backend is running and try again.');
  }

  let data = null;
  if ((response.headers.get('content-type') || '').includes('application/json')) {
    data = await response.json().catch(() => null);
  }

  if (!response.ok) {
    if (response.status === 401 && token) {
      session.clear();
      redirectToLogin();
    }
    throw new ApiError(response.status, data?.message || `Request failed (${response.status})`);
  }
  return data;
}
