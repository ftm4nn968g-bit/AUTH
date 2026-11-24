// Құпия сөзді көрсету/жасыру және күшін бағалау (тек көмекші UI — нақты тексеру серверде)
(function () {
  document.querySelectorAll('[data-toggle-pass]').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var input = document.getElementById(btn.getAttribute('data-toggle-pass'));
      var show = input.type === 'password';
      input.type = show ? 'text' : 'password';
      btn.textContent = show ? 'Жасыру' : 'Көрсету';
    });
  });

  var input = document.getElementById('password');
  var bar = document.querySelector('.meter span');
  var label = document.querySelector('.meter-label');
  if (!input || !bar || !label) return;

  var levels = [
    { w: '10%', c: '#c0392b', t: 'Өте әлсіз' },
    { w: '35%', c: '#e67e22', t: 'Әлсіз' },
    { w: '60%', c: '#b7791f', t: 'Орташа' },
    { w: '85%', c: '#1e8e5a', t: 'Мықты' },
    { w: '100%', c: '#0b6e5f', t: 'Өте мықты' }
  ];

  input.addEventListener('input', function () {
    var v = input.value, score = 0;
    if (v.length >= 8) score++;
    if (v.length >= 12) score++;
    if (/[a-zа-яәіңғүұқөһ]/.test(v) && /[A-ZА-ЯӘІҢҒҮҰҚӨҺ]/.test(v)) score++;
    if (/\d/.test(v)) score++;
    if (/[^\w\s]/.test(v)) score++;
    if (v.length < 8) score = Math.min(score, 1);
    var l = levels[Math.max(0, score - 1)];
    bar.style.width = v ? l.w : '0';
    bar.style.background = l.c;
    label.textContent = v ? 'Күші: ' + l.t : 'Кемінде 8 таңба, әріп және сан';
  });
})();
