/* VenueVista — interactive seating chart + checkout */
(function () {
  'use strict';
  var root = document.getElementById('seatmap');
  if (!root) return;

  var eventId = root.dataset.event;
  var bandsEl = document.getElementById('bands');
  var tip = document.getElementById('tip');
  var cartList = document.getElementById('cartList');
  var cartTotal = document.getElementById('cartTotal');
  var form = document.getElementById('checkout');
  var payBtn = document.getElementById('payBtn');
  var formMsg = document.getElementById('formMsg');
  var qVal = document.getElementById('qVal');

  var inr = function (n) { return '₹' + Number(n).toLocaleString('en-IN'); };
  var rowLabel = function (n) { var s = ''; while (n > 0) { var m = (n - 1) % 26; s = String.fromCharCode(65 + m) + s; n = Math.floor((n - 1) / 26); } return s; };

  var data = null, taken = new Set(), sel = new Map(), qty = 2, secIndex = {};

  function key(sid, r, s) { return sid + '-' + r + '-' + s; }

  function load(initial) {
    return fetch('/api/events/' + eventId + '/seatmap', { cache: 'no-store' })
      .then(function (r) { return r.json(); })
      .then(function (d) {
        data = d;
        var newTaken = new Set(d.taken);
        var lost = [];
        sel.forEach(function (v, k) { if (newTaken.has(k)) lost.push(k); });
        taken = newTaken;
        if (initial) { render(); }
        else { syncTaken(); }
        lost.forEach(function (k) { sel.delete(k); });
        if (lost.length) { flash('Someone just booked ' + lost.length + ' of your seats. They were removed.'); }
        if (lost.length) { paintSelection(); }
        updateCart();
      })
      .catch(function () { if (initial) bandsEl.innerHTML = '<p class="loading">Couldn\'t load the seat map. Please refresh.</p>'; });
  }

  function render() {
    bandsEl.innerHTML = '';
    data.bands.forEach(function (b) {
      var band = el('div', 'band');
      var lab = el('div', 'band-label');
      lab.innerHTML = '<i class="dot ' + b.tier.toLowerCase() + '"></i><b>' + b.label + '</b><span>' + inr(b.price) + '</span>';
      band.appendChild(lab);
      var secs = el('div', 'band-secs');
      b.sections.forEach(function (s) {
        secIndex[s.id] = { sec: s, tier: b.tier, price: b.price };
        var wrap = el('div', 'sec');
        var nm = el('div', 'sec-name'); nm.textContent = s.name; wrap.appendChild(nm);
        for (var r = 1; r <= s.rows; r++) {
          var row = el('div', 'srow');
          var l = el('span', 'rlabel'); l.textContent = rowLabel(r); row.appendChild(l);
          for (var c = 1; c <= s.seats; c++) {
            var btn = el('button', 'seat ' + b.tier.toLowerCase());
            btn.type = 'button';
            btn.dataset.sid = s.id; btn.dataset.r = r; btn.dataset.s = c;
            btn.setAttribute('aria-label', s.name + ' row ' + rowLabel(r) + ' seat ' + c);
            var k = key(s.id, r, c);
            if (taken.has(k)) { btn.classList.add('taken'); btn.disabled = true; }
            if (!data.open) { btn.classList.add('locked'); }
            row.appendChild(btn);
          }
          wrap.appendChild(row);
        }
        secs.appendChild(wrap);
      });
      band.appendChild(secs);
      bandsEl.appendChild(band);
    });
  }

  function syncTaken() {
    bandsEl.querySelectorAll('.seat').forEach(function (b) {
      var isTaken = taken.has(key(b.dataset.sid, b.dataset.r, b.dataset.s));
      b.classList.toggle('taken', isTaken); b.disabled = isTaken;
    });
  }

  function paintSelection() {
    bandsEl.querySelectorAll('.seat').forEach(function (b) {
      b.classList.toggle('sel', sel.has(key(b.dataset.sid, b.dataset.r, b.dataset.s)));
    });
  }

  function toggle(btn) {
    if (!data.open || btn.classList.contains('taken')) return;
    var k = key(btn.dataset.sid, btn.dataset.r, btn.dataset.s);
    if (sel.has(k)) { sel.delete(k); }
    else {
      if (sel.size >= data.max) { flash('You can pick up to ' + data.max + ' seats per order.'); return; }
      sel.set(k, { sid: +btn.dataset.sid, r: +btn.dataset.r, s: +btn.dataset.s });
    }
    paintSelection(); updateCart();
  }

  function updateCart() {
    cartList.innerHTML = '';
    var total = 0;
    if (!sel.size) { cartList.innerHTML = '<li class="cart-empty">Pick seats on the map to begin.</li>'; }
    var items = Array.from(sel.entries()).sort(function (a, b) { return a[1].sid - b[1].sid || a[1].r - b[1].r || a[1].s - b[1].s; });
    items.forEach(function (e) {
      var v = e[1], info = secIndex[v.sid]; total += info.price;
      var li = el('li', 'cart-item');
      li.innerHTML = '<i class="dot ' + info.tier.toLowerCase() + '"></i><b>' + info.sec.name + '<small>Row ' + rowLabel(v.r) + ' · Seat ' + v.s + '</small></b><span class="mono">' + inr(info.price) + '</span>';
      var x = el('button'); x.type = 'button'; x.innerHTML = '✕'; x.setAttribute('aria-label', 'Remove seat');
      x.onclick = function () { sel.delete(e[0]); paintSelection(); updateCart(); };
      li.appendChild(x); cartList.appendChild(li);
    });
    cartTotal.textContent = inr(total);
    if (payBtn) { payBtn.disabled = !sel.size; payBtn.textContent = sel.size ? 'Get ' + sel.size + (sel.size > 1 ? ' tickets' : ' ticket') + ' · ' + inr(total) : 'Get my tickets'; }
  }

  function flash(msg) { if (formMsg) { formMsg.textContent = msg; setTimeout(function () { if (formMsg.textContent === msg) formMsg.textContent = ''; }, 5000); } }

  // "best seats": most central contiguous block, closest to the stage, best tier first
  function findBest(n) {
    for (var bi = 0; bi < data.bands.length; bi++) {
      var best = null;
      data.bands[bi].sections.forEach(function (s) {
        for (var r = 1; r <= s.rows; r++) {
          var run = [];
          var flush = function () {
            if (run.length >= n) {
              for (var st = 0; st + n <= run.length; st++) {
                var mid = (run[st] + run[st + n - 1]) / 2, off = Math.abs(mid - (s.seats + 1) / 2);
                var score = r * 100 + off;
                if (!best || score < best.score) best = { score: score, sid: s.id, r: r, from: run[st], n: n };
              }
            }
            run = [];
          };
          for (var c = 1; c <= s.seats; c++) {
            if (taken.has(key(s.id, r, c))) flush(); else run.push(c);
          }
          flush();
        }
      });
      if (best) return best;
    }
    return null;
  }

  function init() {
    bandsEl.addEventListener('click', function (e) { var b = e.target.closest('.seat'); if (b) toggle(b); });
    bandsEl.addEventListener('mousemove', function (e) {
      var b = e.target.closest('.seat');
      if (!b) { tip.classList.remove('show'); return; }
      var info = secIndex[b.dataset.sid];
      tip.innerHTML = info.sec.name + ' · Row ' + rowLabel(+b.dataset.r) + ' · Seat ' + b.dataset.s +
        '<small>' + (b.classList.contains('taken') ? 'Sold' : inr(info.price) + ' · ' + info.tier) + '</small>';
      tip.style.left = (e.clientX + 14) + 'px'; tip.style.top = (e.clientY - 52) + 'px';
      tip.classList.add('show');
    });
    bandsEl.addEventListener('mouseleave', function () { tip.classList.remove('show'); });

    var minus = document.getElementById('qMinus'), plus = document.getElementById('qPlus');
    if (minus) minus.onclick = function () { qty = Math.max(1, qty - 1); qVal.textContent = qty; };
    if (plus) plus.onclick = function () { qty = Math.min(data.max, qty + 1); qVal.textContent = qty; };
    var best = document.getElementById('bestBtn'), clear = document.getElementById('clearBtn');
    if (best) best.onclick = function () {
      var f = findBest(qty);
      if (!f) { flash('No block of ' + qty + ' seats together is free. Try a smaller group.'); return; }
      sel.clear();
      for (var i = 0; i < f.n; i++) sel.set(key(f.sid, f.r, f.from + i), { sid: f.sid, r: f.r, s: f.from + i });
      paintSelection(); updateCart();
      var first = bandsEl.querySelector('.seat.sel'); if (first) first.scrollIntoView({ block: 'center', inline: 'center', behavior: 'smooth' });
    };
    if (clear) clear.onclick = function () { sel.clear(); paintSelection(); updateCart(); };

    if (form) form.addEventListener('submit', function (e) {
      e.preventDefault();
      if (!sel.size) return;
      payBtn.disabled = true; payBtn.textContent = 'Reserving your seats…'; formMsg.textContent = '';
      var fd = new FormData(form);
      fetch('/api/events/' + eventId + '/checkout', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: fd.get('name'), email: fd.get('email'),
          seats: Array.from(sel.values()).map(function (v) { return { sectionId: v.sid, row: v.r, seat: v.s }; })
        })
      }).then(function (r) { return r.json().then(function (j) { return { ok: r.ok, j: j }; }); })
        .then(function (res) {
          if (res.ok) { window.location.href = '/order/' + res.j.reference; return; }
          formMsg.textContent = res.j.error || 'Something went wrong.';
          load(false);
        })
        .catch(function () { formMsg.textContent = 'Network error. Please try again.'; updateCart(); });
    });

    // keep the map fresh while the buyer thinks
    setInterval(function () { if (!document.hidden) load(false); }, 15000);
  }

  load(true).then(function () { if (data) init(); updateCart(); });
})();

function el(tag, cls) { var e = document.createElement(tag); if (cls) e.className = cls; return e; }
