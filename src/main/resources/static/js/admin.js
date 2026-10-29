/* VenueVista console — analytics charts (hand-rolled SVG, no dependencies) and gate check-in */
(function () {
  'use strict';
  var inr = function (n) { return '₹' + Math.round(n).toLocaleString('en-IN'); };
  var short = function (n) { return n >= 1e7 ? (n / 1e7).toFixed(1) + 'Cr' : n >= 1e5 ? (n / 1e5).toFixed(1) + 'L' : n >= 1e3 ? (n / 1e3).toFixed(0) + 'k' : String(n); };
  var esc = function (s) { return String(s).replace(/[&<>"']/g, function (c) { return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]; }); };
  var TIER = { VIP: '#ffb627', PREMIUM: '#8a6bff', STANDARD: '#38e8b4' };

  var dataEl = document.getElementById('analytics-data');
  if (dataEl) dashboard(JSON.parse(dataEl.textContent));

  function dashboard(d) {
    var k = d.kpis;
    var tiles = [
      ['Total revenue', inr(k.revenue), '#ffb627'], ['Tickets sold', k.tickets.toLocaleString('en-IN'), '#8a6bff'],
      ['Orders', k.orders.toLocaleString('en-IN'), '#38e8b4'], ['Avg. order', inr(k.avgOrder), '#ff5a3c'],
      ['Events on sale', k.liveEvents, '#ffb627'], ['Live occupancy', k.occupancy + '%', '#38e8b4'], ['Checked in', k.checkedIn.toLocaleString('en-IN'), '#8a6bff']
    ];
    document.getElementById('kpis').innerHTML = tiles.map(function (t) {
      return '<div class="kpi" style="--k:' + t[2] + '"><small>' + t[0] + '</small><b>' + t[1] + '</b></div>';
    }).join('');

    area(document.getElementById('areaChart'), d.daily);
    document.getElementById('rangeTotal').textContent = inr(d.daily.reduce(function (a, x) { return a + x.revenue; }, 0)) + ' · ' +
      d.daily.reduce(function (a, x) { return a + x.tickets; }, 0) + ' tickets';
    donut(document.getElementById('donut'), d.tiers);

    var maxRev = Math.max.apply(null, d.events.map(function (e) { return e.revenue; }).concat([1]));
    document.getElementById('eventBars').innerHTML = d.events.filter(function (e) { return e.sold > 0; }).slice(0, 8).map(function (e) {
      return '<div class="hbar"><div class="t">' + esc(e.title) + '<small>' + esc(e.start) + ' · ' + e.sold + '/' + e.capacity + ' seats · ' + e.occupancy + '%</small></div>' +
        '<div class="meter"><i style="width:' + Math.max(2, e.revenue / maxRev * 100) + '%"></i></div><div class="v">' + inr(e.revenue) + '</div></div>';
    }).join('') || '<p class="muted">No sales yet.</p>';

    var maxCat = Math.max.apply(null, d.categories.map(function (c) { return c.revenue; }).concat([1]));
    document.getElementById('catBars').innerHTML = d.categories.map(function (c) {
      return '<div class="hbar" style="grid-template-columns:90px 1fr auto"><div class="t">' + esc(c.name) + '<small>' + c.tickets + ' tickets</small></div>' +
        '<div class="meter"><i style="width:' + Math.max(2, c.revenue / maxCat * 100) + '%"></i></div><div class="v">' + short(c.revenue) + '</div></div>';
    }).join('');

    document.querySelector('#recent tbody').innerHTML = d.recent.map(function (o) {
      return '<tr><td class="mono"><a href="/order/' + esc(o.ref) + '" target="_blank">' + esc(o.ref) + '</a></td><td>' + esc(o.customer) + '</td><td>' + esc(o.event) +
        '</td><td class="r mono">' + o.tickets + '</td><td class="r mono">' + inr(o.total) + '</td><td class="mono muted">' + esc(o.at) + '</td></tr>';
    }).join('');
  }

  function area(host, pts) {
    var W = 800, H = 260, P = { l: 44, r: 10, t: 12, b: 26 };
    var max = Math.max.apply(null, pts.map(function (p) { return p.revenue; }).concat([1000]));
    var nice = Math.pow(10, Math.floor(Math.log10(max))), top = Math.ceil(max / nice) * nice;
    var x = function (i) { return P.l + (W - P.l - P.r) * i / (pts.length - 1); };
    var y = function (v) { return P.t + (H - P.t - P.b) * (1 - v / top); };
    var line = pts.map(function (p, i) { return (i ? 'L' : 'M') + x(i).toFixed(1) + ' ' + y(p.revenue).toFixed(1); }).join(' ');
    var svg = '<svg viewBox="0 0 ' + W + ' ' + H + '" role="img" aria-label="Revenue per day"><defs><linearGradient id="ag" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#ffb627" stop-opacity=".45"/><stop offset="1" stop-color="#ffb627" stop-opacity="0"/></linearGradient></defs>';
    for (var g = 0; g <= 4; g++) {
      var v = top * g / 4;
      svg += '<line x1="' + P.l + '" x2="' + (W - P.r) + '" y1="' + y(v) + '" y2="' + y(v) + '" stroke="rgba(255,244,230,.08)"/><text x="' + (P.l - 8) + '" y="' + (y(v) + 3) + '" text-anchor="end">' + short(v) + '</text>';
    }
    pts.forEach(function (p, i) { if (i % 5 === 0 && i < pts.length - 3 || i === pts.length - 1) svg += '<text x="' + x(i) + '" y="' + (H - 6) + '" text-anchor="' + (i === pts.length - 1 ? 'end' : 'middle') + '">' + p.label + '</text>'; });
    svg += '<path d="' + line + ' L' + x(pts.length - 1) + ' ' + y(0) + ' L' + x(0) + ' ' + y(0) + 'Z" fill="url(#ag)"/><path d="' + line + '" fill="none" stroke="#ffb627" stroke-width="2.5" stroke-linejoin="round"/>' +
      '<line id="hl" y1="' + P.t + '" y2="' + (H - P.b) + '" stroke="rgba(255,244,230,.35)" stroke-dasharray="3 3" opacity="0"/><circle id="hd" r="5" fill="#0a0910" stroke="#ffb627" stroke-width="2.5" opacity="0"/></svg><div class="tt"></div>';
    host.innerHTML = svg;
    var s = host.querySelector('svg'), tt = host.querySelector('.tt'), hl = host.querySelector('#hl'), hd = host.querySelector('#hd');
    s.addEventListener('mousemove', function (e) {
      var rect = s.getBoundingClientRect(), px = (e.clientX - rect.left) / rect.width * W;
      var i = Math.max(0, Math.min(pts.length - 1, Math.round((px - P.l) / (W - P.l - P.r) * (pts.length - 1))));
      var p = pts[i]; hl.setAttribute('x1', x(i)); hl.setAttribute('x2', x(i)); hl.setAttribute('opacity', 1);
      hd.setAttribute('cx', x(i)); hd.setAttribute('cy', y(p.revenue)); hd.setAttribute('opacity', 1);
      tt.innerHTML = inr(p.revenue) + '<small>' + p.label + ' · ' + p.tickets + ' tickets</small>';
      tt.style.left = (x(i) / W * rect.width) + 'px'; tt.style.top = (y(p.revenue) / H * rect.height) + 'px'; tt.style.opacity = 1;
    });
    s.addEventListener('mouseleave', function () { tt.style.opacity = 0; hl.setAttribute('opacity', 0); hd.setAttribute('opacity', 0); });
  }

  function donut(host, tiers) {
    var total = tiers.reduce(function (a, t) { return a + t.revenue; }, 0) || 1, R = 70, C = 2 * Math.PI * R, off = 0;
    var svg = '<svg viewBox="0 0 180 180" role="img" aria-label="Revenue by tier"><g transform="rotate(-90 90 90)"><circle cx="90" cy="90" r="' + R + '" fill="none" stroke="#1a1724" stroke-width="22"/>';
    tiers.forEach(function (t) {
      var len = t.revenue / total * C;
      svg += '<circle cx="90" cy="90" r="' + R + '" fill="none" stroke="' + TIER[t.tier] + '" stroke-width="22" stroke-dasharray="' + Math.max(0, len - 3) + ' ' + (C - Math.max(0, len - 3)) + '" stroke-dashoffset="' + (-off) + '"/>';
      off += len;
    });
    svg += '</g><text x="90" y="86" text-anchor="middle" style="font-size:11px">TOTAL</text><text x="90" y="108" text-anchor="middle" style="font-size:18px;fill:#f5efe4;font-family:Bricolage Grotesque,sans-serif;font-weight:700">' + short(total) + '</text></svg>';
    svg += '<div class="donut-legend">' + tiers.map(function (t) {
      return '<div><i class="dot ' + t.tier.toLowerCase() + '"></i><span>' + t.label + ' <small class="muted mono">' + t.sold + ' seats</small></span><span class="mono">' + inr(t.revenue) + '</span></div>';
    }).join('') + '</div>';
    host.innerHTML = svg;
  }

  // ---- gate check-in ----
  var ci = document.getElementById('ciForm');
  if (ci) {
    var input = document.getElementById('ciCode'), out = document.getElementById('ciResult'), log = document.getElementById('ciLog');
    ci.addEventListener('submit', function (e) {
      e.preventDefault();
      var code = input.value.trim(); if (!code) return;
      fetch('/admin/api/checkin', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ code: code }) })
        .then(function (r) { if (!r.ok) throw new Error('auth'); return r.json(); })
        .then(function (j) {
          var icon = { OK: '✓', ALREADY: '!', INVALID: '✕' }[j.status], head = { OK: 'Valid ticket', ALREADY: 'Already used', INVALID: 'Invalid ticket' }[j.status];
          out.className = 'ci-result ' + j.status.toLowerCase();
          out.innerHTML = '<span class="ci-icon">' + icon + '</span><h2>' + head + '</h2><p>' + esc(j.message) + '</p>' +
            (j.holder ? '<p><b>' + esc(j.holder) + '</b> · ' + esc(j.tier) + '</p><p class="seatline">' + esc(j.seat) + '</p><p class="muted">' + esc(j.event) + '</p>' : '');
          var li = document.createElement('li'); li.textContent = new Date().toLocaleTimeString() + '  ' + code.toUpperCase() + '  →  ' + j.status;
          log.prepend(li);
          input.value = ''; input.focus();
        })
        .catch(function () { out.className = 'ci-result invalid'; out.innerHTML = '<span class="ci-icon">✕</span><p>Session expired. Please sign in again.</p>'; });
    });
  }
})();
