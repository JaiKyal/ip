// Small helpers shared by every page. Nothing here uses innerHTML: text is always inserted as text,
// so a product name or address containing <script> can never run (protection against XSS).

/** h('div', { class: 'card', onclick: fn }, 'text', childElement) builds DOM nodes safely. */
export function h(tag, props = {}, ...children) {
  const element = document.createElement(tag);
  for (const [key, value] of Object.entries(props || {})) {
    if (value === undefined || value === null || value === false) continue;
    if (key === 'class') element.className = value;
    else if (key === 'text') element.textContent = value;
    else if (key.startsWith('on') && typeof value === 'function') element.addEventListener(key.slice(2), value);
    else if (value === true) element.setAttribute(key, '');
    else element.setAttribute(key, value);
  }
  for (const child of children.flat()) {
    if (child === null || child === undefined || child === false) continue;
    element.append(child instanceof Node ? child : document.createTextNode(String(child)));
  }
  return element;
}

export function clear(element) {
  element.replaceChildren();
  return element;
}

export const $ = (selector, root = document) => root.querySelector(selector);

const rupee = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' });
export const money = (amount) => rupee.format(Number(amount));

export function formatDate(iso) {
  return new Date(iso).toLocaleString('en-IN', { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}

export const param = (name) => new URLSearchParams(window.location.search).get(name);

/** Only allow plain http(s) image links; anything else (javascript:, data:) is ignored. */
export function safeUrl(value) {
  try {
    const url = new URL(value);
    return url.protocol === 'https:' || url.protocol === 'http:' ? url.href : null;
  } catch {
    return null;
  }
}

const FALLBACK_IMAGE =
  'data:image/svg+xml;utf8,' +
  encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="600" height="400" viewBox="0 0 600 400"><rect width="600" height="400" fill="#e5e7eb"/>' +
      '<g fill="none" stroke="#9ca3af" stroke-width="10" stroke-linecap="round" stroke-linejoin="round"><rect x="230" y="130" width="140" height="120" rx="12"/><circle cx="275" cy="175" r="14"/><path d="M240 240l45-45 30 30 20-20 25 35"/></g></svg>'
  );

/** An <img> that shows a neutral placeholder when the picture is missing or fails to load. */
export function productImage(url, alt, className = '') {
  const image = h('img', { class: className, alt: alt || '', loading: 'lazy', src: safeUrl(url) || FALLBACK_IMAGE });
  image.addEventListener('error', () => {
    if (image.src !== FALLBACK_IMAGE) image.src = FALLBACK_IMAGE;
  });
  return image;
}

let toastHost;
/** A short message in the corner: toast('Added to cart', 'success') or toast('Something failed', 'error'). */
export function toast(message, type = 'info') {
  if (!toastHost) {
    toastHost = h('div', { class: 'toast-host', role: 'status', 'aria-live': 'polite' });
    document.body.append(toastHost);
  }
  const item = h('div', { class: `toast toast-${type}` }, message);
  toastHost.append(item);
  setTimeout(() => item.remove(), type === 'error' ? 6000 : 3500);
}

export function spinner(label = 'Loading') {
  return h('div', { class: 'loading', role: 'status' }, h('span', { class: 'spinner', 'aria-hidden': 'true' }), h('span', { text: label }));
}

export function emptyState(title, message, action) {
  return h('div', { class: 'empty-state' }, h('h2', { text: title }), message ? h('p', { text: message }) : null, action || null);
}

export function errorBox(message, retry) {
  return h('div', { class: 'error-box', role: 'alert' }, h('p', { text: message }), retry ? h('button', { class: 'btn btn-secondary', onclick: retry }, 'Try again') : null);
}

/** Disables a button and shows a spinner while an async action runs, so it cannot be clicked twice. */
export async function withBusy(button, action) {
  const original = button.textContent;
  button.disabled = true;
  button.classList.add('is-busy');
  try {
    return await action();
  } finally {
    button.disabled = false;
    button.classList.remove('is-busy');
    button.textContent = original;
  }
}

const STATUS_LABEL = {
  PENDING: 'Pending', CONFIRMED: 'Confirmed', PROCESSING: 'Processing', SHIPPED: 'Shipped', DELIVERED: 'Delivered', CANCELLED: 'Cancelled',
  PAID: 'Paid', FAILED: 'Failed', REFUNDED: 'Refunded',
};
export function badge(status) {
  return h('span', { class: `badge badge-${String(status).toLowerCase()}` }, STATUS_LABEL[status] || status);
}

/** "In stock", "Only 3 left" or "Out of stock", coloured. */
export function stockLabel(quantity) {
  if (quantity <= 0) return h('span', { class: 'stock stock-out' }, 'Out of stock');
  if (quantity <= 5) return h('span', { class: 'stock stock-low' }, `Only ${quantity} left`);
  return h('span', { class: 'stock stock-in' }, 'In stock');
}

export const PAYMENT_LABEL = { UPI: 'UPI (simulated)', CARD: 'Card (simulated)', COD: 'Cash on delivery' };
