// Builds the header and footer on every customer page and keeps the cart badge up to date.
import { api, session } from '../../shared/js/api.js';
import { BACKEND_CONFIGURED } from '../../shared/js/config.js';
import { $, h, param } from '../../shared/js/ui.js';

let cartBadge;

export function setCartCount(count) {
  if (!cartBadge) return;
  cartBadge.textContent = String(count);
  cartBadge.classList.toggle('hidden', !count);
}

export async function refreshCartCount() {
  if (!session.isLoggedIn) return setCartCount(0);
  try {
    const cart = await api('/api/cart');
    setCartCount(cart.totalQuantity);
  } catch {
    setCartCount(0);
  }
}

export function logout() {
  session.clear();
  window.location.href = 'index.html';
}

export function initLayout(active = '') {
  const user = session.user;
  cartBadge = h('span', { class: 'cart-badge hidden', 'aria-label': 'items in cart' }, '0');

  const searchInput = h('input', { type: 'search', name: 'q', placeholder: 'Search products', 'aria-label': 'Search products', maxlength: '100', value: param('q') || '' });

  const account = user
    ? [
        h('span', { class: 'nav-user', title: user.email }, `Hi, ${user.fullName.split(' ')[0]}`),
        user.role === 'ADMIN' ? h('a', { class: 'nav-link', href: '../admin/index.html' }, 'Admin') : null,
        h('button', { class: 'btn btn-secondary btn-sm', type: 'button', onclick: logout }, 'Log out'),
      ]
    : [h('a', { class: 'btn btn-sm', href: 'login.html' }, 'Log in')];

  const header = h(
    'div',
    { class: 'container header-inner' },
    h('a', { class: 'logo', href: 'index.html', 'aria-label': 'Cartly home' }, h('span', { class: 'logo-mark', 'aria-hidden': 'true' }, 'C'), h('span', { class: 'logo-text' }, 'Cartly')),
    h('form', { class: 'search', action: 'index.html', role: 'search' }, searchInput, h('button', { class: 'btn', type: 'submit' }, 'Search')),
    h(
      'nav',
      { class: 'nav', 'aria-label': 'Main' },
      h('a', { class: 'nav-link' + (active === 'orders' ? ' is-active' : ''), href: 'orders.html' }, 'My orders'),
      h('a', { class: 'nav-link nav-cart' + (active === 'cart' ? ' is-active' : ''), href: 'cart.html' }, 'Cart', cartBadge),
      ...account
    )
  );
  $('#site-header').replaceChildren(header);

  if (!BACKEND_CONFIGURED) {
    $('#site-header').append(h('div', { class: 'container' }, h('div', { class: 'notice' }, 'This website is not connected to its backend yet, so products cannot load. The backend address is set in frontend/shared/js/config.js.')));
  }

  $('#site-footer').replaceChildren(
    h('div', { class: 'container footer-inner' }, h('span', {}, 'Cartly is a college IP project built with Java Spring Boot and PostgreSQL.'), h('span', { class: 'muted' }, 'Payments are simulated. No real money is ever charged and no card details are collected.'))
  );

  refreshCartCount();
}
