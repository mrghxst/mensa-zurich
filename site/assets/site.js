// Mensa Zürich website: theme, entrances, the live ETH menu, open-now dots and the Zürich clock.
(() => {
  "use strict";
  const root = document.documentElement;
  const $ = (s, el = document) => el.querySelector(s);
  const $$ = (s, el = document) => [...el.querySelectorAll(s)];

  const reduced = matchMedia("(prefers-reduced-motion: reduce)").matches;
  // The hero video is a moving picture; people who asked for less motion get its first frame.
  if (reduced) $$("video[data-tour]").forEach((v) => { v.removeAttribute("autoplay"); v.pause(); });

  /* ---------- Theme: dark by default, light on request ---------- */
  const toggle = $("[data-theme-toggle]");
  function applyTheme(theme) {
    if (theme === "light") root.dataset.theme = "light"; else delete root.dataset.theme;
    const light = theme === "light";
    $$("img[data-shot]").forEach((img) => { img.src = `${img.src.replace(/[^/]*$/, "")}${img.dataset.shot}-${light ? "light" : "dark"}.webp`; });
    $$("video[data-tour]").forEach((v) => {
      const theme = light ? "light" : "dark";
      if (v.src.endsWith(`tour-${theme}.mp4`)) return;
      v.poster = v.poster.replace(/list-(dark|light)/, `list-${theme}`);
      v.src = v.src.replace(/tour-(dark|light)/, `tour-${theme}`);
      if (!reduced) v.play().catch(() => {});
    });
    $('meta[name="theme-color"]')?.setAttribute("content", light ? "#fafafa" : "#0a0a0a");
    toggle?.setAttribute("aria-label", light ? "Switch to dark mode" : "Switch to light mode");
  }
  if (root.dataset.theme === "light") applyTheme("light");
  toggle?.addEventListener("click", () => {
    const next = root.dataset.theme === "light" ? "dark" : "light";
    try { localStorage.setItem("theme", next); } catch (e) {}
    applyTheme(next);
  });

  /* ---------- Entrances ---------- */
  const reveals = $$(".reveal");
  if ("IntersectionObserver" in window) {
    const io = new IntersectionObserver((entries) => {
      entries.forEach((e) => { if (e.isIntersecting) { e.target.classList.add("in"); io.unobserve(e.target); } });
    }, { rootMargin: "0px 0px -12% 0px" });
    reveals.forEach((el) => io.observe(el));
  } else {
    reveals.forEach((el) => el.classList.add("in"));
  }

  /* ---------- Zürich time ---------- */
  function zurich(date = new Date()) {
    const parts = Object.fromEntries(new Intl.DateTimeFormat("en-GB", {
      timeZone: "Europe/Zurich", year: "numeric", month: "2-digit", day: "2-digit",
      hour: "2-digit", minute: "2-digit", hourCycle: "h23", weekday: "short",
    }).formatToParts(date).map((p) => [p.type, p.value]));
    const weekday = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"].indexOf(parts.weekday) + 1;
    return { iso: `${parts.year}-${parts.month}-${parts.day}`, weekday, minutes: +parts.hour * 60 + +parts.minute, hhmm: `${parts.hour}:${parts.minute}` };
  }
  const toMinutes = (t) => { const [h, m] = t.split(":").map(Number); return h * 60 + m; };
  function addDays(iso, days) {
    const d = new Date(`${iso}T12:00:00Z`);
    d.setUTCDate(d.getUTCDate() + days);
    return d.toISOString().slice(0, 10);
  }
  const longDate = (iso) => new Date(`${iso}T12:00:00Z`).toLocaleDateString("en-GB", { weekday: "long", day: "numeric", month: "long", timeZone: "UTC" });

  function tickClock() {
    const now = zurich();
    const clock = $("[data-clock]");
    if (clock) clock.textContent = now.hhmm;
    const note = $("[data-clock-note]");
    if (note) {
      const weekday = now.weekday <= 5;
      note.textContent = weekday && now.minutes >= 660 && now.minutes < 840 ? "Lunch is being served" : "Lunch from 11:00 on weekdays";
    }
    updateOpenDots(now);
  }

  /* ---------- Open-now dots in the mensa list ---------- */
  const ethHours = {}; // facility id -> [[from, to]] for today, from the live data
  function updateOpenDots(now) {
    let open = 0;
    $$(".place").forEach((li) => {
      let ranges = [];
      if (li.dataset.eth) ranges = ethHours[li.dataset.eth] || [];
      else if (li.dataset.hours && now.weekday <= 5) ranges = [li.dataset.hours.split("-")];
      const isOpen = ranges.some(([from, to]) => now.minutes >= toMinutes(from) && now.minutes < toMinutes(to));
      li.querySelector(".dot")?.classList.toggle("is-open", isOpen);
      if (isOpen) open++;
    });
    const label = $("[data-open-count]");
    if (label) label.textContent = open ? `${open} ${open === 1 ? "is" : "are"} open right now` : "All closed right now. Lunch starts around 11:00 on weekdays.";
  }

  /* ---------- The live ETH menu ---------- */
  const ETH_NAMES = {
    14: "Alumni quattro Lounge", 8: "Archimedes", 15: "Bellavista", 1: "bQm", 2: "CafeBar", 3: "Clausiusbar",
    5: "Dozentenfoyer", 6: "Einstein & Zweistein", 12: "Eureka Take Away", 28: "Flavour Kitchen (Basel)", 7: "food&lab",
    19: "food market", 20: "FUSION meal", 4: "Kiosk CLA", 16: "Mendokoro", 9: "Mensa Polyterrasse", 23: "Octavo",
    10: "Polysnack", 22: "Rice Up!", 27: "Science Lounge (Basel)", 11: "Tannenbar", 13: "Zwei Grad Bistro",
  };
  const ALLERGENS = [
    ["peanuts", /peanut/], ["gluten", /gluten|wheat/], ["crustaceans", /crustacean/], ["eggs", /egg/], ["fish", /fish/],
    ["soy", /soy/], ["milk", /milk|lactose/], ["nuts", /(^|[^a-z])nuts?\b/], ["celery", /celery/], ["mustard", /mustard/],
    ["sesame", /sesame/], ["sulphites", /sulfite|sulphite/], ["lupin", /lupin/], ["molluscs", /mollusc/],
  ];
  const allergenKeys = (list) => {
    const keys = new Set();
    (list || []).forEach((a) => { const d = String(a.desc || "").toLowerCase(); ALLERGENS.forEach(([k, re]) => { if (re.test(d)) keys.add(k); }); });
    return [...keys];
  };
  const tidy = (s) => String(s || "").replace(/\s*\n\s*/g, " ").replace(/\s*\|\s*/g, ", ").replace(/\s+/g, " ").trim();
  const lineName = (s) => {
    const t = tidy(s);
    if (/^tagessuppe$/i.test(t)) return "Soup of the day";
    return t === t.toUpperCase() ? t.charAt(0) + t.slice(1).toLowerCase() : t;
  };
  const escapeHtml = (s) => String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

  const demo = { lunch: {}, target: null, isToday: true, loaded: false };
  const list = $("[data-demo-list]");
  const controls = $("[data-demo-controls]");
  const select = $("[data-c-mensa]");

  function tier() { return +(controls?.querySelector('input[name="tier"]:checked')?.value || 0); }
  function avoided() { return $$('fieldset input[type="checkbox"]:checked', controls).map((i) => i.value); }

  function applyFilters() {
    if (!list) return;
    const t = tier();
    const avoid = avoided();
    const hiddenBy = new Set();
    let hidden = 0;
    $$(".meal", list).forEach((li) => {
      const prices = (li.dataset.prices || "").split(",");
      const price = prices[t] || prices[0];
      const priceEl = li.querySelector(".meal-price");
      if (priceEl && price) priceEl.textContent = `CHF ${Number(price).toFixed(2)}${li.dataset.unit ? ` ${li.dataset.unit}` : ""}`;
      const has = (li.dataset.allergens || "").split(",").filter((a) => avoid.includes(a));
      li.classList.toggle("is-hidden", has.length > 0);
      if (has.length) { hidden++; has.forEach((a) => hiddenBy.add(a)); }
    });
    const note = $("[data-demo-note]");
    if (!note) return;
    const base = demo.loaded ? "" : "Menu of Monday, 2 November 2026. ";
    if (hidden) {
      const total = $$(".meal", list).length;
      note.textContent = `${base}${hidden === total ? "Every dish" : hidden === 1 ? "1 dish" : `${hidden} dishes`} hidden because of ${[...hiddenBy].join(", ")}.`;
    } else {
      note.textContent = demo.loaded ? "" : "Menu of Monday, 2 November 2026.";
    }
  }

  function renderMensa(id) {
    const entry = demo.lunch[id];
    if (!entry || !list) return;
    $("[data-demo-mensa]").textContent = ETH_NAMES[id] || "ETH mensa";
    const day = longDate(demo.target);
    $("[data-demo-when]").textContent = `${entry.label}, ${day}, ${entry.from}–${entry.to}`;
    list.innerHTML = entry.meals.map((m) => `
      <li class="meal enter" data-allergens="${m.allergens.join(",")}" data-prices="${m.prices.join(",")}"${m.unit ? ` data-unit="${escapeHtml(m.unit)}"` : ""}>
        <div><div class="meal-top"><span class="meal-line">${escapeHtml(m.line)}</span><span class="meal-price"></span></div>
        <p class="meal-name">${escapeHtml(m.name)}</p>${m.desc ? `<p class="meal-desc">${escapeHtml(m.desc)}</p>` : ""}
        ${m.allergens.length ? `<p class="meal-desc">Contains ${m.allergens.join(", ")}</p>` : ""}</div>
        ${m.image ? `<img src="${escapeHtml(m.image)}" width="72" height="72" alt="${escapeHtml(m.name)}" loading="lazy">` : "<span></span>"}
      </li>`).join("");
    $$(".meal", list).forEach((li, i) => { li.style.animationDelay = `${i * 50}ms`; });
    applyFilters();
  }

  function parseRotas(json, target, weekday, isToday) {
    const lunch = {};
    for (const rota of json["weekly-rota-array"] || []) {
      const id = rota["facility-id"];
      if (!id || rota["valid-from"] > target || (rota["valid-to"] && rota["valid-to"] < target)) continue;
      const day = (rota["day-of-week-array"] || []).find((d) => d["day-of-week-code"] === weekday);
      for (const hours of day?.["opening-hour-array"] || []) {
        for (const mt of hours["meal-time-array"] || []) {
          const from = mt["time-from"], to = mt["time-to"];
          if (!from || !to) continue;
          if (isToday) (ethHours[id] = ethHours[id] || []).push([from, to]);
          const isLunch = /lunch|mittag/i.test(mt.name || "") || toMinutes(from) < 15 * 60;
          if (!isLunch || lunch[id]) continue;
          const meals = (mt["line-array"] || []).map((line) => {
            const meal = line.meal;
            if (!meal || !meal.name) return null;
            const prices = (meal["meal-price-array"] || []).slice(0, 3).map((p) => Number(p.price).toFixed(2));
            if (!prices.length) return null;
            while (prices.length < 3) prices.push(prices[prices.length - 1]);
            const unit = meal["price-unit-desc"] && meal["price-unit-desc"] !== "serving" ? `per ${meal["price-unit-desc"]}` : "";
            return {
              line: lineName(line.name || meal.name), name: tidy(meal.name), desc: tidy(meal.description),
              prices, unit, allergens: allergenKeys(meal["allergen-array"]),
              image: meal["image-url"] ? `${meal["image-url"]}?client-id=ethz-wcms` : "",
            };
          }).filter(Boolean);
          if (meals.length) lunch[id] = { label: mt.name ? tidy(mt.name) : "Lunch", from, to, meals };
        }
      }
    }
    return lunch;
  }

  async function loadMenu() {
    const now = zurich();
    // On weekends the next Monday's menu is more useful than an empty day.
    const target = now.weekday >= 6 ? addDays(now.iso, 8 - now.weekday) : now.iso;
    const weekday = now.weekday >= 6 ? 1 : now.weekday;
    const monday = addDays(target, 1 - weekday);
    const url = "https://idapps.ethz.ch/cookpit-pub-services/v1/weeklyrotas?client-id=ethz-wcms&lang=en&rs-first=0&rs-size=50"
      + `&valid-after=${monday}&valid-before=${addDays(monday, 7)}`;
    try {
      const res = await fetch(url, { headers: { Accept: "application/json" } });
      if (!res.ok) throw new Error(res.status);
      const lunch = parseRotas(await res.json(), target, weekday, target === now.iso);
      Object.values(ethHours).forEach((r) => r.sort((a, b) => a[0].localeCompare(b[0])));
      updateOpenDots(zurich());
      $$(".place[data-eth]").forEach((li) => {
        const r = ethHours[li.dataset.eth];
        const hours = li.querySelector(".place-hours");
        if (r && r.length && hours) hours.textContent = `${r[0][0]}–${r[0][1]}`;
      });
      const ids = Object.keys(lunch).sort((a, b) => (ETH_NAMES[a] || "").localeCompare(ETH_NAMES[b] || ""));
      if (!ids.length) {
        $("[data-demo-note]").textContent = "The mensas haven't published a menu for today. This is the menu of Monday, 2 November 2026.";
        return;
      }
      demo.lunch = lunch; demo.target = target; demo.isToday = target === now.iso; demo.loaded = true;
      select.innerHTML = ids.map((id) => `<option value="${id}">${escapeHtml(ETH_NAMES[id] || `ETH mensa ${id}`)}</option>`).join("");
      const preferred = ids.includes("9") ? "9" : ids[0];
      select.value = preferred;
      const title = $("[data-today-title]");
      if (title) title.textContent = demo.isToday ? "Today's lunch at ETH" : `${longDate(target).split(" ")[0]}'s lunch at ETH`;
      renderMensa(preferred);
    } catch (e) {
      const note = $("[data-demo-note]");
      if (note) note.textContent = "Today's menu couldn't be loaded. This is the menu of Monday, 2 November 2026.";
    }
  }

  controls?.addEventListener("change", (e) => {
    if (e.target === select) renderMensa(select.value); else applyFilters();
  });
  controls?.addEventListener("submit", (e) => e.preventDefault());
  $("[data-more]")?.addEventListener("click", () => controls.classList.add("show-all"));

  tickClock();
  setInterval(tickClock, 15000);

  const today = $("#today");
  if (today && "IntersectionObserver" in window) {
    const io = new IntersectionObserver((entries) => {
      if (entries.some((e) => e.isIntersecting)) { io.disconnect(); loadMenu(); }
    }, { rootMargin: "800px 0px" });
    io.observe(today);
  } else if (today) {
    loadMenu();
  }
})();
