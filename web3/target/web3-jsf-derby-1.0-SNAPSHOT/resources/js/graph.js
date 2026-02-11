function bySuffix(suffix) {
    return document.querySelector(`[id$=":${suffix}"]`) || document.getElementById(suffix);
}

function formatInputTo4Digits(evt) {
    let v = String(evt.target.value);
    v = v.replace(',', '.').replace(/[^0-9.\-]/g, '');

    // минус только в начале
    v = v.replace(/(?!^)-/g, '');

    // только одна точка
    const firstDot = v.indexOf('.');
    if (firstDot !== -1) {
        const left = v.substring(0, firstDot + 1);
        const right = v.substring(firstDot + 1).replace(/\./g, '');
        v = left + right;
    }

    if (v.includes('.')) {
        const parts = v.split('.');
        v = parts[0] + '.' + parts[1].substring(0, 4);
    }

    evt.target.value = v;
}

function getRElement() {
    // p:selectOneMenu хранит значение в скрытом input с суффиксом _input
    return bySuffix("r_input") || bySuffix("r");
}

function getR() {
    const el = getRElement();
    if (!el) return NaN;
    return Number(String(el.value).replace(',', '.'));
}

function getY() {
    const el = bySuffix("y");
    if (!el) return NaN;
    return Number(String(el.value).replace(',', '.'));
}

function getX() {
    const el = bySuffix("x");
    if (!el) return NaN;
    return Number(String(el.value).replace(',', '.'));
}

// Раньше здесь была client-side валидация с alert().
// По требованию — убираем любые всплывающие окна/alerts.
// Валидация остаётся на стороне JSF (и на сервере для клика по canvas).

function getPointsFromHidden() {
    const el = bySuffix("pointsJson");
    if (!el || !el.value) return [];
    try {
        const arr = JSON.parse(el.value);
        if (!Array.isArray(arr)) return [];
        return arr.map(p => ({
            x: Number(p.x),
            y: Number(p.y),
            hit: !!p.hit
        })).filter(p => Number.isFinite(p.x) && Number.isFinite(p.y));
    } catch (e) {
        console.error("Bad pointsJson", e);
        return [];
    }
}

// Фиксируем масштаб (пиксели на 1 единицу), чтобы
// при смене R менялась только область, а точки НЕ «прыгали».
// Оси рисуем в фиксированном диапазоне.
const AXIS_MAX = 5;
const PLOT_RADIUS_PX = 170;

function scalePxPerUnit() {
    return PLOT_RADIUS_PX / AXIS_MAX;
}

function fmtTick(n) {
    // Печатаем числа (как есть)
    if (!Number.isFinite(n)) return "";
    const rounded = Math.round(n);
    if (Math.abs(n - rounded) < 1e-9) return String(rounded);
    return String(Number(n.toFixed(2))).replace(/\.?0+$/, "");
}

/*
  Новая область (как в задании):
  1 четверть — прямоугольник: x ∈ [0..R], y ∈ [0..R/2]
  2 четверть — сектор круга радиуса R, центр (0,0) (берём четверть круга во II четверти)
  3 четверть — пусто
  4 четверть — прямоугольный треугольник: (0,0), (R,0), (0,-R/2)
*/
function drawArea(r) {
    const canvas = document.getElementById("myCanvas");
    if (!canvas) return;
    const ctx = canvas.getContext("2d");

    const w = canvas.width, h = canvas.height;
    const cx = w / 2, cy = h / 2;

    ctx.clearRect(0, 0, w, h);

    if (!Number.isFinite(r) || r <= 0) return;

    const k = scalePxPerUnit();

    // --- Filled area ---
    ctx.fillStyle = "rgba(0, 120, 255, 0.55)";

    // Q1 rectangle: x [0..R], y [0..R/2]
    ctx.beginPath();
    ctx.rect(cx, cy - (r / 2) * k, r * k, (r / 2) * k);
    ctx.closePath();
    ctx.fill();

    // Q2 quarter-circle sector radius R (upper-left in math coords)
    // В пиксельных углах: left = PI, up = 3PI/2 (т.к. ось Y вниз).
    ctx.beginPath();
    ctx.moveTo(cx, cy);
    ctx.arc(cx, cy, r * k, Math.PI, 1.5 * Math.PI, false);
    ctx.closePath();
    ctx.fill();

    // Q4 right triangle: (0,0), (R,0), (0,-R/2)
    ctx.beginPath();
    ctx.moveTo(cx, cy);
    ctx.lineTo(cx + r * k, cy);
    ctx.lineTo(cx, cy + (r / 2) * k);
    ctx.closePath();
    ctx.fill();

    // --- Axes ---
    ctx.strokeStyle = "#000";
    ctx.lineWidth = 2;

    ctx.beginPath();
    ctx.moveTo(0, cy); ctx.lineTo(w, cy);
    ctx.moveTo(cx, 0); ctx.lineTo(cx, h);
    ctx.stroke();

    // --- Ticks & labels (фиксированная шкала) ---
    ctx.fillStyle = "#000";
    ctx.font = "12px Arial";
    ctx.lineWidth = 1.5;

    function tickX(val) {
        const px = cx + val * k;
        ctx.beginPath();
        ctx.moveTo(px, cy - 5);
        ctx.lineTo(px, cy + 5);
        ctx.stroke();

        if (val !== 0) {
            const label = fmtTick(val);
            ctx.fillText(label, px - (label.length * 3), cy + 18);
        }
    }

    function tickY(val) {
        const py = cy - val * k;
        ctx.beginPath();
        ctx.moveTo(cx - 5, py);
        ctx.lineTo(cx + 5, py);
        ctx.stroke();

        if (val !== 0) {
            const label = fmtTick(val);
            ctx.fillText(label, cx + 8, py + 4);
        }
    }

    for (let i = -AXIS_MAX; i <= AXIS_MAX; i += 1) {
        tickX(i);
        tickY(i);
    }

    // рамка canvas (чтобы выглядело как на примере)
    ctx.lineWidth = 1;
    ctx.strokeRect(0.5, 0.5, w - 1, h - 1);
}

