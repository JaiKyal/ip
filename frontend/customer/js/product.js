import { api, redirectToLogin, session } from '../../shared/js/api.js';
import { $, clear, emptyState, errorBox, h, money, param, productImage, spinner, stockLabel, toast } from '../../shared/js/ui.js';
import { initLayout, setCartCount } from './layout.js';

initLayout();
const content = $('#content');

function render(product) {
  document.title = `${product.name} - Cartly`;
  const out = product.stockQuantity <= 0;
  const maxQuantity = Math.min(product.stockQuantity, 99);
  let quantity = 1;

  const value = h('output', { 'aria-live': 'polite' }, '1');
  const minus = h('button', { type: 'button', 'aria-label': 'Decrease quantity', disabled: true }, '−');
  const plus = h('button', { type: 'button', 'aria-label': 'Increase quantity', disabled: maxQuantity <= 1 }, '+');
  const refresh = () => {
    value.textContent = String(quantity);
    minus.disabled = quantity <= 1;
    plus.disabled = quantity >= maxQuantity;
  };
  minus.addEventListener('click', () => { quantity -= 1; refresh(); });
  plus.addEventListener('click', () => { quantity += 1; refresh(); });

  const addButton = h('button', { class: 'btn', type: 'button', disabled: out }, out ? 'Out of stock' : 'Add to cart');
  addButton.addEventListener('click', async () => {
    if (!session.isLoggedIn) return redirectToLogin();
    addButton.disabled = true;
    try {
      const cart = await api('/api/cart/items', { method: 'POST', body: { productId: product.id, quantity } });
      setCartCount(cart.totalQuantity);
      toast(`${quantity} × ${product.name} added to your cart`, 'success');
    } catch (error) {
      toast(error.message, 'error');
    } finally {
      addButton.disabled = false;
    }
  });

  clear(content).append(
    h('nav', { class: 'breadcrumb', 'aria-label': 'Breadcrumb' }, h('a', { href: 'index.html' }, 'Shop'), ' / ', h('a', { href: `index.html?categoryId=${product.category.id}` }, product.category.name)),
    h(
      'div',
      { class: 'product-page' },
      h('div', { class: 'photo' }, productImage(product.imageUrl, product.name)),
      h(
        'div',
        {},
        h('span', { class: 'muted' }, product.category.name),
        h('h1', { text: product.name }),
        h('div', { class: 'price' }, money(product.price)),
        stockLabel(product.stockQuantity),
        h('p', { class: 'muted', style: 'margin-top:1rem' }, product.description),
        out
          ? null
          : h('div', { class: 'buy-row' }, h('div', { class: 'stepper', role: 'group', 'aria-label': 'Quantity' }, minus, value, plus), addButton),
        out ? h('div', { class: 'buy-row' }, addButton) : null,
        h('p', { class: 'muted' }, 'Free delivery. Pay by UPI, card or cash on delivery (payments are simulated in this demo).')
      )
    )
  );
}

async function load() {
  const id = Number(param('id'));
  if (!Number.isInteger(id) || id <= 0) {
    return clear(content).append(emptyState('Product not found', 'The link looks incorrect.', h('a', { class: 'btn', href: 'index.html' }, 'Back to the shop')));
  }
  clear(content).append(spinner('Loading product'));
  try {
    render(await api(`/api/products/${id}`, { auth: false }));
  } catch (error) {
    if (error.status === 404) {
      clear(content).append(emptyState('Product not found', 'It may have been removed.', h('a', { class: 'btn', href: 'index.html' }, 'Back to the shop')));
    } else {
      clear(content).append(errorBox(error.message, load));
    }
  }
}

load();
