// HTML shell for the admin console. Script and styles are served as separate
// files so the page can run under a strict Content-Security-Policy with no
// inline code.
export const adminPageHtml = `<!doctype html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="robots" content="noindex, nofollow">
<meta name="color-scheme" content="light">
<title>安忆后台</title>
<link rel="icon" href="data:,">
<link rel="stylesheet" href="/admin/app.css">
</head>
<body>
<noscript>安忆后台需要启用 JavaScript。</noscript>
<div id="app"></div>
<script src="/admin/app.js" defer></script>
</body>
</html>
`;
