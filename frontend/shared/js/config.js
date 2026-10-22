// Where the Java backend lives.
//  - On your own computer (localhost) it is the locally running backend.
//  - On the internet it is the hosted backend: set PRODUCTION_API_BASE below once the backend is deployed.
//  - For a one-off override (for example a temporary tunnel) run in the browser console:
//      localStorage.setItem('cartly.apiBase', 'https://your-backend.example.com')
//    and remove it again with localStorage.removeItem('cartly.apiBase').
const PRODUCTION_API_BASE = 'https://REPLACE-WITH-BACKEND-URL';

function readOverride() {
  try {
    return localStorage.getItem('cartly.apiBase');
  } catch {
    return null;
  }
}

const isLocal = ['localhost', '127.0.0.1'].includes(window.location.hostname);

export const API_BASE = (readOverride() || (isLocal ? 'http://localhost:8080' : PRODUCTION_API_BASE)).replace(/\/+$/, '');
export const BACKEND_CONFIGURED = !API_BASE.includes('REPLACE-WITH-BACKEND-URL');
