// Stylesheet for the admin console, served at /admin/app.css.
export const adminStyles = String.raw`
:root {
  --paper: #f4f2ec;
  --surface: #fffefb;
  --surface-2: #f8f5ee;
  --ink: #1d1d1f;
  --ink-2: #45454a;
  --muted: #6e6e73;
  --line: #e1dccf;
  --line-2: #ece8dd;
  --brass: #9a6b2f;
  --brass-2: #7e561f;
  --brass-soft: #f4ead8;
  --amber-ink: #8a6a12;
  --amber-soft: #fbf1d3;
  --green-ink: #2f7a5a;
  --green-soft: #e3f1ea;
  --red-ink: #b42318;
  --red-soft: #fbe9e6;
  --stage: #2b2418;
  --stage-2: #3a3124;
  --stage-ink: #efe6d3;
  --font-body: -apple-system, BlinkMacSystemFont, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "Noto Sans CJK SC", "Helvetica Neue", Arial, sans-serif;
  --font-display: "Songti SC", "STSong", "Noto Serif CJK SC", "Source Han Serif SC", "SimSun", Georgia, serif;
  --font-mono: ui-monospace, "SF Mono", Menlo, Consolas, monospace;
  --rail-w: 216px;
  --detail-w: 440px;
}

* { box-sizing: border-box; }
html { height: 100%; }
body {
  margin: 0;
  min-height: 100%;
  background: var(--paper);
  color: var(--ink);
  font-family: var(--font-body);
  font-size: 14px;
  line-height: 1.5;
  -webkit-font-smoothing: antialiased;
  font-variant-numeric: tabular-nums;
}
button, input, select, textarea { font: inherit; color: inherit; }
a { color: var(--brass); text-decoration: none; }
a:hover { text-decoration: underline; }
p { margin: 0; }
h1, h2, h3 { margin: 0; font-weight: 600; }
kbd {
  display: inline-block;
  min-width: 16px;
  padding: 0 4px;
  border: 1px solid var(--line);
  border-radius: 3px;
  background: var(--surface);
  color: var(--muted);
  font-family: var(--font-body);
  font-size: 11px;
  line-height: 16px;
  text-align: center;
  vertical-align: 1px;
}
.hidden { display: none !important; }
.sr-only {
  position: absolute; width: 1px; height: 1px; overflow: hidden;
  clip: rect(0 0 0 0); white-space: nowrap;
}
:focus-visible { outline: 2px solid var(--brass); outline-offset: 2px; }
::selection { background: var(--brass-soft); }

/* ---------- login ---------- */
.login {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
}
.login-card {
  width: min(400px, 100%);
  background: var(--surface);
  border: 1px solid var(--line);
  padding: 32px 32px 28px;
}
.login-brand {
  font-family: var(--font-display);
  font-size: 26px;
  letter-spacing: 0.04em;
  line-height: 1.2;
}
.login-sub { margin-top: 6px; color: var(--muted); font-size: 13px; }
.login-form { margin-top: 24px; display: grid; gap: 14px; }
.login-form label { display: grid; gap: 6px; font-size: 13px; color: var(--ink-2); }
.login-form .row { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.login-remember { display: flex; align-items: center; gap: 6px; font-size: 13px; color: var(--muted); }
.login-message { min-height: 20px; font-size: 13px; color: var(--red-ink); }
.login-foot { margin-top: 22px; font-size: 12.5px; color: var(--muted); line-height: 1.6; }

/* ---------- controls ---------- */
.input, .select, .textarea {
  width: 100%;
  border: 1px solid var(--line);
  border-radius: 4px;
  background: var(--surface);
  padding: 8px 10px;
  color: var(--ink);
}
.input::placeholder, .textarea::placeholder { color: #9a9a9f; }
.input:focus, .select:focus, .textarea:focus { border-color: var(--brass); outline: none; box-shadow: 0 0 0 3px var(--brass-soft); }
.textarea { min-height: 84px; resize: vertical; }
.textarea-short { min-height: 58px; }
.select { appearance: auto; }
.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid var(--line);
  border-radius: 4px;
  background: var(--surface);
  padding: 7px 12px;
  font-size: 13.5px;
  line-height: 1.3;
  color: var(--ink);
  cursor: pointer;
  white-space: nowrap;
}
.btn:hover { background: var(--surface-2); border-color: #cfc8b7; }
.btn:disabled { opacity: 0.5; cursor: default; }
.btn-primary { background: var(--brass); border-color: var(--brass); color: #fff; }
.btn-primary:hover { background: var(--brass-2); border-color: var(--brass-2); }
.btn-good { color: var(--green-ink); border-color: #b8d7c6; }
.btn-good:hover { background: var(--green-soft); }
.btn-danger { color: var(--red-ink); border-color: #e7b7b1; }
.btn-danger:hover { background: var(--red-soft); }
.btn-quiet { border-color: transparent; background: transparent; color: var(--ink-2); }
.btn-quiet:hover { background: var(--surface-2); border-color: transparent; }
.btn-sm { padding: 4px 9px; font-size: 12.5px; }
.btn kbd { margin-left: 2px; }
.check {
  width: 15px; height: 15px; margin: 0;
  accent-color: var(--brass);
  cursor: pointer;
}

/* ---------- shell ---------- */
.shell {
  display: grid;
  grid-template-columns: var(--rail-w) minmax(0, 1fr);
  grid-template-rows: 52px minmax(0, 1fr);
  height: 100vh;
}
.top {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 0 18px 0 20px;
  background: var(--surface);
  border-bottom: 1px solid var(--line);
}
.brand {
  font-family: var(--font-display);
  font-size: 17px;
  letter-spacing: 0.06em;
  color: var(--ink);
  white-space: nowrap;
}
.brand:hover { text-decoration: none; }
.brand small { font-family: var(--font-body); font-size: 12px; color: var(--muted); letter-spacing: 0; margin-left: 8px; }
.global-search { flex: 1; max-width: 460px; position: relative; }
.global-search .input { padding-left: 34px; }
.global-search svg { position: absolute; left: 11px; top: 50%; transform: translateY(-50%); color: var(--muted); }
.top-spacer { flex: 1; }
.who { display: flex; align-items: center; gap: 12px; color: var(--muted); font-size: 13px; white-space: nowrap; }
.who b { color: var(--ink); font-weight: 500; }

.rail {
  border-right: 1px solid var(--line);
  background: var(--surface);
  overflow-y: auto;
  padding: 14px 0 20px;
}
.rail-group { padding: 8px 12px 6px; }
.rail-title {
  padding: 0 10px 6px;
  font-family: var(--font-display);
  font-size: 12.5px;
  letter-spacing: 0.12em;
  color: var(--muted);
}
.rail-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 7px 10px;
  border-radius: 4px;
  color: var(--ink-2);
  font-size: 13.5px;
}
.rail-item:hover { background: var(--surface-2); text-decoration: none; }
.rail-item.active { background: var(--brass-soft); color: var(--ink); }
.rail-item .count { color: var(--muted); font-size: 12.5px; }
.rail-item .count.due { color: var(--amber-ink); font-weight: 600; }
.rail-item.active .count { color: var(--brass-2); }
.rail-foot { margin: 18px 22px 0; font-size: 11.5px; color: var(--muted); line-height: 1.6; }

.work {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  min-height: 0;
  height: 100%;
}
.work.split { grid-template-columns: minmax(0, 1fr) var(--detail-w); }
.pane { min-height: 0; display: flex; flex-direction: column; }
.pane-list { overflow: hidden; }
.pane-head {
  padding: 22px 26px 0;
  display: grid;
  gap: 12px;
}
.pane-title-row { display: flex; align-items: baseline; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
.pane-title { font-family: var(--font-display); font-size: 22px; font-weight: 600; letter-spacing: 0.02em; }
.pane-title small { font-family: var(--font-body); font-size: 13px; font-weight: 400; color: var(--muted); margin-left: 10px; letter-spacing: 0; }
.pane-tools { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.tabs { display: flex; gap: 2px; border-bottom: 1px solid var(--line); overflow-x: auto; }
.tab {
  border: 0; background: none; padding: 8px 12px 9px;
  color: var(--muted); font-size: 13.5px; cursor: pointer; white-space: nowrap;
  border-bottom: 2px solid transparent; margin-bottom: -1px;
}
.tab:hover { color: var(--ink); }
.tab.active { color: var(--ink); border-bottom-color: var(--brass); }
.tab .n { margin-left: 5px; color: var(--muted); font-size: 12px; }
.tab.active .n { color: var(--brass-2); }
.bulk-reason { max-width: 280px; }
.bulkbar {
  display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
  padding: 8px 12px; background: var(--brass-soft); border: 1px solid #e6d7b8; border-radius: 4px; font-size: 13px;
}
.list-scroll { flex: 1; min-height: 0; overflow-y: auto; padding: 6px 26px 40px; }
.rows { list-style: none; margin: 0; padding: 0; }
.row {
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: start;
  padding: 11px 10px 11px 8px;
  border-bottom: 1px solid var(--line-2);
  cursor: pointer;
  border-left: 3px solid transparent;
  margin-left: -11px;
}
.row:hover { background: var(--surface-2); }
.row.selected { background: var(--surface-2); border-left-color: var(--brass); }
.row.done { opacity: 0.55; }
.row-lead { display: flex; justify-content: center; padding-top: 3px; }
.row-dot { margin-top: 5px; }
.row-dot.pending { background: #d9a919; }
.row-dot.good { background: var(--green-ink); }
.row-dot.bad { background: var(--red-ink); }
.row-dot.busy { background: var(--brass); }
.row-main { min-width: 0; }
.row-title { display: flex; align-items: baseline; gap: 8px; min-width: 0; }
.row-title b { font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.row-kind { flex: none; font-size: 12px; color: var(--muted); }
.row-text {
  margin-top: 2px; color: var(--ink-2); font-size: 13.5px;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
  overflow-wrap: anywhere;
}
.row-meta { margin-top: 3px; display: flex; flex-wrap: wrap; gap: 4px 14px; font-size: 12.5px; color: var(--muted); }
.row-side { text-align: right; font-size: 12.5px; color: var(--muted); white-space: nowrap; }
.row-side .status { display: block; margin-top: 3px; }
.dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; background: var(--line); vertical-align: 1px; }
.status { display: inline-flex; align-items: center; gap: 6px; font-size: 12.5px; color: var(--muted); }
.status .dot { background: #b9b3a4; }
.status.pending { color: var(--amber-ink); }
.status.pending .dot { background: #d9a919; }
.status.good { color: var(--green-ink); }
.status.good .dot { background: var(--green-ink); }
.status.bad { color: var(--red-ink); }
.status.bad .dot { background: var(--red-ink); }
.status.busy { color: var(--brass-2); }
.status.busy .dot { background: var(--brass); }
.empty { padding: 48px 8px; color: var(--muted); text-align: center; font-size: 13.5px; }
.empty b { display: block; font-family: var(--font-display); font-size: 18px; color: var(--ink-2); margin-bottom: 6px; font-weight: 500; }
.loading { padding: 40px 8px; color: var(--muted); text-align: center; font-size: 13px; }
.error-box { margin: 14px 0; padding: 12px 14px; background: var(--red-soft); border: 1px solid #e7b7b1; color: var(--red-ink); border-radius: 4px; font-size: 13px; }

/* ---------- detail ---------- */
.pane-detail {
  border-left: 1px solid var(--line);
  background: var(--surface);
  overflow-y: auto;
}
.detail-head {
  position: sticky; top: 0; z-index: 2;
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  padding: 14px 20px; background: var(--surface); border-bottom: 1px solid var(--line-2);
}
.detail-head h2 { font-family: var(--font-display); font-size: 17px; font-weight: 600; }
.detail-body { padding: 18px 20px 28px; display: grid; gap: 20px; }
.section-title { font-size: 12.5px; color: var(--muted); margin-bottom: 8px; }
.meta { display: grid; grid-template-columns: 88px minmax(0, 1fr); gap: 6px 12px; margin: 0; font-size: 13.5px; }
.meta dt { color: var(--muted); }
.meta dd { margin: 0; overflow-wrap: anywhere; }
.meta dd.mono { font-family: var(--font-mono); font-size: 12px; color: var(--ink-2); }
.content-block {
  padding: 12px 14px; background: var(--surface-2); border: 1px solid var(--line-2); border-radius: 4px;
  font-size: 14px; white-space: pre-wrap; overflow-wrap: anywhere; line-height: 1.65;
}
.stage {
  background: var(--stage); border-radius: 6px; color: var(--stage-ink);
  min-height: 160px; display: grid; place-items: center; overflow: hidden;
  border-top: 2px solid var(--brass);
}
.stage img { max-width: 100%; max-height: 62vh; display: block; }
.stage audio { width: 100%; padding: 22px 16px; }
.stage .stage-note { padding: 28px 16px; font-size: 13px; color: #bfb39a; text-align: center; }
.stage pre { margin: 0; padding: 16px; width: 100%; max-height: 50vh; overflow: auto; font-size: 12px; white-space: pre-wrap; }
.thumbs { display: grid; grid-template-columns: repeat(auto-fill, minmax(96px, 1fr)); gap: 8px; }
.thumb {
  aspect-ratio: 1; background: var(--stage); border-radius: 4px; overflow: hidden; border: 0; padding: 0; cursor: pointer;
  display: grid; place-items: center; color: #bfb39a; font-size: 12px;
}
.thumb img { width: 100%; height: 100%; object-fit: cover; display: block; }
.thumb.active { outline: 2px solid var(--brass); outline-offset: 1px; }
.actions { display: flex; flex-wrap: wrap; gap: 8px; }
.actions-note { font-size: 12.5px; color: var(--muted); line-height: 1.6; }
.reason { display: grid; gap: 8px; }
.chips { display: flex; flex-wrap: wrap; gap: 6px; }
.chip {
  border: 1px solid var(--line); background: var(--surface); border-radius: 999px;
  padding: 3px 10px; font-size: 12.5px; color: var(--ink-2); cursor: pointer;
}
.chip:hover { border-color: var(--brass); color: var(--brass-2); }
.chip.active { background: var(--brass-soft); border-color: var(--brass); color: var(--brass-2); }
.keys { display: flex; flex-wrap: wrap; gap: 6px 14px; font-size: 12px; color: var(--muted); }
.avatar {
  width: 56px; height: 56px; border-radius: 50%; background: var(--surface-2); border: 1px solid var(--line);
  overflow: hidden; display: grid; place-items: center; font-family: var(--font-display); font-size: 22px; color: var(--brass-2);
}
.avatar img { width: 100%; height: 100%; object-fit: cover; }
.person { display: flex; align-items: center; gap: 14px; }
.person .name { font-size: 16px; font-weight: 500; }
.person .sub { color: var(--muted); font-size: 12.5px; }
.count-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.count-cell { padding: 10px 12px; background: var(--surface-2); border-radius: 4px; }
.count-cell b { display: block; font-size: 20px; font-weight: 500; line-height: 1.2; }
.count-cell span { font-size: 12px; color: var(--muted); }
.linklist { list-style: none; margin: 0; padding: 0; font-size: 13.5px; display: grid; gap: 4px; }
.linklist li { display: flex; justify-content: space-between; gap: 10px; }
.linklist .t { color: var(--muted); font-size: 12.5px; white-space: nowrap; }
.audit-list { list-style: none; margin: 0; padding: 0; display: grid; gap: 8px; font-size: 13px; }
.audit-list li { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 2px 10px; padding-bottom: 8px; border-bottom: 1px solid var(--line-2); }
.audit-list .t { color: var(--muted); font-size: 12px; white-space: nowrap; }
.audit-list .d { grid-column: 1 / -1; color: var(--muted); font-size: 12.5px; overflow-wrap: anywhere; }
.json { margin: 0; padding: 10px 12px; background: var(--surface-2); border: 1px solid var(--line-2); border-radius: 4px; font-family: var(--font-mono); font-size: 12px; white-space: pre-wrap; overflow-wrap: anywhere; max-height: 40vh; overflow: auto; }
.form { display: grid; gap: 12px; }
.form label { display: grid; gap: 5px; font-size: 13px; color: var(--ink-2); }
.form .inline { display: flex; gap: 10px; align-items: center; }
.confirm-box { padding: 12px 14px; background: var(--red-soft); border: 1px solid #e7b7b1; border-radius: 4px; display: grid; gap: 10px; }
.confirm-box p { font-size: 13px; color: var(--red-ink); line-height: 1.6; }
.progress { height: 6px; background: var(--line-2); border-radius: 3px; overflow: hidden; }
.progress i { display: block; height: 100%; background: var(--brass); }

/* ---------- overview ---------- */
.overview { padding: 22px 26px 48px; overflow-y: auto; display: grid; gap: 28px; align-content: start; }
.ov-head { display: flex; align-items: baseline; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
.ov-head .sub { color: var(--muted); font-size: 13px; }
.ledger { width: 100%; border-collapse: collapse; background: var(--surface); border: 1px solid var(--line); }
.ledger th, .ledger td { padding: 10px 14px; text-align: left; border-bottom: 1px solid var(--line-2); font-size: 13.5px; vertical-align: middle; }
.ledger th { font-weight: 500; color: var(--muted); font-size: 12.5px; background: var(--surface-2); }
.ledger tr:last-child td { border-bottom: 0; }
.ledger td.num, .ledger th.num { text-align: right; width: 90px; }
.ledger td.num b { font-size: 18px; font-weight: 500; }
.ledger td.num.due b { color: var(--amber-ink); }
.ledger td.num.zero b { color: var(--muted); font-weight: 400; }
.ledger td.act { width: 90px; text-align: right; }
.ledger .hint { color: var(--muted); font-size: 12.5px; }
.ov-stack { display: grid; gap: 28px; }
.ov-grid { display: grid; grid-template-columns: minmax(0, 1.4fr) minmax(280px, 0.8fr); gap: 28px; align-items: start; }
.panel { background: var(--surface); border: 1px solid var(--line); padding: 16px 18px 18px; }
.panel-title { font-family: var(--font-display); font-size: 16px; font-weight: 600; margin-bottom: 12px; display: flex; justify-content: space-between; align-items: baseline; gap: 12px; }
.panel-title small { font-family: var(--font-body); font-size: 12.5px; font-weight: 400; color: var(--muted); }
.charts { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18px 24px; }
.chart { display: grid; gap: 6px; }
.chart-head { display: flex; justify-content: space-between; align-items: baseline; font-size: 13px; }
.chart-head b { font-weight: 500; }
.chart-head span { color: var(--muted); font-size: 12.5px; }
.chart svg { width: 100%; height: 64px; display: block; overflow: visible; }
.chart rect.bar { fill: var(--brass); }
.chart rect.bar.today { fill: var(--brass-2); }
.chart rect.bar.hit { fill: var(--ink); }
.chart rect.hit-area { fill: transparent; }
.chart .axis { fill: var(--muted); font-size: 10px; }
.chart .baseline { stroke: var(--line); stroke-width: 1; }
.chart-table { margin-top: 4px; font-size: 12px; color: var(--muted); }
.chart-table summary { cursor: pointer; }
.chart-table table { border-collapse: collapse; margin-top: 6px; }
.chart-table td, .chart-table th { padding: 2px 8px 2px 0; text-align: right; font-weight: 400; }
.chart-table th:first-child, .chart-table td:first-child { text-align: left; }
.tip {
  position: fixed; z-index: 20; pointer-events: none;
  background: var(--ink); color: var(--paper); padding: 5px 8px; border-radius: 3px; font-size: 12px; white-space: nowrap;
}
.services { display: grid; gap: 8px; font-size: 13.5px; }
.services li { display: flex; justify-content: space-between; gap: 12px; padding: 6px 0; border-bottom: 1px solid var(--line-2); }
.services li:last-child { border-bottom: 0; }
.services ul { list-style: none; margin: 0; padding: 0; }
.services .k { color: var(--muted); }
.totals { display: flex; flex-wrap: wrap; gap: 6px 22px; font-size: 13px; color: var(--muted); }
.totals b { color: var(--ink); font-weight: 500; font-size: 15px; margin-right: 4px; }

/* ---------- toast ---------- */
.toasts { position: fixed; left: 50%; bottom: 22px; transform: translateX(-50%); display: grid; gap: 8px; z-index: 30; }
.toast {
  background: var(--ink); color: var(--paper); padding: 9px 14px; border-radius: 4px; font-size: 13px;
  box-shadow: 0 6px 18px rgba(29, 29, 31, 0.18);
  animation: toast-in 160ms ease-out;
}
.toast.bad { background: var(--red-ink); }
@keyframes toast-in { from { opacity: 0; transform: translateY(6px); } to { opacity: 1; transform: none; } }
@media (prefers-reduced-motion: reduce) { .toast { animation: none; } }

/* ---------- responsive ---------- */
@media (max-width: 1180px) {
  :root { --rail-w: 196px; --detail-w: 400px; }
  .work.split { grid-template-columns: minmax(0, 1fr); }
  .work.split .pane-detail {
    position: fixed; top: 52px; right: 0; bottom: 0; width: min(480px, 94vw); z-index: 10;
    box-shadow: -12px 0 32px rgba(29, 29, 31, 0.12);
  }
}
@media (max-width: 860px) {
  .shell { grid-template-columns: minmax(0, 1fr); grid-template-rows: 52px auto minmax(0, 1fr); }
  .top { padding: 0 14px; gap: 12px; }
  .brand small { display: none; }
  .who span.name { display: none; }
  .rail { border-right: 0; border-bottom: 1px solid var(--line); padding: 6px 8px; overflow-x: auto; display: flex; gap: 4px; }
  .rail-group { display: flex; gap: 2px; padding: 0; }
  .rail-title, .rail-foot { display: none; }
  .rail-item { padding: 6px 9px; font-size: 13px; }
  .pane-head { padding: 16px 14px 0; }
  .list-scroll { padding: 6px 14px 40px; }
  .row { margin-left: -6px; }
  .overview { padding: 16px 14px 40px; }
  .ov-grid, .charts { grid-template-columns: minmax(0, 1fr); }
  .work.split .pane-detail { top: 0; width: 100vw; }
  .ledger td.act { display: none; }
}
`;
