import { api, redirectToLogin, session } from '../../shared/js/api.js';
import { $, clear, emptyState, errorBox, h, money, spinner, toast, withBusy } from '../../shared/js/ui.js';
import { initLayout, setCartCount } from './layout.js';

initLayout('cart');
const content = $('#content');
const KEY_STORAGE = 'cartly.checkoutKey';

if (!session.isLoggedIn) redirectToLogin();

/**
 * One random key per checkout attempt. If the request is sent twice (double click, retry after a network
 * error) the server sees the same key and returns the same order instead of creating a second one.
 * The key is thrown away once an order succeeds.
 */
function checkoutKey() {
  try {
    let key = sessionStorage.getItem(KEY_STORAGE);
    if (!key) {
      key = crypto.randomUUID();
      sessionStorage.setItem(KEY_STORAGE, key);
    }
    return key;
  } catch {
    return crypto.randomUUID();
  }
}

function forgetKey() {
  try {
    sessionStorage.removeItem(KEY_STORAGE);
  } catch {
    // nothing to forget
  }
}

const PAYMENTS = [
  { value: 'UPI', title: 'UPI (simulated)', text: 'Approved instantly in this demo. No UPI id is asked for or stored.' },
  { value: 'CARD', title: 'Credit / debit card (simulated)', text: 'Approved instantly in this demo. No card number is asked for or stored.' },
  { value: 'COD', title: 'Cash on delivery', text: 'Pay when your order arrives.' },
];

function field(id, label, attrs = {}, hint) {
  return h('div', { class: 'field' }, h('label', { for: id }, label), h('input', { id, name: id, required: true, ...attrs }), hint ? h('div', { class: 'field-hint' }, hint) : null, h('div', { class: 'field-error', id: `${id}-error`, role: 'alert' }));
}

function render(cart) {
  clear(content);
  if (!cart.items.length) {
    return content.append(emptyState('Your cart is empty', 'Add something before checking out.', h('a', { class: 'btn', href: 'index.html' }, 'Continue shopping')));
  }
  if (cart.hasUnavailableItems) {
    return content.append(emptyState('Your cart needs attention', 'Some items are no longer available in the quantity you chose.', h('a', { class: 'btn', href: 'cart.html' }, 'Review your cart')));
  }

  const user = session.user;
  const formError = h('div', { class: 'form-error hidden', role: 'alert' });
  const submit = h('button', { class: 'btn btn-accent btn-block', type: 'submit' }, `Place order · ${money(cart.subtotal)}`);

  const form = h(
    'form',
    { class: 'card', novalidate: true },
    h('h2', { text: 'Delivery details' }),
    h('div', { class: 'grid-2' }, field('name', 'Full name', { autocomplete: 'name', maxlength: '120', value: user?.fullName || '' }), field('phone', 'Phone number', { autocomplete: 'tel', inputmode: 'tel', placeholder: '9876543210', maxlength: '14' }, '10 digits, optionally with +91')),
    field('address', 'Address', { autocomplete: 'street-address', maxlength: '300', placeholder: 'House number, street, area' }),
    h('div', { class: 'grid-2' }, field('city', 'City', { autocomplete: 'address-level2', maxlength: '80' }), field('pincode', 'PIN code', { autocomplete: 'postal-code', inputmode: 'numeric', maxlength: '6', placeholder: '560001' }, '6 digits')),
    h('h2', { text: 'Payment', style: 'margin-top:1rem' }),
    h('div', { class: 'sim-banner' }, 'Payments are simulated in this demo. No real money is charged and no card or UPI details are collected.'),
    h('div', { class: 'pay-options', role: 'radiogroup', 'aria-label': 'Payment method' }, PAYMENTS.map((option, index) => h('label', { class: 'pay-option' }, h('input', { type: 'radio', name: 'paymentMethod', value: option.value, checked: index === 0 }), h('span', {}, h('span', { class: 'title' }, option.title), h('br'), h('span', { class: 'muted' }, option.text))))),
    formError,
    submit
  );

  const summary = h(
    'aside',
    { class: 'card sticky' },
    h('h2', { text: 'Order summary' }),
    ...cart.items.map((item) => h('div', { class: 'summary-line' }, h('span', {}, `${item.name} × ${item.quantity}`), h('span', {}, money(item.lineTotal)))),
    h('div', { class: 'summary-line' }, h('span', {}, 'Delivery'), h('span', { class: 'stock-in' }, 'Free')),
    h('div', { class: 'summary-line summary-total' }, h('span', {}, 'Total'), h('span', {}, money(cart.subtotal))),
    h('a', { href: 'cart.html', class: 'muted' }, 'Edit cart')
  );
  content.append(h('div', { class: 'two-col' }, form, summary));

  function showFormError(message) {
    formError.textContent = message;
    formError.classList.remove('hidden');
    formError.scrollIntoView({ behavior: 'smooth', block: 'center' });
  }

  function validate() {
    const values = Object.fromEntries(new FormData(form));
    const problems = {
      name: values.name.trim() ? '' : 'Please enter your name',
      phone: /^\+?[0-9]{10,13}$/.test(values.phone.trim()) ? '' : 'Enter a valid phone number (10 to 13 digits)',
      address: values.address.trim().length >= 5 ? '' : 'Please enter your full address',
      city: values.city.trim().length >= 2 ? '' : 'Please enter your city',
      pincode: /^[0-9]{6}$/.test(values.pincode.trim()) ? '' : 'Enter a 6-digit PIN code',
    };
    for (const [id, message] of Object.entries(problems)) $(`#${id}-error`).textContent = message;
    return Object.values(problems).every((message) => !message) ? values : null;
  }

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    formError.classList.add('hidden');
    const values = validate();
    if (!values) return;

    await withBusy(submit, async () => {
      try {
        const order = await api('/api/checkout', {
          method: 'POST',
          headers: { 'Idempotency-Key': checkoutKey() },
          body: {
            shipping: { name: values.name.trim(), phone: values.phone.trim(), address: values.address.trim(), city: values.city.trim(), pincode: values.pincode.trim() },
            paymentMethod: values.paymentMethod,
          },
        });
        forgetKey();
        setCartCount(0);
        window.location.href = `orders.html?id=${order.id}&placed=1`;
      } catch (error) {
        if (error.status === 409) {
          toast(error.message, 'error');
          showFormError(`${error.message}. Please review your cart.`);
        } else {
          showFormError(error.message);
        }
      }
    });
  });
}

async function load() {
  clear(content).append(spinner('Preparing checkout'));
  try {
    render(await api('/api/cart'));
  } catch (error) {
    clear(content).append(errorBox(error.message, load));
  }
}

if (session.isLoggedIn) load();