function plotPoint(x, y, hit) {
    const canvas = document.getElementById("myCanvas");
    if (!canvas) return;
    const ctx = canvas.getContext("2d");

    const k = scalePxPerUnit();
    const cx = canvas.width / 2;
    const cy = canvas.height / 2;

    const px = cx + x * k;
    const py = cy - y * k;

    ctx.beginPath();
    ctx.arc(px, py, 4, 0, 2 * Math.PI);
    ctx.fillStyle = hit ? "green" : "red";
    ctx.fill();
}

function drawGraph() {
    const r = getR();
    const rr = (Number.isFinite(r) && r > 0) ? r : 2;

    drawArea(rr);

    const points = getPointsFromHidden();
    for (const p of points) {
        plotPoint(p.x, p.y, p.hit);
    }
}

function canvasToCoords(evt) {
    const canvas = document.getElementById("myCanvas");
    const rect = canvas.getBoundingClientRect();

    const cx = canvas.width / 2;
    const cy = canvas.height / 2;
    const k = scalePxPerUnit();

    const px = evt.clientX - rect.left;
    const py = evt.clientY - rect.top;

    const x = (px - cx) / k;
    const y = (cy - py) / k;

    return { x, y };
}

document.addEventListener("DOMContentLoaded", () => {
    drawGraph();

    const yEl = bySuffix("y");
    if (yEl) yEl.addEventListener("input", formatInputTo4Digits);

    // Перерисовка при смене R (p:selectOneMenu)
    const rEl = getRElement();
    if (rEl) rEl.addEventListener("change", () => drawGraph());

    // На всякий случай — перерисовка при движении X-слайдера
    const xEl = bySuffix("x");
    if (xEl) xEl.addEventListener("change", () => drawGraph());

    const canvas = document.getElementById("myCanvas");
    if (canvas) {
        canvas.addEventListener("click", (evt) => {
            // По требованию: никаких alert()/попапов.
            // И даём кликать в ЛЮБУЮ точку на canvas.
            // Если R вдруг не распарсился, подставим ближайшее допустимое.
            let r = getR();
            const allowedR = [1, 1.5, 2, 2.5, 3];
            if (!Number.isFinite(r)) {
                r = 2;
            }
            if (!allowedR.includes(r)) {
                let best = allowedR[0];
                let bestD = Math.abs(r - best);
                for (const a of allowedR) {
                    const d = Math.abs(r - a);
                    if (d < bestD) {
                        bestD = d;
                        best = a;
                    }
                }
                r = best;
            }

            const { x, y } = canvasToCoords(evt);

            if (typeof sendPoint === "function") {
                sendPoint([
                    { name: "x", value: x.toFixed(4) },
                    { name: "y", value: y.toFixed(4) },
                    { name: "r", value: r.toFixed(4) }
                ]);
            } else {
                // Не мешаем пользователю попапами — просто пишем в консоль.
                console.error("sendPoint() не найден (нет p:remoteCommand на странице)");
            }
        });
    }
});

function afterAjaxUpdate() {
    drawGraph();
}
