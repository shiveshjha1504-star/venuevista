/* VenueVista — global flourishes: stage light, hero seat fan, scroll reveals */
(function () {
  'use strict';
  var root = document.documentElement;

  // roaming spotlight
  var raf;
  window.addEventListener('pointermove', function (e) {
    if (raf) return;
    raf = requestAnimationFrame(function () {
      root.style.setProperty('--mx', e.clientX + 'px');
      root.style.setProperty('--my', e.clientY + 'px');
      document.body.style.setProperty('--mx', e.clientX + 'px');
      document.body.style.setProperty('--my', e.clientY + 'px');
      raf = null;
    });
  }, { passive: true });

  // scroll reveal
  var targets = document.querySelectorAll('.stub-wrap, .bento-card, .venue-card, .section-head, .foot-cta, .panel, .kpi');
  if ('IntersectionObserver' in window) {
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (en) { if (en.isIntersecting) { en.target.classList.add('in'); io.unobserve(en.target); } });
    }, { threshold: 0.08 });
    targets.forEach(function (t, i) { t.classList.add('rv'); t.style.transitionDelay = Math.min(i % 6, 5) * 60 + 'ms'; io.observe(t); });
  }

  // hero: a fan of seats that keeps "booking" itself
  var fan = document.getElementById('fan');
  if (fan) {
    var NS = 'http://www.w3.org/2000/svg', cx = 300, cy = 410, seats = [];
    var rings = 10, r0 = 96, step = 30;
    for (var i = 0; i < rings; i++) {
      var r = r0 + i * step, span = 150 * Math.PI / 180;
      var n = Math.floor((r * span) / 19);
      var color = i < 3 ? '#ffb627' : i < 6 ? '#8a6bff' : '#38e8b4';
      for (var k = 0; k < n; k++) {
        var a = -span / 2 + (span * k) / (n - 1) - Math.PI / 2;
        var c = document.createElementNS(NS, 'circle');
        c.setAttribute('cx', (cx + r * Math.cos(a)).toFixed(1));
        c.setAttribute('cy', (cy + r * Math.sin(a)).toFixed(1));
        c.setAttribute('r', 6.2);
        c.dataset.color = color;
        var taken = Math.random() < 0.55;
        paint(c, taken);
        fan.appendChild(c);
        seats.push(c);
      }
    }
    var stage = document.createElementNS(NS, 'path');
    stage.setAttribute('d', 'M210 412 A 90 38 0 0 1 390 412 Z');
    stage.setAttribute('fill', 'rgba(255,182,39,.55)');
    fan.appendChild(stage);
    var label = document.createElementNS(NS, 'text');
    label.setAttribute('x', cx); label.setAttribute('y', 404); label.setAttribute('text-anchor', 'middle');
    label.setAttribute('fill', '#ffe7b0'); label.setAttribute('font-size', '10');
    label.setAttribute('letter-spacing', '5'); label.setAttribute('font-family', 'JetBrains Mono, monospace');
    label.textContent = 'STAGE';
    fan.appendChild(label);

    if (!window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      setInterval(function () {
        for (var j = 0; j < 3; j++) {
          var s = seats[Math.floor(Math.random() * seats.length)];
          paint(s, s.dataset.taken !== '1');
        }
      }, 220);
    }
  }
  function paint(c, taken) {
    c.dataset.taken = taken ? '1' : '0';
    if (taken) { c.setAttribute('fill', c.dataset.color); c.setAttribute('opacity', '1'); }
    else { c.setAttribute('fill', '#241f31'); c.setAttribute('opacity', '.9'); }
  }

  // feature-card mini seat grid
  var mini = document.querySelector('.mini-seats');
  if (mini) {
    var cls = ['a', 'v', 'm'];
    for (var m = 0; m < 78; m++) {
      var i2 = document.createElement('i');
      if (Math.random() < .5) i2.className = cls[Math.floor(m / 26)];
      mini.appendChild(i2);
    }
    setInterval(function () {
      var el = mini.children[Math.floor(Math.random() * mini.children.length)];
      var idx = Array.prototype.indexOf.call(mini.children, el);
      el.className = el.className ? '' : cls[Math.floor(idx / 26)];
    }, 260);
  }
})();
