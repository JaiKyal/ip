import { api, redirectToLogin, session } from '../../shared/js/api.js';
import { $, clear, emptyState, errorBox, h, money, param, productImage, spinner, stockLabel, toast } from '../../shared/js/ui.js';
import { initLayout, setCartCount } from './layout.js';

const PAGE_SIZE = 12;
const state = {
  q: param('q') || '',
  categoryId: param('categoryId') || '',
  sort: param('sort') || 'featured',
  page: Number(param('page')) || 0,
};
let categories = [];

initLayout();

/** Keeps the address bar in step with the filters, so a filtered page can be bookmarked or shared. */
function syncUrl() {
  const url = new URL(window.location.href);
  for (const [key, value] of Object.entries(state)) {
    const isDefault = value === '' || value === 0 || value === 'featured';
    if (isDefault) url.searchParams.delete(key);
    else url.searchParams.set(key, value);
  }
  window.history.replaceState(null, '', url);
}

async function addToCart(product, button) {
  if (!session.isLoggedIn) return redirectToLogin();
  button.disabled = true;
  try {
    const cart = await api('/api/cart/items', { method: 'POST', body: { productId: product.id, quantity: 1 } });
    setCartCount(cart.totalQuantity);
    toast(`${product.name} added to your cart`, 'success');
  } catch (error) {
    toast(error.message, 'error');
  } finally {
    button.disabled = false;
  }
}

function productCard(product) {
  const out = product.stockQuantity <= 0;
  const button = h('button', { class: 'btn btn-block', type: 'button', disabled: out }, out ? 'Out of stock' : 'Add to cart');
  button.addEventListener('click', () => addToCart(product, button));
  return h(
    'article',
    { class: 'product-card' + (out ? ' is-out' : '') },
    h('a', { class: 'thumb', href: `product.html?id=${product.id}`, 'aria-label': product.name }, productImage(product.imageUrl, product.name)),
    h(
      'div',
      { class: 'body' },
      h('span', { class: 'category' }, product.category.name),
      h('a', { class: 'name', href: `product.html?id=${product.id}` }, product.name),
      h('span', { class: 'price' }, money(product.price)),
      stockLabel(product.stockQuantity),
      h('div', { class: 'actions' }, button)
    )
  );
}

function renderPager(totalPages) {
  const pager = clear($('#pager'));
  if (totalPages <= 1) return;
  const go = (page) => {
    state.page = page;
    load();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };
  pager.append(
    h('button', { class: 'btn btn-secondary', type: 'button', disabled: state.page <= 0, onclick: () => go(state.page - 1) }, 'Previous'),
    h('span', { class: 'muted' }, `Page ${state.page + 1} of ${totalPages}`),
    h('button', { class: 'btn btn-secondary', type: 'button', disabled: state.page + 1 >= totalPages, onclick: () => go(state.page + 1) }, 'Next')
  );
}

async function load() {
  syncUrl();
  const host = clear($('#products'));
  host.append(spinner('Loading products'));
  try {
    const result = await api('/api/products', {
      auth: false,
      query: { q: state.q, categoryId: state.categoryId, sort: state.sort, page: state.page, size: PAGE_SIZE },
    });
    clear(host);
    const forText = state.q ? ` for "${state.q}"` : '';
    $('#result-count').textContent = `${result.totalElements} product${result.totalElements === 1 ? '' : 's'}${forText}`;
    if (result.items.length) {
      host.append(h('div', { class: 'product-grid' }, result.items.map(productCard)));
    } else {
      host.append(emptyState('No products found', 'Try a different search or category.', h('a', { class: 'btn', href: 'index.html' }, 'Clear filters')));
    }
    renderPager(result.totalPages);
  } catch (error) {
    clear(host).append(errorBox(error.message, load));
    clear($('#pager'));
    $('#result-count').textContent = '';
  }
}

function categoryChip(label, id) {
  const active = String(state.categoryId) === String(id);
  return h(
    'button',
    {
      class: 'chip' + (active ? ' is-active' : ''),
      type: 'button',
      'aria-pressed': String(active),
      onclick: () => {
        state.categoryId = String(id);
        state.page = 0;
        renderCategories();
        load();
      },
    },
    label
  );
}

function renderCategories() {
  const host = clear($('#categories'));
  if (categories.length) host.append(categoryChip('All', ''), ...categories.map((c) => categoryChip(c.name, c.id)));
}

async function loadCategories() {
  try {
    categories = await api('/api/categories', { auth: false });
    renderCategories();
  } catch {
    // the product list reports the real error; the category bar is optional
  }
}

$('#sort').value = state.sort;
$('#sort').addEventListener('change', (event) => {
  state.sort = event.target.value;
  state.page = 0;
  load();
});

loadCategories();
load();
