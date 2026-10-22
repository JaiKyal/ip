import { api, redirectToLogin, session } from '../../shared/js/api.js';
import { $, clear, emptyState, errorBox, h, money, productImage, spinner, stockLabel, toast } from '../../shared/js/ui.js';
import { initLayout, setCartCount } from './layout.js';

initLayout('cart');
const content = $('#content');

if (!session.isLoggedIn) {
  redirectToLogin();
}

let cart;

function show(newCart) {
  cart = newCart;
  setCartCount(cart.totalQuantity);
  render();
}

async function change(action, failureMessage) {
  try {
    show(await action());
  } catch (error) {
    toast(error.message || failureMessage, 'error');
    load(); // the server may have changed the numbers (for example stock), so show the real cart again
  }
}

function row(item) {
  const minus = h('button', { type: 'button', 'aria-label': `Decrease quantity of ${item.name}`, disabled: item.quantity <= 1 }, '−');
  const plus = h('button', { type: 'button', 'aria-label': `Increase quantity of ${item.name}`, disabled: item.quantity >= item.stockQuantity || item.quantity >= 99 }, '+');
  const setQuantity = (quantity) => change(() => api(`/api/cart/items/${item.id}`, { method: 'PATCH', body: { quantity } }));
  minus.addEventListener('click', () => setQuantity(item.quantity - 1));
  plus.addEventListener('click', () => setQuantity(item.quantity + 1));

  const remove = h('button', { class: 'btn btn-secondary btn-sm', type: 'button', 'aria-label': `Remove ${item.name} from cart` }, 'Remove');
  remove.addEventListener('click', () => change(() => api(`/api/cart/items/${item.id}`, { method: 'DELETE' })));

  return h(
    'div',
    { class: 'cart-row' + (item.available ? '' : ' is-unavailable') },
    h('a', { href: `product.html?id=${item.productId}` }, productImage(item.imageUrl, item.name)),
    h(
      'div',
      { class: 'info' },
      h('a', { class: 'name', href: `product.html?id=${item.productId}` }, item.name),
      h('div', { class: 'muted' }, `${money(item.unitPrice)} each`),
      item.available ? stockLabel(item.stockQuantity) : h('span', { class: 'stock stock-out' }, item.stockQuantity === 0 ? 'No longer in stock. Please remove it.' : `Only ${item.stockQuantity} left. Please lower the quantity.`),
      h('div', { style: 'margin-top:0.5rem; display:flex; gap:0.75rem; align-items:center; flex-wrap:wrap' }, h('div', { class: 'stepper', role: 'group', 'aria-label': 'Quantity' }, minus, h('output', {}, String(item.quantity)), plus), remove)
    ),
    h('div', { class: 'line' }, h('div', { class: 'total' }, money(item.lineTotal)))
  );
}

function render() {
  clear(content);
  if (!cart.items.length) {
    content.append(emptyState('Your cart is empty', 'Add something you like from the shop.', h('a', { class: 'btn', href: 'index.html' }, 'Continue shopping')));
    return;
  }
  const checkout = h('a', { class: 'btn btn-accent btn-block', href: 'checkout.html', 'aria-disabled': String(cart.hasUnavailableItems) }, 'Proceed to checkout');
  if (cart.hasUnavailableItems) checkout.addEventListener('click', (event) => event.preventDefault());

  if (cart.hasUnavailableItems) {
    content.append(h('div', { class: 'notice' }, 'Some items in your cart are no longer available in the quantity you chose. Fix them below to continue.'));
  }
  content.append(
    h(
      'div',
      { class: 'two-col' },
      h('div', { class: 'card' }, cart.items.map(row)),
      h(
        'aside',
        { class: 'card sticky' },
        h('h2', { text: 'Order summary' }),
        h('div', { class: 'summary-line' }, h('span', {}, `Items (${cart.totalQuantity})`), h('span', {}, money(cart.subtotal))),
        h('div', { class: 'summary-line' }, h('span', {}, 'Delivery'), h('span', { class: 'stock-in' }, 'Free')),
        h('div', { class: 'summary-line summary-total' }, h('span', {}, 'Total'), h('span', {}, money(cart.subtotal))),
        checkout,
        h('p', { class: 'muted', style: 'margin-top:0.75rem; font-size:0.85rem' }, 'The final price is calculated again on the server when you place the order.')
      )
    )
  );
}

async function load() {
  clear(content).append(spinner('Loading your cart'));
  try {
    show(await api('/api/cart'));
  } catch (error) {
    clear(content).append(errorBox(error.message, load));
  }
}

if (session.isLoggedIn) load();
