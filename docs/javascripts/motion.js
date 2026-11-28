/*
 * Motion for the docs site. Pairs with stylesheets/motion.css.
 * Zensical swaps pages in place (navigation.instant) and emits on the global
 * document$ after each swap, so per-page setup runs from there.
 */
(function () {
  "use strict";

  var root = document.documentElement;
  var reduceQuery = window.matchMedia("(prefers-reduced-motion: reduce)");
  var reduced = function () { return reduceQuery.matches; };

  var EMPHASIZED = "cubic-bezier(.2, 0, 0, 1)";
  var DECELERATE = "cubic-bezier(.05, .7, .1, 1)";

  function visible(el) {
    return el && el.getClientRects().length > 0;
  }

  function samePage(url) {
    return url.origin === location.origin && url.pathname === location.pathname;
  }

  /* ---------- A + C: page leave and enter ---------- */

  function enterPage() {
    var content = document.querySelector(".md-content");
    if (!content) return;
    content.classList.remove("m-leaving");
    if (reduced()) return;
    var inner = content.querySelector(".md-content__inner");
    if (inner) {
      Array.prototype.forEach.call(inner.children, function (el, i) {
        el.style.setProperty("--m-i", Math.min(i, 10));
      });
    }
    content.classList.remove("m-entering");
    void content.offsetWidth;
    content.classList.add("m-entering");
    setTimeout(function () { content.classList.remove("m-entering"); }, 1000);
  }

  function leavePage() {
    var content = document.querySelector(".md-content");
    if (!content || reduced()) return;
    content.classList.add("m-leaving");
    // If the page never swaps (load error, cancelled), don't leave it blank.
    setTimeout(function () { content.classList.remove("m-leaving"); }, 1500);
  }

  /* ---------- B: sliding nav highlight ---------- */

  var lastPill = null; // where the pill was before the page swapped

  function activePrimaryLink(nav) {
    var links = nav.querySelectorAll(".md-nav__link--active");
    for (var i = 0; i < links.length; i++) {
      if (visible(links[i]) && !links[i].closest(".md-nav--secondary")) return links[i];
    }
    return null;
  }

  function pillBox(nav, link) {
    var n = nav.getBoundingClientRect(), r = link.getBoundingClientRect();
    return { x: r.left - n.left, y: r.top - n.top, w: r.width, h: r.height };
  }

  function placePill(pill, box) {
    pill.style.transform = "translate(" + box.x + "px," + box.y + "px)";
    pill.style.width = box.w + "px";
    pill.style.height = box.h + "px";
  }

  function setupPill() {
    var nav = document.querySelector(".md-sidebar--primary .md-nav--primary");
    if (!nav) return;
    var link = activePrimaryLink(nav);
    if (!link) { root.classList.remove("m-has-pill"); return; }
    var pill = document.createElement("div");
    pill.className = "m-pill";
    nav.insertBefore(pill, nav.firstChild);
    root.classList.add("m-has-pill");
    var target = pillBox(nav, link);
    if (lastPill && !reduced()) {
      placePill(pill, lastPill);
      void pill.offsetWidth;
      pill.classList.add("m-go");
    }
    placePill(pill, target);
    lastPill = target;
  }

  /* ---------- F: "On this page" marker ---------- */

  function setupTocBar() {
    var list = document.querySelector(".md-sidebar--secondary .md-nav--secondary > .md-nav__list");
    if (!list) { root.classList.remove("m-has-bar"); return; }
    var bar = document.createElement("div");
    bar.className = "m-toc-bar";
    list.appendChild(bar);
    root.classList.add("m-has-bar");
    var place = function () {
      var a = list.querySelector(".md-nav__link--active");
      if (!a || !visible(a)) { bar.style.opacity = "0"; return; }
      var l = list.getBoundingClientRect(), r = a.getBoundingClientRect();
      bar.style.opacity = "1";
      bar.style.transform = "translateY(" + (r.top - l.top) + "px)";
      bar.style.height = r.height + "px";
    };
    new MutationObserver(place).observe(list, { attributes: true, subtree: true, attributeFilter: ["class"] });
    place();
  }

  /* ---------- I: glide to a section on the same page ---------- */

  function glow(el) {
    el.classList.remove("m-glow");
    void el.offsetWidth;
    el.classList.add("m-glow");
    setTimeout(function () { el.classList.remove("m-glow"); }, 1400);
  }

  document.addEventListener("click", function (e) {
    if (e.defaultPrevented || e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return;
    var a = e.target.closest("a[href]");
    if (!a || a.target === "_blank") return;
    var url = new URL(a.href, location.href);

    if (samePage(url) && url.hash) {
      var el = document.getElementById(decodeURIComponent(url.hash.slice(1)));
      if (!el) return;
      e.preventDefault();
      e.stopPropagation();
      history.pushState(history.state, "", url.href);
      if (reduced()) { el.scrollIntoView(); glow(el); return; }
      var done = false;
      var finish = function () {
        if (done) return;
        done = true;
        window.removeEventListener("scrollend", finish);
        glow(el);
      };
      window.addEventListener("scrollend", finish);
      setTimeout(finish, 1000);
      el.scrollIntoView({ behavior: "smooth", block: "start" });
      return;
    }

    /* A: an internal link to another page starts the fade-out */
    if (url.origin === location.origin && !samePage(url) && !a.hasAttribute("download")) leavePage();
  }, true);

  /* ---------- D: theme switch spreads in a circle from the button ---------- */

  document.addEventListener("click", function (e) {
    var label = e.target.closest('label.md-header__button[for^="__palette_"]');
    if (!label || reduced() || !document.startViewTransition) return;
    var input = document.getElementById(label.htmlFor);
    if (!input) return;
    e.preventDefault();
    e.stopPropagation();
    var r = label.getBoundingClientRect();
    var x = r.left + r.width / 2, y = r.top + r.height / 2;
    var end = Math.hypot(Math.max(x, innerWidth - x), Math.max(y, innerHeight - y));
    root.classList.toggle("m-turn");
    var t = document.startViewTransition(function () { input.click(); });
    t.ready.then(function () {
      root.animate(
        { clipPath: ["circle(0px at " + x + "px " + y + "px)", "circle(" + end + "px at " + x + "px " + y + "px)"] },
        { duration: 550, easing: EMPHASIZED, pseudoElement: "::view-transition-new(root)" }
      );
    }).catch(function () {});
  }, true);

  /* ---------- E: search dialog opens and results drop in ---------- */

  var watched = new WeakSet();

  function animateResult(li, i) {
    li.animate(
      [{ opacity: 0, transform: "translateY(10px)" }, { opacity: 1, transform: "none" }],
      { duration: 260, delay: Math.min(i, 8) * 30, easing: DECELERATE, fill: "backwards" }
    );
  }

  function watchSearch() {
    if (reduced()) return;
    var hosts = document.querySelectorAll("*");
    for (var i = 0; i < hosts.length; i++) {
      var sr = hosts[i].shadowRoot;
      var dialog = sr && sr.querySelector("dialog");
      if (!dialog) continue;
      if (dialog.open) {
        dialog.animate(
          [{ opacity: 0, transform: "translateY(-8px) scale(.97)" }, { opacity: 1, transform: "none" }],
          { duration: 240, easing: DECELERATE }
        );
      }
      if (!watched.has(dialog)) {
        watched.add(dialog);
        new MutationObserver(function (records) {
          var n = 0;
          records.forEach(function (rec) {
            rec.addedNodes.forEach(function (node) {
              if (node.nodeType === 1 && node.tagName === "LI" && node.parentElement && node.parentElement.tagName === "OL") {
                animateResult(node, n++);
              }
            });
          });
        }).observe(dialog, { childList: true, subtree: true });
      }
      return;
    }
  }

  document.addEventListener("click", function (e) {
    if (e.target.closest(".md-search__button")) setTimeout(watchSearch, 0);
  });
  document.addEventListener("keydown", function (e) {
    var typing = e.target.closest && e.target.closest("input, textarea, [contenteditable]");
    if ((e.key === "k" && (e.metaKey || e.ctrlKey)) || (!typing && (e.key === "/" || e.key === "s" || e.key === "f"))) {
      setTimeout(watchSearch, 0);
    }
  });

  /* ---------- Per-page setup ---------- */

  function onPage() {
    enterPage();
    setupPill();
    setupTocBar();
  }

  if (window.document$ && typeof window.document$.subscribe === "function") {
    window.document$.subscribe(onPage);
  } else {
    document.addEventListener("DOMContentLoaded", onPage);
  }
  window.addEventListener("resize", function () {
    lastPill = null;
    var nav = document.querySelector(".md-sidebar--primary .md-nav--primary");
    var pill = nav && nav.querySelector(".m-pill");
    var link = nav && activePrimaryLink(nav);
    if (pill && link) { pill.classList.remove("m-go"); placePill(pill, (lastPill = pillBox(nav, link))); }
  });
})();
