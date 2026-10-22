import { api, redirectToLogin, session } from '../../shared/js/api.js';
import { $, PAYMENT_LABEL, badge, clear, emptyState, errorBox, formatDate, h, money, param, spinner } from '../../shared/js/ui.js';
import { initLayout } from './layout.js';

initLayout('orders');
const content = $('#content');
let page = Number(param('page')) || 0;

if (!session.isLoggedIn) redirectToLogin();

// ------------------------------------------------------------------ list

function orderCard(order) {
  return h(
    'a',
    { class: 'card order-card', href: `orders.html?id=${order.id}` },
    h('div', {}, h('div', { class: 'num' }, order.orderNumber), h('div', { class: 'meta' }, `${formatDate(order.orderDate)} · ${order.itemCount} item${order.itemCount === 1 ? '' : 's'} · ${PAYMENT_LABEL[order.paymentMethod]}`)),
    h('div', { class: 'right' }, h('div', { style: 'font-weight:800' }, money(order.totalAmount)), h('div', {}, badge(order.status), ' ', badge(order.paymentStatus)))
  );
}

async function loadList() {
  clear(content).append(h('h1', { text: 'My orders' }), spinner('Loading your orders'));
  try {
    const result = await api('/api/orders/my-orders', { query: { page, size: 10 } });
    clear(content).append(h('h1', { text: 'My orders' }));
    if (!result.items.length) {
      return content.append(emptyState('No orders yet', 'When you place an order it will show up here.', h('a', { class: 'btn', href: 'index.html' }, 'Start shopping')));
    }
    content.append(...result.items.map(orderCard));
    if (result.totalPages > 1) {
      const go = (target) => { page = target; loadList(); };
      content.append(h('div', { class: 'pager' }, h('button', { class: 'btn btn-secondary', type: 'button', disabled: page <= 0, onclick: () => go(page - 1) }, 'Newer'), h('span', { class: 'muted' }, `Page ${page + 1} of ${result.totalPages}`), h('button', { class: 'btn btn-secondary', type: 'button', disabled: page + 1 >= result.totalPages, onclick: () => go(page + 1) }, 'Older')));
    }
  } catch (error) {
    clear(content).append(h('h1', { text: 'My orders' }), errorBox(error.message, loadList));
  }
}

// ---------------------------------------------------------------- detail

const STEPS = ['PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED'];

function timeline(order) {
  if (order.status === 'CANCELLED') {
    return h('p', {}, badge('CANCELLED'), ' This order was cancelled.', order.paymentStatus === 'REFUNDED' ? ' Your (simulated) payment was refunded.' : '');
  }
  const reached = STEPS.indexOf(order.status);
  return h('div', { class: 'timeline', role: 'list', 'aria-label': 'Order progress' }, STEPS.map((step, index) => h('div', { class: 'step' + (index <= reached ? ' is-done' : ''), role: 'listitem' }, step.charAt(0) + step.slice(1).toLowerCase())));
}

function renderDetail(order, justPlaced) {
  document.title = `${order.orderNumber} - Cartly`;
  clear(content).append(h('nav', { class: 'breadcrumb' }, h('a', { href: 'orders.html' }, 'My orders'), ` / ${order.orderNumber}`));
  if (justPlaced) {
    content.append(h('div', { class: 'success-banner' }, h('h2', { text: 'Thank you! Your order is placed.' }), h('p', { style: 'margin:0' }, `Your order number is ${order.orderNumber}. ${order.paymentMethod === 'COD' ? 'Please keep the amount ready for delivery.' : 'Your simulated payment was approved.'}`)));
  }
  content.append(
    h('h1', {}, order.orderNumber),
    h('p', { class: 'muted' }, `Placed on ${formatDate(order.orderDate)}`),
    h('div', { class: 'card', style: 'margin-bottom:1rem' }, h('h2', { text: 'Status' }), timeline(order)),
    h(
      'div',
      { class: 'two-col' },
      h(
        'div',
        { class: 'table-wrap' },
        h('table', {}, h('thead', {}, h('tr', {}, h('th', {}, 'Item'), h('th', { class: 'num' }, 'Price'), h('th', { class: 'num' }, 'Qty'), h('th', { class: 'num' }, 'Total'))), h('tbody', {}, order.items.map((item) => h('tr', {}, h('td', {}, item.name), h('td', { class: 'num' }, money(item.unitPrice)), h('td', { class: 'num' }, String(item.quantity)), h('td', { class: 'num' }, money(item.lineTotal))))))
      ),
      h(
        'aside',
        { class: 'card' },
        h('h2', { text: 'Summary' }),
        h('div', { class: 'summary-line' }, h('span', {}, 'Subtotal'), h('span', {}, money(order.subtotal))),
        h('div', { class: 'summary-line' }, h('span', {}, 'Delivery'), h('span', {}, 'Free')),
        h('div', { class: 'summary-line summary-total' }, h('span', {}, 'Total'), h('span', {}, money(order.totalAmount))),
        h('h2', { text: 'Payment', style: 'margin-top:1rem' }),
        h('p', { class: 'muted', style: 'margin-bottom:0.25rem' }, PAYMENT_LABEL[order.paymentMethod]),
        h('p', {}, badge(order.paymentStatus)),
        order.payment ? h('p', { class: 'muted', style: 'font-size:0.85rem' }, `Simulated reference ${order.payment.reference}`) : null,
        h('h2', { text: 'Delivering to', style: 'margin-top:1rem' }),
        h('address', { style: 'font-style:normal' }, h('div', { text: order.shipping.name }), h('div', { text: order.shipping.address }), h('div', { text: `${order.shipping.city} ${order.shipping.pincode}` }), h('div', { text: order.shipping.phone }))
      )
    )
  );
}

async function loadDetail(id) {
  clear(content).append(spinner('Loading your order'));
  try {
    renderDetail(await api(`/api/orders/${id}`), param('placed') === '1');
  } catch (error) {
    if (error.status === 404) {
      clear(content).append(emptyState('Order not found', 'This order does not exist or is not yours.', h('a', { class: 'btn', href: 'orders.html' }, 'Back to my orders')));
    } else {
      clear(content).append(errorBox(error.message, () => loadDetail(id)));
    }
  }
}

if (session.isLoggedIn) {
  const id = Number(param('id'));
  if (Number.isInteger(id) && id > 0) loadDetail(id);
  else loadList();
}
