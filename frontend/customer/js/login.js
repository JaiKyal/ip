import { api, session } from '../../shared/js/api.js';
import { $, h, param, withBusy } from '../../shared/js/ui.js';
import { initLayout } from './layout.js';

initLayout();

/** After logging in, go back to the page the user came from, but only to a plain page of this site (no open redirects). */
function nextPage() {
  const next = param('next');
  return next && /^[a-z0-9_-]+\.html(\?[A-Za-z0-9_=&%.-]*)?$/i.test(next) ? next : 'index.html';
}

if (session.isLoggedIn) window.location.replace(nextPage());

const message = $('#message');
function show(text, kind = 'form-error') {
  message.replaceChildren(text ? h('div', { class: kind }, text) : '');
}

function selectTab(which) {
  const login = which === 'login';
  $('#login-form').classList.toggle('hidden', !login);
  $('#register-form').classList.toggle('hidden', login);
  $('#tab-login').classList.toggle('is-active', login);
  $('#tab-register').classList.toggle('is-active', !login);
  $('#tab-login').setAttribute('aria-selected', String(login));
  $('#tab-register').setAttribute('aria-selected', String(!login));
  show('');
}
$('#tab-login').addEventListener('click', () => selectTab('login'));
$('#tab-register').addEventListener('click', () => selectTab('register'));
if (param('tab') === 'register') selectTab('register');

async function submit(event, path, body) {
  event.preventDefault();
  show('');
  const button = event.target.querySelector('button[type=submit]');
  await withBusy(button, async () => {
    try {
      session.save(await api(path, { method: 'POST', body, auth: false }));
      window.location.href = nextPage();
    } catch (error) {
      show(error.message);
    }
  });
}

$('#login-form').addEventListener('submit', (event) => {
  const email = $('#login-email').value.trim();
  const password = $('#login-password').value;
  if (!email || !password) {
    event.preventDefault();
    return show('Please enter your email and password.');
  }
  submit(event, '/api/auth/login', { email, password });
});

$('#register-form').addEventListener('submit', (event) => {
  const fullName = $('#reg-name').value.trim();
  const email = $('#reg-email').value.trim();
  const password = $('#reg-password').value;
  if (!fullName || !email.includes('@')) {
    event.preventDefault();
    return show('Please enter your name and a valid email.');
  }
  if (password.length < 8) {
    event.preventDefault();
    return show('Your password must have at least 8 characters.');
  }
  submit(event, '/api/auth/register', { fullName, email, password });
});
