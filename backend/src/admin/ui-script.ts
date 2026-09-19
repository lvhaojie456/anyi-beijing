// Client script for the admin console, served at /admin/app.js.
// Written without template literals so it can live inside this String.raw block.
export const adminScript = String.raw`/* 安忆管理后台 — 单文件前端。无外部依赖，配合 /admin/* 接口使用。 */
(function () {
  'use strict';

  /* ---------------------------------------------------------------- 基础工具 */

  var SESSION_KEY = 'anyi_admin_session';
  var doc = document;

  function h(tag, props, children) {
    var node = doc.createElement(tag);
    if (props) {
      Object.keys(props).forEach(function (key) {
        var value = props[key];
        if (value === null || value === undefined || value === false) return;
        if (key === 'class') node.className = value;
        else if (key === 'text') node.textContent = value;
        else if (key === 'html') node.innerHTML = value;
        else if (key === 'dataset') Object.keys(value).forEach(function (k) { node.dataset[k] = value[k]; });
        else if (key.slice(0, 2) === 'on') node.addEventListener(key.slice(2), value);
        else if (key === 'checked' || key === 'disabled' || key === 'selected' || key === 'value') node[key] = value;
        else if (key === 'style') Object.keys(value).forEach(function (k) { node.style[k] = value[k]; });
        else node.setAttribute(key, value);
      });
    }
    append(node, children);
    return node;
  }

  function append(node, children) {
    if (children === null || children === undefined || children === false) return node;
    if (Array.isArray(children)) {
      children.forEach(function (child) { append(node, child); });
    } else if (typeof children === 'string' || typeof children === 'number') {
      node.appendChild(doc.createTextNode(String(children)));
    } else {
      node.appendChild(children);
    }
    return node;
  }

  function svgIcon(name) {
    var paths = {
      search: 'M10.5 3a7.5 7.5 0 1 1-4.7 13.3L3 19.1 4.9 17a7.5 7.5 0 0 1 5.6-14zm0 2a5.5 5.5 0 1 0 0 11 5.5 5.5 0 0 0 0-11z',
      close: 'M6.2 4.8 12 10.6l5.8-5.8 1.4 1.4-5.8 5.8 5.8 5.8-1.4 1.4-5.8-5.8-5.8 5.8-1.4-1.4 5.8-5.8-5.8-5.8z'
    };
    var svg = doc.createElementNS('http://www.w3.org/2000/svg', 'svg');
    svg.setAttribute('width', '16');
    svg.setAttribute('height', '16');
    svg.setAttribute('viewBox', '0 0 24 24');
    svg.setAttribute('aria-hidden', 'true');
    var path = doc.createElementNS('http://www.w3.org/2000/svg', 'path');
    path.setAttribute('d', paths[name] || '');
    path.setAttribute('fill', 'currentColor');
    svg.appendChild(path);
    return svg;
  }

  function pad(n) { return n < 10 ? '0' + n : String(n); }

  function fmtTime(value) {
    if (!value) return '';
    var d = new Date(value);
    if (isNaN(d.getTime())) return String(value);
    return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes());
  }

  function timeAgo(value) {
    if (!value) return '';
    var t = new Date(value).getTime();
    if (isNaN(t)) return String(value);
    var diff = Date.now() - t;
    if (diff < 0) diff = 0;
    var m = Math.floor(diff / 60000);
    if (m < 1) return '刚刚';
    if (m < 60) return m + ' 分钟前';
    var hr = Math.floor(m / 60);
    if (hr < 24) return hr + ' 小时前';
    var day = Math.floor(hr / 24);
    if (day === 1) return '昨天';
    if (day < 30) return day + ' 天前';
    return fmtTime(value).slice(0, 10);
  }

  function timeNode(value) {
    return h('span', { title: fmtTime(value), text: timeAgo(value) });
  }

  function fmtBytes(n) {
    n = Number(n || 0);
    if (n < 1024) return n + ' B';
    if (n < 1024 * 1024) return (n / 1024).toFixed(0) + ' KB';
    return (n / 1024 / 1024).toFixed(1) + ' MB';
  }

  function fmtNumber(n) {
    return Number(n || 0).toLocaleString('zh-CN');
  }

  function excerpt(text, max) {
    text = String(text || '').replace(/\s+/g, ' ').trim();
    if (!text) return '';
    return text.length > max ? text.slice(0, max) + '…' : text;
  }

  function parseJsonSafe(raw, fallback) {
    try { return JSON.parse(raw); } catch (e) { return fallback; }
  }

  function query(params) {
    var parts = [];
    Object.keys(params || {}).forEach(function (key) {
      var value = params[key];
      if (value === undefined || value === null || value === '') return;
      parts.push(encodeURIComponent(key) + '=' + encodeURIComponent(value));
    });
    return parts.length ? '?' + parts.join('&') : '';
  }

  /* ------------------------------------------------------------- 状态与文案 */

  var STATUS = {
    pending: ['待处理', 'pending'],
    approved: ['已通过', 'good'],
    rejected: ['已拒绝', 'bad'],
    quarantined: ['已隔离', 'bad'],
    blocked: ['已屏蔽', 'bad'],
    banned: ['已封禁', 'bad'],
    active: ['正常', 'good'],
    actioned: ['已处理', 'good'],
    dismissed: ['已驳回', ''],
    processing: ['处理中', 'busy'],
    completed: ['已完成', 'good'],
    deleted: ['已删除', 'good'],
    failed: ['失败', 'bad'],
    queued: ['排队中', 'pending'],
    running: ['生成中', 'busy'],
    succeeded: ['已完成', 'good'],
    cancelled: ['已取消', ''],
    open: ['招募中', 'good'],
    closed: ['已截止', '']
  };

  function statusNode(status, override) {
    var entry = STATUS[status] || [status || '未知', ''];
    return h('span', { class: 'status ' + entry[1] }, [h('i', { class: 'dot' }), override || entry[0]]);
  }

  function statusText(status) {
    var entry = STATUS[status];
    return entry ? entry[0] : (status || '未知');
  }

  var SCOPE_LABELS = [
    ['profiles', '个人头像'],
    ['ai/avatar-generated', 'AI 生成头像'],
    ['ai/avatar', '陪伴对象头像'],
    ['ai/background/list', '陪伴列表背景'],
    ['ai/background/chat', '聊天背景'],
    ['ai/voice', '语音消息'],
    ['ai/speech', '合成语音'],
    ['ai/live2d', '动态形象'],
    ['community/posts', '社区动态图片'],
    ['community/volunteer', '招募封面'],
    ['memorials', '纪念馆照片'],
    ['general', '其他上传']
  ];

  function scopeOf(assetKey) {
    var parts = String(assetKey || '').split('/');
    if (parts.length < 3) return '';
    return parts.slice(1, -1).join('/');
  }

  function scopeLabel(assetKey) {
    var scope = scopeOf(assetKey);
    for (var i = 0; i < SCOPE_LABELS.length; i += 1) {
      if (scope === SCOPE_LABELS[i][0] || scope.indexOf(SCOPE_LABELS[i][0] + '/') === 0) return SCOPE_LABELS[i][1];
    }
    return scope || '未知用途';
  }

  function mimeLabel(mime) {
    mime = String(mime || '');
    if (mime.indexOf('image/') === 0) return '图片';
    if (mime.indexOf('audio/') === 0) return '音频';
    if (mime === 'text/plain') return '文本';
    return mime || '文件';
  }

  var REASON_LABELS = {
    media_manual_review_required: '媒体文件需人工审核',
    text_auto_checked: '文本自动检查通过',
    upload_review_rejected: '上传审核拒绝',
    upload_review_quarantined: '上传审核隔离',
    community_post_rejected: '动态被拒绝',
    community_post_blocked: '动态被屏蔽',
    account_deleted: '账号已注销',
    ai_list_background_replaced: '列表背景已替换',
    ai_chat_background_replaced: '聊天背景已替换',
    ai_avatar_replaced: '头像已替换',
    ai_avatar_rejected: '头像被拒绝',
    ai_companion_deleted: '陪伴对象已删除',
    ai_speech_cache_expired: '合成语音缓存过期',
    upload_transaction_rollback: '上传事务回滚',
    asset_delete_no_longer_allowed: '文件仍被引用，未删除'
  };

  function reasonLabel(reason) {
    if (!reason) return '';
    reason = String(reason);
    if (REASON_LABELS[reason]) return REASON_LABELS[reason];
    if (reason.indexOf('blocked_keyword:') === 0) return '命中关键词「' + reason.slice(16) + '」';
    if (reason.indexOf('manual_review_keyword:') === 0) return '关键词待复核「' + reason.slice(22) + '」';
    return reason;
  }

  var ACTION_LABELS = {
    'admin.upload.review': '审核上传',
    'admin.community.post.moderate': '审核动态',
    'admin.community.comment.moderate': '审核评论',
    'admin.community.report.review': '处理举报',
    'admin.user.moderation': '处置用户',
    'admin.user.account.delete': '注销账号（管理员操作）',
    'admin.asset_delete_queue.process': '处理文件删除队列',
    'admin.asset_delete_queue.retry': '重试文件删除',
    'admin.account_deletion_request.status_update': '更新注销申请',
    'community.post.create': '发布动态',
    'community.post.delete': '删除动态',
    'community.comment.create': '发表评论',
    'community.comment.delete': '删除评论',
    'community.report.create': '提交举报',
    'community.volunteer.create': '发布招募',
    'community.volunteer.open': '开放招募',
    'community.volunteer.closed': '截止招募',
    'community.volunteer.application.create': '提交义工报名',
    'community.volunteer.application.approved': '通过义工报名',
    'community.volunteer.application.rejected': '拒绝义工报名',
    'community.volunteer.application.cancelled': '取消义工报名',
    'user.profile.update': '更新资料',
    'user.account.delete': '注销账号（本人）'
  };

  function actionLabel(action) {
    return ACTION_LABELS[action] || action || '';
  }

  var TARGET_LABELS = {
    upload_review: '上传审核',
    community_post: '动态',
    community_post_comment: '评论',
    community_comment: '评论',
    community_volunteer_post: '义工招募',
    community_volunteer_application: '义工报名',
    user: '用户',
    account_deletion_request: '注销申请',
    asset_delete_queue: '文件删除队列'
  };

  var STAGE_LABELS = {
    queued: '排队', preparing: '准备', generating: '生成图像', planning: '规划', decomposing: '拆分图层',
    expressions: '表情', refining: '细化', rigging: '绑定', verifying: '校验', uploading: '上传', completed: '完成',
    failed: '失败', cancelled: '取消'
  };

  var DIAGNOSIS_LABELS = {
    provider_unavailable: '图像供应商不可用',
    background_leak: '背景混入图层',
    face_not_located: '未定位到人脸',
    expression_failed: '表情生成失败',
    rig_unstable: '绑定不稳定',
    budget_exhausted: '预算耗尽'
  };

  var SUGGESTION_LABELS = { retry: '直接重试', regenerate_image: '重新生成图片', new_input: '更换输入图片' };

  var ERROR_LABELS = {
    invalid_credentials: '账号或密码不正确',
    admin_required: '这个账号不是管理员',
    missing_token: '登录已失效，请重新登录',
    invalid_token: '登录已失效，请重新登录',
    expired_token: '登录已过期，请重新登录',
    rate_limit_exceeded: '操作太频繁，请稍后再试',
    review_status_conflict: '这条记录已被处理过',
    community_media_not_approved: '动态里的图片还没有通过审核，先在「上传审核」里通过图片',
    community_report_already_reviewed: '这条举报已经处理过',
    admin_moderation_forbidden: '不能处置管理员账号',
    admin_account_deletion_forbidden: '不能注销管理员账号',
    confirm_username_mismatch: '输入的用户名和账号不一致',
    cannot_moderate_self: '不能处置自己',
    volunteer_application_not_pending: '这条报名已经处理过',
    community_image_not_owned: '封面必须是当前管理员上传的图片',
    volunteer_deadline_must_be_future: '截止时间必须晚于现在',
    invalid_volunteer_deadline: '截止时间格式不正确',
    file_required: '请选择文件',
    unsupported_file_type: '不支持的文件类型',
    file_size_invalid: '文件太大',
    request_failed: '请求失败',
    network_error: '网络连接失败'
  };

  function errorText(error) {
    var code = error && error.code ? error.code : (error && error.message ? error.message : 'request_failed');
    return ERROR_LABELS[code] || ('请求失败：' + code);
  }

  /* ----------------------------------------------------------------- 会话 */

  var session = null;

  function loadSession() {
    var raw = sessionStorage.getItem(SESSION_KEY) || localStorage.getItem(SESSION_KEY);
    var data = raw ? parseJsonSafe(raw, null) : null;
    return data && data.token && data.user ? data : null;
  }

  function saveSession(data, remember) {
    var raw = JSON.stringify(data);
    sessionStorage.setItem(SESSION_KEY, raw);
    if (remember) localStorage.setItem(SESSION_KEY, raw);
    else localStorage.removeItem(SESSION_KEY);
  }

  function clearSession() {
    sessionStorage.removeItem(SESSION_KEY);
    localStorage.removeItem(SESSION_KEY);
    session = null;
    media.clear();
  }

  function ApiError(status, code, details) {
    this.status = status;
    this.code = code;
    this.details = details;
    this.message = code;
  }

  function api(path, options) {
    options = options || {};
    var headers = {};
    if (session && session.token) headers.Authorization = 'Bearer ' + session.token;
    if (options.json !== undefined) {
      headers['Content-Type'] = 'application/json';
      options.body = JSON.stringify(options.json);
    }
    return fetch(path, { method: options.method || 'GET', headers: headers, body: options.body })
      .catch(function () { throw new ApiError(0, 'network_error'); })
      .then(function (res) {
        return res.text().then(function (text) {
          var data = text ? parseJsonSafe(text, {}) : {};
          if (!res.ok) {
            if (res.status === 401 && session) {
              clearSession();
              render();
            }
            throw new ApiError(res.status, data.error || 'request_failed', data.details);
          }
          return data;
        });
      });
  }

  /* --------------------------------------------------------- 媒体文件缓存 */

  var media = {
    cache: {},
    order: [],
    key: function (assetKey, max) { return assetKey + '|' + (max || 'full'); },
    url: function (assetKey, max) {
      var key = this.key(assetKey, max);
      if (this.cache[key]) return this.cache[key];
      var self = this;
      var promise = fetch('/assets/' + encodeURIComponent(assetKey) + (max ? '?max=' + max : ''), {
        headers: { Authorization: 'Bearer ' + (session ? session.token : '') }
      }).then(function (res) {
        if (!res.ok) throw new ApiError(res.status, 'asset_unavailable');
        return res.blob();
      }).then(function (blob) {
        return { url: URL.createObjectURL(blob), type: blob.type, blob: blob };
      });
      promise.catch(function () { delete self.cache[key]; });
      this.cache[key] = promise;
      this.order.push(key);
      if (this.order.length > 80) {
        var old = this.order.shift();
        var stale = this.cache[old];
        delete this.cache[old];
        if (stale) stale.then(function (entry) { URL.revokeObjectURL(entry.url); }).catch(function () {});
      }
      return promise;
    },
    clear: function () {
      var self = this;
      this.order.forEach(function (key) {
        var entry = self.cache[key];
        if (entry) entry.then(function (value) { URL.revokeObjectURL(value.url); }).catch(function () {});
      });
      this.cache = {};
      this.order = [];
    }
  };

  function assetKeyFromUrl(url) {
    var marker = '/assets/';
    var index = String(url || '').indexOf(marker);
    if (index < 0) return null;
    try { return decodeURIComponent(String(url).slice(index + marker.length).split('?')[0]); } catch (e) { return null; }
  }

  function imageInto(container, assetKey, max, alt) {
    container.textContent = '';
    container.appendChild(h('div', { class: 'stage-note', text: '载入中' }));
    media.url(assetKey, max).then(function (entry) {
      container.textContent = '';
      container.appendChild(h('img', { src: entry.url, alt: alt || '' }));
    }).catch(function () {
      container.textContent = '';
      container.appendChild(h('div', { class: 'stage-note', text: '文件不可用：可能已被拒绝、隔离或删除' }));
    });
  }

  function stageFor(assetKey, mime) {
    var stage = h('div', { class: 'stage' });
    mime = String(mime || '');
    if (mime.indexOf('image/') === 0) {
      imageInto(stage, assetKey, 1024, '待审核图片');
    } else if (mime.indexOf('audio/') === 0) {
      stage.appendChild(h('div', { class: 'stage-note', text: '载入音频' }));
      media.url(assetKey).then(function (entry) {
        stage.textContent = '';
        stage.appendChild(h('audio', { controls: 'controls', src: entry.url, preload: 'metadata' }));
      }).catch(function () {
        stage.textContent = '';
        stage.appendChild(h('div', { class: 'stage-note', text: '音频不可用：可能已被拒绝、隔离或删除' }));
      });
    } else if (mime === 'text/plain') {
      stage.appendChild(h('div', { class: 'stage-note', text: '载入文本' }));
      media.url(assetKey).then(function (entry) {
        return entry.blob.text();
      }).then(function (text) {
        stage.textContent = '';
        stage.appendChild(h('pre', { text: text.slice(0, 20000) }));
      }).catch(function () {
        stage.textContent = '';
        stage.appendChild(h('div', { class: 'stage-note', text: '文本不可用' }));
      });
    } else {
      stage.appendChild(h('div', { class: 'stage-note', text: '此类型没有预览' }));
    }
    return stage;
  }

  /* ----------------------------------------------------------------- 提示 */

  var toastRoot = null;
  function toast(message, bad) {
    if (!toastRoot) {
      toastRoot = h('div', { class: 'toasts', role: 'status', 'aria-live': 'polite' });
      doc.body.appendChild(toastRoot);
    }
    var node = h('div', { class: 'toast' + (bad ? ' bad' : ''), text: message });
    toastRoot.appendChild(node);
    setTimeout(function () { if (node.parentNode) node.parentNode.removeChild(node); }, bad ? 5000 : 2600);
  }

  /* ----------------------------------------------------------------- 路由 */

  function parseHash() {
    var hash = location.hash.replace(/^#\/?/, '');
    var parts = hash.split('?');
    var view = parts[0] || 'overview';
    var params = {};
    if (parts[1]) {
      parts[1].split('&').forEach(function (pair) {
        if (!pair) return;
        var kv = pair.split('=');
        params[decodeURIComponent(kv[0])] = decodeURIComponent(kv.slice(1).join('=') || '');
      });
    }
    return { view: views[view] ? view : 'overview', params: params };
  }

  function hashFor(view, params) {
    return '#/' + view + query(params);
  }

  function navigate(view, params, replace) {
    var next = hashFor(view, params);
    if (replace) history.replaceState(null, '', next);
    else location.hash = next;
    if (replace) route();
  }

  function withParams(extra) {
    var params = {};
    Object.keys(state.params).forEach(function (key) { params[key] = state.params[key]; });
    Object.keys(extra).forEach(function (key) {
      if (extra[key] === null || extra[key] === undefined) delete params[key];
      else params[key] = extra[key];
    });
    return params;
  }

  /* ----------------------------------------------------------------- 状态 */

  var state = {
    view: 'overview',
    params: {},
    items: [],
    counts: {},
    extra: null,
    loading: false,
    error: null,
    selectedId: null,
    checked: {},
    loadToken: 0,
    overview: null,
    detailCache: {}
  };

  var root = doc.getElementById('app');
  var refs = {};

  /* ----------------------------------------------------------- 视图定义 */

  function reasonBox(chips, placeholder) {
    var box = h('div', { class: 'reason' });
    var input = h('textarea', { class: 'textarea textarea-short', rows: '2', placeholder: placeholder || '处理原因（拒绝、隔离、屏蔽时必填，会记入审计日志）' });
    var chipRow = h('div', { class: 'chips' }, chips.map(function (label) {
      return h('button', { type: 'button', class: 'chip', text: label, onclick: function () {
        input.value = input.value ? input.value + '；' + label : label;
        input.focus();
      } });
    }));
    box.appendChild(chipRow);
    box.appendChild(input);
    box.value = function () { return input.value.trim(); };
    box.focusInput = function () { input.focus(); };
    return box;
  }

  function metaList(pairs) {
    var dl = h('dl', { class: 'meta' });
    pairs.forEach(function (pair) {
      if (!pair || pair[1] === null || pair[1] === undefined || pair[1] === '') return;
      dl.appendChild(h('dt', { text: pair[0] }));
      dl.appendChild(h('dd', { class: pair[2] || '' }, pair[1]));
    });
    return dl;
  }

  function userLink(id, name, username) {
    var label = name || username || id || '';
    if (!id) return h('span', { text: label });
    return h('a', { href: hashFor('users', { id: id }), text: label + (username && username !== label ? '（' + username + '）' : '') });
  }

  function section(title, body) {
    return h('div', null, [h('div', { class: 'section-title', text: title }), body]);
  }

  function keysHint(pairs) {
    return h('div', { class: 'keys' }, pairs.map(function (pair) {
      return h('span', null, [h('kbd', { text: pair[0] }), ' ' + pair[1]]);
    }));
  }

  function emptyNode(title, sub) {
    return h('div', { class: 'empty' }, [h('b', { text: title }), sub || '']);
  }

  var UPLOAD_CHIPS = ['涉及色情', '血腥暴力', '广告或二维码', '涉及他人隐私', '与产品无关', '图片无法辨认'];
  var COMMUNITY_CHIPS = ['广告引流', '人身攻击', '涉政或违法', '虚假信息', '违反社区规范'];
  var USER_CHIPS = ['多次违规', '骚扰他人', '发布违规内容', '涉嫌诈骗'];

  var views = {};

  /* 总览 */
  views.overview = { title: '总览', custom: true };

  /* 上传审核 */
  views.uploads = {
    title: '上传审核',
    queue: 'uploads',
    filterParam: 'status',
    filters: [['pending', '待审核'], ['approved', '已通过'], ['rejected', '已拒绝'], ['quarantined', '已隔离']],
    defaultFilter: 'pending',
    bulk: true,
    load: function (params) {
      return api('/admin/upload-reviews' + query({ status: params.status || 'pending' })).then(function (data) {
        return { items: data.reviews || [], counts: data.counts || {} };
      });
    },
    row: function (item) {
      return {
        title: mimeLabel(item.mime_type) + '，' + scopeLabel(item.asset_key),
        text: '上传者 ' + (item.display_name || item.username || item.owner_id || ''),
        meta: [fmtBytes(item.size_bytes), reasonLabel(item.reason)],
        time: item.created_at,
        status: item.status
      };
    },
    detail: function (item, ctx) {
      var body = [];
      body.push(stageFor(item.asset_key, item.mime_type));
      body.push(metaList([
        ['用途', scopeLabel(item.asset_key)],
        ['类型', item.mime_type],
        ['大小', fmtBytes(item.size_bytes)],
        ['上传者', userLink(item.owner_id, item.display_name, item.username)],
        ['上传时间', fmtTime(item.created_at)],
        ['当前状态', statusNode(item.status)],
        ['审核时间', item.reviewed_at ? fmtTime(item.reviewed_at) : null],
        ['审核人', item.reviewer_username || null],
        ['原因', reasonLabel(item.reason) || null],
        ['文件键', item.asset_key, 'mono']
      ]));
      if (item.status === 'pending') {
        var reason = reasonBox(UPLOAD_CHIPS);
        function act(status, label) {
          var text = reason.value();
          if (status !== 'approved' && !text) { reason.focusInput(); toast('请先填写原因', true); return; }
          ctx.run(api('/admin/upload-reviews/' + encodeURIComponent(item.id), { method: 'PATCH', json: { status: status, reason: text } }), label);
        }
        ctx.keys = { a: function () { act('approved', '已通过'); }, x: function () { act('rejected', '已拒绝'); }, q: function () { act('quarantined', '已隔离'); } };
        body.push(section('处理', h('div', { class: 'form' }, [
          reason,
          h('div', { class: 'actions' }, [
            h('button', { class: 'btn btn-good', onclick: function () { act('approved', '已通过'); } }, ['通过', h('kbd', { text: 'A' })]),
            h('button', { class: 'btn btn-danger', onclick: function () { act('rejected', '已拒绝'); } }, ['拒绝', h('kbd', { text: 'X' })]),
            h('button', { class: 'btn', onclick: function () { act('quarantined', '已隔离'); } }, ['隔离', h('kbd', { text: 'Q' })])
          ]),
          h('p', { class: 'actions-note', text: '拒绝或隔离会把文件设为私有、解除头像或语音引用，并加入文件删除队列。通过头像类图片会同时公开给其他用户。' })
        ])));
      } else {
        body.push(h('p', { class: 'actions-note', text: '这条记录已经处理，状态不能再改。若文件被误拒，用户需要重新上传。' }));
      }
      return body;
    }
  };

  /* 社区内容 */
  views.community = {
    title: '社区内容',
    queue: 'community',
    filterParam: 'status',
    filters: [['pending', '待审核'], ['approved', '已通过'], ['rejected', '已拒绝'], ['blocked', '已屏蔽'], ['all', '全部']],
    defaultFilter: 'pending',
    load: function (params) {
      return api('/admin/community/moderation' + query({ status: params.status || 'pending' })).then(function (data) {
        var items = (data.posts || []).map(function (post) { post.kind = 'post'; return post; })
          .concat((data.comments || []).map(function (comment) { comment.kind = 'comment'; return comment; }));
        items.sort(function (a, b) { return String(b.createdAt).localeCompare(String(a.createdAt)); });
        return { items: items, counts: data.counts || {} };
      });
    },
    row: function (item) {
      var images = (item.imageUrls || []).length;
      return {
        kind: item.kind === 'post' ? '动态' : '评论',
        title: item.authorName || item.authorUsername || '',
        text: excerpt(item.content, 140) || (images ? '（仅图片）' : '（空）'),
        meta: [images ? images + ' 张图片' : '', reasonLabel(item.moderationReason)],
        time: item.createdAt,
        status: item.moderationStatus
      };
    },
    detail: function (item, ctx) {
      var body = [];
      var isPost = item.kind === 'post';
      if (item.imageUrls && item.imageUrls.length) {
        var stage = h('div', { class: 'stage' });
        var thumbs = h('div', { class: 'thumbs' });
        var buttons = [];
        item.imageUrls.forEach(function (url, index) {
          var key = assetKeyFromUrl(url);
          var thumb = h('button', { type: 'button', class: 'thumb' + (index === 0 ? ' active' : ''), title: '查看大图' });
          if (key) {
            media.url(key, 256).then(function (entry) {
              thumb.textContent = '';
              thumb.appendChild(h('img', { src: entry.url, alt: '' }));
            }).catch(function () { thumb.textContent = '不可用'; });
          } else {
            thumb.textContent = '外链';
          }
          thumb.addEventListener('click', function () {
            buttons.forEach(function (b) { b.classList.remove('active'); });
            thumb.classList.add('active');
            if (key) imageInto(stage, key, 1024, '动态图片');
          });
          buttons.push(thumb);
          thumbs.appendChild(thumb);
        });
        var firstKey = assetKeyFromUrl(item.imageUrls[0]);
        if (firstKey) imageInto(stage, firstKey, 1024, '动态图片');
        else stage.appendChild(h('div', { class: 'stage-note', text: '图片不在本站' }));
        body.push(stage);
        if (item.imageUrls.length > 1) body.push(thumbs);
      }
      body.push(section(isPost ? '动态内容' : '评论内容', h('div', { class: 'content-block', text: item.content || '（无文字）' })));
      body.push(metaList([
        ['作者', userLink(item.authorId, item.authorName, item.authorUsername)],
        ['发布时间', fmtTime(item.createdAt)],
        ['当前状态', statusNode(item.moderationStatus)],
        ['审核原因', reasonLabel(item.moderationReason) || null],
        [isPost ? '动态编号' : '所属动态', isPost ? item.id : item.postId, 'mono']
      ]));
      var reason = reasonBox(COMMUNITY_CHIPS);
      var path = isPost ? '/admin/community/posts/' : '/admin/community/comments/';
      function act(status, label) {
        var text = reason.value();
        if (status !== 'approved' && !text) { reason.focusInput(); toast('请先填写原因', true); return; }
        ctx.run(api(path + encodeURIComponent(item.id), { method: 'PATCH', json: { status: status, reason: text } }), label);
      }
      ctx.keys = { a: function () { act('approved', '已通过'); }, x: function () { act('rejected', '已拒绝'); }, q: function () { act('blocked', '已屏蔽'); } };
      var actions = [];
      if (item.moderationStatus !== 'approved') actions.push(h('button', { class: 'btn btn-good', onclick: function () { act('approved', '已通过'); } }, ['通过', h('kbd', { text: 'A' })]));
      if (item.moderationStatus !== 'rejected') actions.push(h('button', { class: 'btn btn-danger', onclick: function () { act('rejected', '已拒绝'); } }, ['拒绝', h('kbd', { text: 'X' })]));
      if (item.moderationStatus !== 'blocked') actions.push(h('button', { class: 'btn', onclick: function () { act('blocked', '已屏蔽'); } }, ['屏蔽', h('kbd', { text: 'Q' })]));
      body.push(section('处理', h('div', { class: 'form' }, [
        reason,
        h('div', { class: 'actions' }, actions),
        h('p', { class: 'actions-note', text: isPost
          ? '通过动态前，其中的图片必须已经在「上传审核」通过。拒绝或屏蔽会把图片设为私有并加入删除队列；屏蔽用于保留记录但对外隐藏。'
          : '评论只影响文字，不涉及文件。' })
      ])));
      return body;
    }
  };

  /* 举报 */
  views.reports = {
    title: '举报处理',
    queue: 'reports',
    filterParam: 'status',
    filters: [['pending', '待处理'], ['actioned', '已处理'], ['dismissed', '已驳回'], ['all', '全部']],
    defaultFilter: 'pending',
    load: function (params) {
      return api('/admin/community/reports' + query({ status: params.status || 'pending' })).then(function (data) {
        return { items: data.reports || [], counts: data.counts || {} };
      });
    },
    row: function (item) {
      var target = item.target || {};
      return {
        kind: item.target_type === 'post' ? '举报动态' : '举报评论',
        title: target.exists ? (target.authorName || target.authorUsername || '') : '内容已不存在',
        text: '理由：' + excerpt(item.reason, 100) + (target.exists ? '　内容：' + excerpt(target.content, 80) : ''),
        meta: ['举报人 ' + (item.reporter_display_name || item.reporter_username || '')],
        time: item.created_at,
        status: item.status
      };
    },
    detail: function (item, ctx) {
      var target = item.target || {};
      var body = [];
      body.push(section('举报理由', h('div', { class: 'content-block', text: item.reason || '' })));
      if (target.exists) {
        if (target.imageUrls && target.imageUrls.length) {
          var thumbs = h('div', { class: 'thumbs' });
          target.imageUrls.forEach(function (url) {
            var key = assetKeyFromUrl(url);
            var thumb = h('button', { type: 'button', class: 'thumb', title: '查看大图' });
            if (key) {
              media.url(key, 256).then(function (entry) {
                thumb.textContent = '';
                thumb.appendChild(h('img', { src: entry.url, alt: '' }));
              }).catch(function () { thumb.textContent = '不可用'; });
              thumb.addEventListener('click', function () {
                var stage = h('div', { class: 'stage' });
                imageInto(stage, key, 1024, '被举报图片');
                thumbs.parentNode.insertBefore(stage, thumbs);
              });
            }
            thumbs.appendChild(thumb);
          });
          body.push(thumbs);
        }
        body.push(section('被举报的' + (item.target_type === 'post' ? '动态' : '评论'), h('div', { class: 'content-block', text: target.content || '（无文字）' })));
      } else {
        body.push(section('被举报内容', h('p', { class: 'actions-note', text: '内容已被删除或不存在，只能驳回这条举报。' })));
      }
      body.push(metaList([
        ['内容作者', target.exists ? userLink(target.authorId, target.authorName, target.authorUsername) : null],
        ['内容状态', target.exists ? statusNode(target.status) : null],
        ['举报人', userLink(item.reporter_id, item.reporter_display_name, item.reporter_username)],
        ['举报时间', fmtTime(item.created_at)],
        ['举报状态', statusNode(item.status)],
        ['处理时间', item.reviewed_at ? fmtTime(item.reviewed_at) : null]
      ]));
      if (item.status === 'pending') {
        var reason = reasonBox(COMMUNITY_CHIPS.concat(USER_CHIPS), '处理备注（移除、屏蔽、封禁时必填）');
        function act(action, label, needReason) {
          var text = reason.value();
          if (needReason && !text) { reason.focusInput(); toast('请先填写处理备注', true); return; }
          ctx.run(api('/admin/community/reports/' + encodeURIComponent(item.id), { method: 'PATCH', json: { action: action, reason: text } }), label);
        }
        ctx.keys = { a: function () { act('approve', '已保留内容', false); }, x: function () { act('remove', '已移除内容', true); } };
        var actions = [];
        if (target.exists) {
          actions.push(h('button', { class: 'btn btn-good', onclick: function () { act('approve', '已保留内容', false); } }, ['保留内容', h('kbd', { text: 'A' })]));
          actions.push(h('button', { class: 'btn btn-danger', onclick: function () { act('remove', '已移除内容', true); } }, ['移除内容', h('kbd', { text: 'X' })]));
        }
        actions.push(h('button', { class: 'btn', onclick: function () { act('dismiss', '已驳回举报', false); } }, '驳回举报'));
        if (target.exists) {
          actions.push(h('button', { class: 'btn btn-danger', onclick: function () { act('block_user', '已屏蔽作者', true); } }, '屏蔽作者'));
          actions.push(h('button', { class: 'btn btn-danger', onclick: function () { act('ban_user', '已封禁作者', true); } }, '封禁作者'));
        }
        body.push(section('处理', h('div', { class: 'form' }, [
          reason,
          h('div', { class: 'actions' }, actions),
          h('p', { class: 'actions-note', text: '保留或移除只作用于被举报的内容；屏蔽和封禁只作用于作者账号，内容保持原状，需要时再到「社区内容」处理。' })
        ])));
      }
      return body;
    }
  };

  /* 用户 */
  views.users = {
    title: '用户',
    filterParam: 'filter',
    filters: [['all', '全部'], ['moderated', '处置中'], ['admins', '管理员']],
    defaultFilter: 'all',
    search: true,
    load: function (params) {
      return api('/admin/users' + query({ q: params.q || '', filter: params.filter || 'all' })).then(function (data) {
        return { items: data.users || [], counts: {} };
      });
    },
    row: function (item) {
      var meta = [item.username, item.role === 'admin' ? '管理员' : '', item.wechat_bound ? '微信已绑定' : ''];
      if (item.moderation_status && item.moderation_expires_at) meta.push('到期 ' + fmtTime(item.moderation_expires_at));
      return {
        title: item.display_name || item.username,
        text: item.moderation_reason ? '处置原因：' + excerpt(item.moderation_reason, 100) : '',
        meta: meta,
        time: item.created_at,
        timeLabel: '注册',
        status: item.moderation_status || 'active'
      };
    },
    detailLoad: function (item) {
      return api('/admin/users/' + encodeURIComponent(item.id));
    },
    detail: function (item, ctx, data) {
      var user = data.user;
      var counts = data.counts || {};
      var body = [];
      var avatar = h('div', { class: 'avatar', text: (user.display_name || user.username || '?').slice(0, 1) });
      var avatarKey = user.avatar_url ? assetKeyFromUrl(user.avatar_url) : null;
      if (avatarKey) {
        media.url(avatarKey, 128).then(function (entry) {
          avatar.textContent = '';
          avatar.appendChild(h('img', { src: entry.url, alt: '' }));
        }).catch(function () {});
      }
      body.push(h('div', { class: 'person' }, [
        avatar,
        h('div', null, [
          h('div', { class: 'name', text: user.display_name || user.username }),
          h('div', { class: 'sub', text: user.username + (user.role === 'admin' ? '，管理员' : '') + (user.gender ? '，' + user.gender : '') })
        ])
      ]));
      body.push(h('div', { class: 'count-grid' }, [
        ['纪念馆', counts.memorials], ['社区动态', counts.posts], ['评论', counts.comments],
        ['陪伴对象', counts.companions], ['上传文件', counts.assets], ['待审上传', counts.pendingUploads],
        ['崩溃上报', counts.crashes], ['发起举报', counts.reportsFiled], ['被举报', counts.reportsAgainst]
      ].map(function (pair) {
        return h('div', { class: 'count-cell' }, [h('b', { text: fmtNumber(pair[1]) }), h('span', { text: pair[0] })]);
      })));
      body.push(metaList([
        ['注册时间', fmtTime(user.created_at)],
        ['微信登录', user.wechat_bound ? '已绑定' + (user.wechat_nickname ? '（' + user.wechat_nickname + '）' : '') : '未绑定'],
        ['协议同意', user.terms_accepted_at ? fmtTime(user.terms_accepted_at) : '无记录'],
        ['用户编号', user.id, 'mono']
      ]));

      var modBody = [];
      if (user.moderation_status) {
        modBody.push(metaList([
          ['当前状态', statusNode(user.moderation_status)],
          ['原因', user.moderation_reason || '未填写'],
          ['到期', user.moderation_expires_at ? fmtTime(user.moderation_expires_at) : '不自动解除'],
          ['更新时间', fmtTime(user.moderation_updated_at)]
        ]));
      } else {
        modBody.push(h('p', { class: 'actions-note', text: '账号状态正常，没有处置记录。' }));
      }
      if (user.role !== 'admin') {
        var reason = reasonBox(USER_CHIPS, '处置原因（必填，会展示给用户）');
        var duration = h('select', { class: 'select' }, [
          h('option', { value: '1', text: '屏蔽 1 天' }),
          h('option', { value: '7', text: '屏蔽 7 天' }),
          h('option', { value: '30', text: '屏蔽 30 天' }),
          h('option', { value: '0', text: '屏蔽，直到手动解除' })
        ]);
        duration.value = '7';
        function moderate(status, expiresAt, label) {
          var text = reason.value();
          if (status !== 'active' && !text) { reason.focusInput(); toast('请先填写处置原因', true); return; }
          ctx.run(api('/admin/users/' + encodeURIComponent(user.id) + '/moderation', {
            method: 'PATCH', json: { status: status, reason: text, expiresAt: expiresAt }
          }), label);
        }
        var actions = [];
        if (user.moderation_status) actions.push(h('button', { class: 'btn btn-good', onclick: function () { moderate('active', null, '已解除处置'); } }, '解除处置'));
        actions.push(h('button', { class: 'btn btn-danger', onclick: function () {
          var days = Number(duration.value);
          moderate('blocked', days > 0 ? new Date(Date.now() + days * 86400000).toISOString() : null, '已屏蔽');
        } }, '屏蔽'));
        actions.push(h('button', { class: 'btn btn-danger', onclick: function () { moderate('banned', null, '已封禁'); } }, '封禁'));
        modBody.push(h('div', { class: 'form' }, [reason, h('div', { class: 'inline' }, [duration]), h('div', { class: 'actions' }, actions),
          h('p', { class: 'actions-note', text: '屏蔽是临时限制，可设到期自动解除；封禁不会自动解除。处置期间用户无法登录使用 App，但数据保留。' })]));
      } else {
        modBody.push(h('p', { class: 'actions-note', text: '管理员账号不能被处置或注销。' }));
      }
      body.push(section('账号处置', h('div', { class: 'form' }, modBody)));

      if (data.companions && data.companions.length) {
        body.push(section('陪伴对象', h('ul', { class: 'linklist' }, data.companions.map(function (row) {
          return h('li', null, [h('span', { text: row.display_name + '（' + row.relation + '）' }), h('span', { class: 't', text: fmtTime(row.created_at).slice(0, 10) })]);
        }))));
      }
      if (data.memorials && data.memorials.length) {
        body.push(section('纪念馆', h('ul', { class: 'linklist' }, data.memorials.map(function (row) {
          return h('li', null, [h('span', { text: row.name }), h('span', { class: 't', text: fmtTime(row.created_at).slice(0, 10) })]);
        }))));
      }
      if (data.audit && data.audit.length) {
        body.push(section('相关记录', auditList(data.audit)));
      }
      if (user.role !== 'admin') {
        body.push(section('危险操作', dangerDelete(user, null, ctx)));
      }
      return body;
    }
  };

  function auditList(rows) {
    return h('ul', { class: 'audit-list' }, rows.map(function (row) {
      var meta = parseJsonSafe(row.metadata_json || '{}', {});
      var details = [];
      Object.keys(meta).forEach(function (key) {
        if (meta[key] === null || meta[key] === undefined || meta[key] === '') return;
        details.push(key + '=' + (typeof meta[key] === 'object' ? JSON.stringify(meta[key]) : meta[key]));
      });
      return h('li', null, [
        h('span', null, [actionLabel(row.action), h('span', { class: 't', text: '　' + (row.actor_username || (row.actor_role === 'admin' ? '管理员' : '')) + (TARGET_LABELS[row.target_type] ? '　' + TARGET_LABELS[row.target_type] : '') })]),
        h('span', { class: 't', title: fmtTime(row.created_at), text: timeAgo(row.created_at) }),
        details.length ? h('span', { class: 'd', text: details.join('，') }) : null
      ]);
    }));
  }

  function dangerDelete(user, deletionRequestId, ctx) {
    var open = h('button', { class: 'btn btn-danger btn-sm' }, '注销这个账号');
    var box = h('div', { class: 'confirm-box hidden' });
    var input = h('input', { class: 'input', placeholder: '输入用户名 ' + user.username + ' 确认' });
    var reason = h('input', { class: 'input', placeholder: '备注（可选，例如：用户邮件申请）' });
    box.appendChild(h('p', { text: '注销会立即永久删除该用户的纪念馆、社区内容、陪伴对象、聊天记录和上传文件，无法恢复。用户上传的文件会进入删除队列。' }));
    box.appendChild(input);
    box.appendChild(reason);
    box.appendChild(h('div', { class: 'actions' }, [
      h('button', { class: 'btn btn-danger', onclick: function () {
        if (input.value.trim().toLowerCase() !== String(user.username).toLowerCase()) { toast('用户名不一致', true); input.focus(); return; }
        ctx.run(api('/admin/users/' + encodeURIComponent(user.id), {
          method: 'DELETE', json: { confirmUsername: input.value.trim(), reason: reason.value.trim(), deletionRequestId: deletionRequestId }
        }), '账号已注销');
      } }, '确认注销'),
      h('button', { class: 'btn', onclick: function () { box.classList.add('hidden'); open.classList.remove('hidden'); } }, '取消')
    ]));
    open.addEventListener('click', function () { box.classList.remove('hidden'); open.classList.add('hidden'); input.focus(); });
    return h('div', { class: 'form' }, [open, box]);
  }

  /* 义工报名 */
  views.applications = {
    title: '义工报名',
    queue: 'volunteerApplications',
    filterParam: 'status',
    filters: [['pending', '待审核'], ['approved', '已通过'], ['rejected', '已拒绝'], ['cancelled', '已取消'], ['all', '全部']],
    defaultFilter: 'pending',
    load: function (params) {
      return api('/community/volunteer/applications' + query({ status: params.status || 'pending' })).then(function (data) {
        return { items: data.applications || [], counts: {} };
      });
    },
    row: function (item) {
      return {
        title: item.name + '，' + item.volunteerTitle,
        text: excerpt(item.note, 120),
        meta: [item.phone, '账号 ' + (item.applicantName || item.applicantUsername || '')],
        time: item.createdAt,
        status: item.status
      };
    },
    detail: function (item, ctx) {
      var body = [];
      body.push(metaList([
        ['报名岗位', item.volunteerTitle],
        ['姓名', item.name],
        ['电话', item.phone],
        ['账号', userLink(item.applicantId, item.applicantName, item.applicantUsername)],
        ['提交时间', fmtTime(item.createdAt)],
        ['状态', statusNode(item.status)],
        ['处理时间', item.reviewedAt ? fmtTime(item.reviewedAt) : null]
      ]));
      body.push(section('备注', h('div', { class: 'content-block', text: item.note || '（未填写）' })));
      if (item.status === 'pending') {
        function act(status, label) {
          ctx.run(api('/community/volunteer/applications/' + encodeURIComponent(item.id), { method: 'PATCH', json: { status: status } }), label);
        }
        ctx.keys = { a: function () { act('approved', '已通过报名'); }, x: function () { act('rejected', '已拒绝报名'); } };
        body.push(section('处理', h('div', { class: 'actions' }, [
          h('button', { class: 'btn btn-good', onclick: function () { act('approved', '已通过报名'); } }, ['通过', h('kbd', { text: 'A' })]),
          h('button', { class: 'btn btn-danger', onclick: function () { act('rejected', '已拒绝报名'); } }, ['拒绝', h('kbd', { text: 'X' })])
        ])));
      }
      return body;
    }
  };

  /* 义工招募 */
  views.recruitments = {
    title: '义工招募',
    load: function () {
      return api('/admin/volunteer/posts').then(function (data) {
        return { items: data.posts || [], counts: {} };
      });
    },
    tools: function () {
      return [h('button', { class: 'btn btn-primary', onclick: function () { navigate('recruitments', withParams({ id: 'new' })); } }, '新建招募')];
    },
    row: function (item) {
      return {
        title: item.title,
        text: excerpt(item.body, 120),
        meta: [item.deadlineAt ? '截止 ' + fmtTime(item.deadlineAt) : '长期', item.pendingApplications ? item.pendingApplications + ' 条待审报名' : '', item.approvedApplications ? item.approvedApplications + ' 人已通过' : ''],
        time: item.createdAt,
        status: item.status
      };
    },
    detail: function (item, ctx) {
      if (item.id === 'new') return recruitmentForm(ctx);
      var body = [];
      if (item.imageUrl) {
        var key = assetKeyFromUrl(item.imageUrl);
        var stage = h('div', { class: 'stage' });
        if (key) imageInto(stage, key, 1024, '招募封面');
        body.push(stage);
      }
      body.push(section('招募说明', h('div', { class: 'content-block', text: item.body })));
      body.push(metaList([
        ['状态', statusNode(item.status)],
        ['联系方式', item.contact || '未填写'],
        ['截止时间', item.deadlineAt ? fmtTime(item.deadlineAt) : '长期招募'],
        ['发布时间', fmtTime(item.createdAt)],
        ['发布人', item.adminUsername || null],
        ['待审报名', String(item.pendingApplications || 0)],
        ['已通过', String(item.approvedApplications || 0)]
      ]));
      var next = item.status === 'open' ? 'closed' : 'open';
      body.push(section('处理', h('div', { class: 'form' }, [
        h('div', { class: 'actions' }, [
          h('button', { class: next === 'closed' ? 'btn btn-danger' : 'btn btn-good', onclick: function () {
            ctx.run(api('/community/volunteer/' + encodeURIComponent(item.id), { method: 'PATCH', json: { status: next } }), next === 'closed' ? '已截止招募' : '已重新开放');
          } }, next === 'closed' ? '截止招募' : '重新开放'),
          h('a', { class: 'btn', href: hashFor('applications', { status: 'pending' }) }, '查看报名')
        ]),
        h('p', { class: 'actions-note', text: '截止后 App 里仍能看到这条招募，但不能再报名。重新开放会清除已经过期的截止时间。' })
      ])));
      return body;
    }
  };

  function recruitmentForm(ctx) {
    var title = h('input', { class: 'input', maxlength: '40', placeholder: '例如：故事整理义工' });
    var text = h('textarea', { class: 'textarea', maxlength: '500', placeholder: '写清楚要做什么、时间要求和参与方式（最多 500 字）' });
    var contact = h('input', { class: 'input', maxlength: '160', placeholder: '联系方式，可选' });
    var deadline = h('input', { class: 'input', type: 'datetime-local' });
    var cover = h('input', { class: 'input', type: 'file', accept: 'image/png,image/jpeg,image/webp' });
    var submit = h('button', { class: 'btn btn-primary' }, '发布招募');
    submit.addEventListener('click', function () {
      if (!title.value.trim()) { toast('请填写标题', true); title.focus(); return; }
      if (!text.value.trim()) { toast('请填写招募说明', true); text.focus(); return; }
      var deadlineAt = deadline.value ? new Date(deadline.value).toISOString() : '';
      submit.disabled = true;
      var flow = Promise.resolve(null);
      var file = cover.files && cover.files[0];
      if (file) {
        var form = new FormData();
        form.append('scope', 'community/volunteer');
        form.append('file', file, file.name);
        flow = api('/assets', { method: 'POST', body: form }).then(function (data) {
          var asset = data.asset || {};
          if (asset.reviewStatus === 'approved') return asset.url;
          return api('/admin/upload-reviews/' + encodeURIComponent(asset.reviewId), { method: 'PATCH', json: { status: 'approved', reason: '' } })
            .then(function () { return asset.url; });
        });
      }
      var run = flow.then(function (imageUrl) {
        return api('/community/volunteer', { method: 'POST', json: {
          title: title.value.trim(), body: text.value.trim(), contact: contact.value.trim(), deadlineAt: deadlineAt, imageUrl: imageUrl || ''
        } });
      });
      run.catch(function () { submit.disabled = false; });
      ctx.run(run, '招募已发布', { selectId: null });
    });
    return [h('div', { class: 'form' }, [
      h('label', null, ['标题', title]),
      h('label', null, ['招募说明', text]),
      h('label', null, ['联系方式', contact]),
      h('label', null, ['截止时间（留空为长期招募）', deadline]),
      h('label', null, ['封面图片（可选，会以当前管理员身份上传并直接通过审核）', cover]),
      h('div', { class: 'actions' }, [submit, h('a', { class: 'btn', href: hashFor('recruitments', {}) }, '取消')])
    ])];
  }

  /* 注销申请 */
  views.deletions = {
    title: '注销申请',
    queue: 'deletionRequests',
    filterParam: 'status',
    filters: [['pending', '待处理'], ['processing', '处理中'], ['completed', '已完成'], ['rejected', '已拒绝'], ['all', '全部']],
    defaultFilter: 'pending',
    load: function (params) {
      return api('/admin/account-deletion-requests' + query({ status: params.status || 'pending' })).then(function (data) {
        return { items: data.requests || [], counts: data.counts || {} };
      });
    },
    row: function (item) {
      return {
        title: item.username,
        text: excerpt(item.reason, 120),
        meta: [item.contact || '未留联系方式', item.user_id ? '账号存在' : '账号已不存在'],
        time: item.created_at,
        status: item.status
      };
    },
    detail: function (item, ctx) {
      var body = [];
      body.push(metaList([
        ['申请账号', item.username],
        ['联系方式', item.contact || '未填写'],
        ['提交时间', fmtTime(item.created_at)],
        ['更新时间', fmtTime(item.updated_at)],
        ['申请状态', statusNode(item.status)],
        ['账号', item.user_id ? userLink(item.user_id, item.user_display_name, item.username) : '已不存在（可能已注销）']
      ]));
      body.push(section('补充说明', h('div', { class: 'content-block', text: item.reason || '（未填写）' })));
      function setStatus(status, label) {
        ctx.run(api('/admin/account-deletion-requests/' + encodeURIComponent(item.id), { method: 'PATCH', json: { status: status } }), label);
      }
      var actions = [];
      if (item.status !== 'processing' && item.status !== 'completed') actions.push(h('button', { class: 'btn', onclick: function () { setStatus('processing', '已标记为处理中'); } }, '标记处理中'));
      if (item.status !== 'completed') actions.push(h('button', { class: 'btn btn-good', onclick: function () { setStatus('completed', '已标记为完成'); } }, '标记已完成'));
      if (item.status !== 'rejected' && item.status !== 'completed') actions.push(h('button', { class: 'btn btn-danger', onclick: function () { setStatus('rejected', '已拒绝申请'); } }, '拒绝申请'));
      body.push(section('申请状态', h('div', { class: 'form' }, [
        h('div', { class: 'actions' }, actions),
        h('p', { class: 'actions-note', text: '状态只用于记录进度。核实账号归属后，在下方注销账号，会自动把申请标记为已完成。' })
      ])));
      if (item.user_id && item.user_role !== 'admin' && item.status !== 'completed') {
        body.push(section('注销账号', dangerDelete({ id: item.user_id, username: item.username }, item.id, ctx)));
      }
      return body;
    }
  };

  /* 动态形象生成 */
  views.live2d = {
    title: '动态形象生成',
    queue: 'live2dActive',
    filterParam: 'status',
    filters: [['active', '进行中'], ['succeeded', '已完成'], ['failed', '失败与取消'], ['all', '全部']],
    defaultFilter: 'active',
    load: function (params) {
      return api('/admin/live2d/jobs' + query({ status: params.status || 'active' })).then(function (data) {
        var counts = data.counts || {};
        return { items: data.jobs || [], counts: {
          active: (counts.queued || 0) + (counts.running || 0),
          succeeded: counts.succeeded || 0,
          failed: (counts.failed || 0) + (counts.cancelled || 0)
        } };
      });
    },
    row: function (item) {
      return {
        title: (item.companion_name || '对象已删除') + '，' + (item.display_name || item.username || ''),
        text: excerpt(item.prompt, 100),
        meta: [(STAGE_LABELS[item.stage] || item.stage) + (item.status === 'running' ? ' ' + item.progress + '%' : ''), item.attempts > 1 ? '第 ' + item.attempts + ' 次尝试' : '', DIAGNOSIS_LABELS[item.diagnosis_code] || ''],
        time: item.updated_at,
        timeLabel: '更新',
        status: item.status
      };
    },
    detail: function (item) {
      var body = [];
      body.push(h('div', { class: 'progress' }, [h('i', { style: { width: Math.max(0, Math.min(100, Number(item.progress || 0))) + '%' } })]));
      body.push(metaList([
        ['状态', statusNode(item.status)],
        ['阶段', (STAGE_LABELS[item.stage] || item.stage) + '，' + Number(item.progress || 0) + '%'],
        ['用户', userLink(item.user_id, item.display_name, item.username)],
        ['陪伴对象', item.companion_name || '已删除'],
        ['尝试次数', String(item.attempts || 0)],
        ['错误码', item.error_code || null],
        ['诊断', item.diagnosis_code ? (DIAGNOSIS_LABELS[item.diagnosis_code] || item.diagnosis_code) : null],
        ['建议', item.suggestion ? (SUGGESTION_LABELS[item.suggestion] || item.suggestion) : null],
        ['制作端摘要', item.supervisor_summary || null],
        ['租约到期', item.lease_until ? fmtTime(item.lease_until) : null],
        ['创建时间', fmtTime(item.created_at)],
        ['更新时间', fmtTime(item.updated_at)],
        ['任务编号', item.id, 'mono']
      ]));
      body.push(section('用户提示词', h('div', { class: 'content-block', text: item.prompt || '（无）' })));
      body.push(h('p', { class: 'actions-note', text: '生成任务由用户在 App 里取消或重试，后台只查看。运行中的任务如果租约过期，制作端会自动重新领取，最多 3 次。' }));
      return body;
    }
  };

  /* 崩溃日志 */
  views.crashes = {
    title: '崩溃日志',
    filterParam: 'type',
    dynamicFilters: true,
    load: function () {
      return api('/admin/crash-reports').then(function (data) {
        var reports = data.reports || [];
        var groups = {};
        reports.forEach(function (row) { groups[row.error_type] = (groups[row.error_type] || 0) + 1; });
        var filters = [['', '全部']].concat(Object.keys(groups).sort(function (a, b) { return groups[b] - groups[a]; }).slice(0, 6).map(function (type) {
          return [type, shortType(type)];
        }));
        return { items: reports, counts: groups, filters: filters, all: reports.length };
      });
    },
    filterItems: function (items, params) {
      return params.type ? items.filter(function (row) { return row.error_type === params.type; }) : items;
    },
    row: function (item) {
      return {
        title: shortType(item.error_type),
        text: excerpt(item.message, 140),
        meta: [item.app_version ? '版本 ' + item.app_version : '', item.device_model || '', item.os_version ? 'Android ' + item.os_version : '', item.username || ''],
        time: item.created_at,
        status: null
      };
    },
    detail: function (item) {
      return [
        metaList([
          ['异常类型', item.error_type],
          ['应用版本', item.app_version || null],
          ['设备', item.device_model || null],
          ['系统', item.os_version || null],
          ['平台', item.platform],
          ['用户', item.user_id ? userLink(item.user_id, item.display_name, item.username) : '未登录或已注销'],
          ['时间', fmtTime(item.created_at)]
        ]),
        section('错误信息', h('div', { class: 'content-block', text: item.message || '（无）' })),
        section('堆栈', h('pre', { class: 'json', text: item.stack_trace || '（无）' }))
      ];
    }
  };

  function shortType(type) {
    type = String(type || '');
    var index = type.lastIndexOf('.');
    return index >= 0 ? type.slice(index + 1) : type;
  }

  /* 审计日志 */
  views.audit = {
    title: '审计日志',
    filterParam: 'action',
    filters: [['', '全部'], ['admin.', '管理员操作'], ['community.', '社区'], ['user.', '用户账号']],
    defaultFilter: '',
    search: true,
    searchPlaceholder: '按操作者用户名筛选',
    load: function (params) {
      return api('/admin/audit-logs' + query({ action: params.action || '', actor: params.q || '' })).then(function (data) {
        return { items: data.logs || [], counts: {} };
      });
    },
    row: function (item) {
      var meta = parseJsonSafe(item.metadata_json || '{}', {});
      var bits = [];
      if (meta.status) bits.push(statusText(meta.status));
      if (meta.action) bits.push(meta.action);
      if (meta.reason) bits.push(excerpt(meta.reason, 60));
      return {
        title: actionLabel(item.action),
        text: bits.join('，'),
        meta: [item.actor_username || (item.actor_role === 'admin' ? '管理员' : '系统'), TARGET_LABELS[item.target_type] || item.target_type, item.ip || ''],
        time: item.created_at,
        status: null
      };
    },
    detail: function (item) {
      var meta = parseJsonSafe(item.metadata_json || '{}', {});
      return [
        metaList([
          ['操作', actionLabel(item.action)],
          ['操作者', userLink(item.actor_id, item.actor_display_name, item.actor_username) || null],
          ['角色', item.actor_role === 'admin' ? '管理员' : (item.actor_role || '未登录')],
          ['对象类型', TARGET_LABELS[item.target_type] || item.target_type],
          ['对象编号', item.target_id || null, 'mono'],
          ['时间', fmtTime(item.created_at)],
          ['IP', item.ip || null],
          ['客户端', item.user_agent || null]
        ]),
        section('详细信息', h('pre', { class: 'json', text: JSON.stringify(meta, null, 2) }))
      ];
    }
  };

  /* 文件删除队列 */
  views.assets = {
    title: '文件删除队列',
    queue: 'assetDeletes',
    filterParam: 'status',
    filters: [['pending', '待删除'], ['failed', '失败'], ['deleted', '已删除'], ['all', '全部']],
    defaultFilter: 'pending',
    load: function (params) {
      return api('/admin/asset-delete-queue' + query({ status: params.status || 'pending' })).then(function (data) {
        return { items: data.items || [], counts: data.counts || {} };
      });
    },
    tools: function (ctx) {
      var pending = (state.counts && state.counts.pending) || 0;
      var button = h('button', { class: 'btn btn-primary', disabled: !pending, onclick: function () {
        button.disabled = true;
        api('/admin/asset-delete-queue/process', { method: 'POST', json: {} }).then(function (data) {
          toast('处理 ' + data.attempted + ' 条，删除 ' + data.deleted + ' 条');
          ctx.reload();
        }).catch(function (error) { toast(errorText(error), true); button.disabled = false; });
      } }, '处理待删除文件' + (pending ? '（' + pending + '）' : ''));
      return [button];
    },
    row: function (item) {
      return {
        title: scopeLabel(item.asset_key),
        text: item.error_message ? reasonLabel(item.error_message) : '',
        meta: [reasonLabel(item.reason), item.processed_at ? '处理于 ' + fmtTime(item.processed_at) : ''],
        time: item.created_at,
        status: item.status
      };
    },
    detail: function (item, ctx) {
      var body = [
        metaList([
          ['状态', statusNode(item.status)],
          ['用途', scopeLabel(item.asset_key)],
          ['入队原因', reasonLabel(item.reason)],
          ['入队时间', fmtTime(item.created_at)],
          ['处理时间', item.processed_at ? fmtTime(item.processed_at) : null],
          ['失败原因', item.error_message ? reasonLabel(item.error_message) : null],
          ['所属用户', item.owner_id ? userLink(item.owner_id, null, null) : '已注销'],
          ['文件键', item.asset_key, 'mono']
        ]),
        h('p', { class: 'actions-note', text: '每次处理最多 50 条待删除记录。仍被头像、背景或形象引用的文件会标记为失败并保留，引用解除后可以重新入队。' })
      ];
      if (item.status === 'failed') {
        body.push(section('处理', h('div', { class: 'actions' }, [
          h('button', { class: 'btn btn-primary', onclick: function () {
            ctx.run(api('/admin/asset-delete-queue/' + encodeURIComponent(item.id) + '/retry', { method: 'POST', json: {} }), '已重新入队');
          } }, '重新加入待删除')
        ])));
      }
      return body;
    }
  };

  var RAIL = [
    ['队列', [['uploads', '上传审核', 'uploads'], ['community', '社区内容', 'community'], ['reports', '举报处理', 'reports'], ['applications', '义工报名', 'volunteerApplications'], ['deletions', '注销申请', 'deletionRequests'], ['assets', '文件删除', 'assetDeletes']]],
    ['记录', [['users', '用户', 'moderatedUsers'], ['recruitments', '义工招募', null], ['live2d', '动态形象', 'live2dActive'], ['crashes', '崩溃日志', null], ['audit', '审计日志', null]]]
  ];

  /* ------------------------------------------------------------------ 渲染 */

  function render() {
    if (!session) { renderLogin(); return; }
    if (!refs.shell) renderShell();
    var view = views[state.view];
    refs.rail.querySelectorAll('.rail-item').forEach(function (node) {
      node.classList.toggle('active', node.dataset.view === state.view);
    });
    if (view.custom) {
      refs.work.className = 'work';
      refs.work.textContent = '';
      refs.work.appendChild(renderOverview());
      return;
    }
    renderListPane();
    renderDetailPane();
  }

  function renderLogin() {
    refs = {};
    root.textContent = '';
    var username = h('input', { class: 'input', autocomplete: 'username', autocapitalize: 'none', placeholder: '管理员账号' });
    var password = h('input', { class: 'input', type: 'password', autocomplete: 'current-password', placeholder: '密码' });
    var remember = h('input', { type: 'checkbox', class: 'check' });
    var message = h('div', { class: 'login-message' });
    var button = h('button', { class: 'btn btn-primary', type: 'submit' }, '登录');
    var form = h('form', { class: 'login-form', onsubmit: function (event) {
      event.preventDefault();
      message.textContent = '';
      button.disabled = true;
      fetch('/auth/login', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: username.value.trim(), password: password.value })
      }).then(function (res) {
        return res.text().then(function (text) {
          var data = text ? parseJsonSafe(text, {}) : {};
          if (!res.ok) throw new ApiError(res.status, data.error || 'request_failed');
          if (!data.user || data.user.role !== 'admin') throw new ApiError(403, 'admin_required');
          session = { token: data.token, user: data.user };
          saveSession(session, remember.checked);
          refs = {};
          route();
        });
      }).catch(function (error) {
        message.textContent = errorText(error);
        button.disabled = false;
      });
    } }, [
      h('label', null, ['账号', username]),
      h('label', null, ['密码', password]),
      h('div', { class: 'row' }, [
        h('label', { class: 'login-remember' }, [remember, '在这台电脑记住登录']),
        button
      ]),
      message
    ]);
    root.appendChild(h('div', { class: 'login' }, [
      h('div', { class: 'login-card' }, [
        h('div', { class: 'login-brand', text: '安忆' }),
        h('div', { class: 'login-sub', text: '管理后台。审核上传、社区与举报，处理用户与注销申请。' }),
        form,
        h('div', { class: 'login-foot', text: '只有由运维提升为管理员的账号可以登录。所有处理动作都会写入审计日志。' })
      ])
    ]));
    username.focus();
  }

  function renderShell() {
    root.textContent = '';
    var search = h('input', { class: 'input', type: 'search', placeholder: '搜索用户名或昵称，回车打开用户列表', 'aria-label': '搜索用户' });
    search.addEventListener('keydown', function (event) {
      if (event.key === 'Enter') {
        navigate('users', { q: search.value.trim(), filter: 'all' });
      }
    });
    refs.search = search;
    refs.rail = h('nav', { class: 'rail', 'aria-label': '后台导航' });
    refs.rail.appendChild(h('div', { class: 'rail-group' }, [
      h('a', { class: 'rail-item', href: '#/overview', dataset: { view: 'overview' } }, [h('span', { text: '总览' })])
    ]));
    RAIL.forEach(function (group) {
      var items = group[1].map(function (entry) {
        return h('a', { class: 'rail-item', href: '#/' + entry[0], dataset: { view: entry[0], queue: entry[2] || '' } }, [
          h('span', { text: entry[1] }),
          h('span', { class: 'count', text: '' })
        ]);
      });
      refs.rail.appendChild(h('div', { class: 'rail-group' }, [h('div', { class: 'rail-title', text: group[0] })].concat(items)));
    });
    refs.rail.appendChild(h('div', { class: 'rail-foot', text: '快捷键：J / K 上下选择，A 通过，X 拒绝，/ 搜索，Esc 关闭详情。' }));
    refs.work = h('main', { class: 'work' });
    refs.shell = h('div', { class: 'shell' }, [
      h('header', { class: 'top' }, [
        h('a', { class: 'brand', href: '#/overview' }, ['安忆', h('small', { text: '管理后台' })]),
        h('div', { class: 'global-search' }, [svgIcon('search'), search]),
        h('div', { class: 'top-spacer' }),
        h('div', { class: 'who' }, [
          h('span', { class: 'name' }, [h('b', { text: session.user.displayName || session.user.username }), '，管理员']),
          h('button', { class: 'btn btn-sm', onclick: function () { clearSession(); location.hash = '#/overview'; render(); } }, '退出')
        ])
      ]),
      refs.rail,
      refs.work
    ]);
    root.appendChild(refs.shell);
    refreshCounts();
  }

  function refreshCounts() {
    if (!session) return;
    api('/admin/overview').then(function (data) {
      state.overview = data;
      var queues = data.queues || {};
      refs.rail.querySelectorAll('.rail-item').forEach(function (node) {
        var key = node.dataset.queue;
        var count = node.querySelector('.count');
        if (!key || !count) return;
        var value = Number(queues[key] || 0);
        count.textContent = value ? String(value) : '';
        count.classList.toggle('due', value > 0 && key !== 'live2dActive' && key !== 'moderatedUsers');
      });
      if (state.view === 'overview' && refs.work) render();
    }).catch(function () {});
  }

  function currentFilter(view) {
    if (!view.filterParam) return '';
    var value = state.params[view.filterParam];
    return value === undefined ? (view.defaultFilter || '') : value;
  }

  function visibleItems() {
    var view = views[state.view];
    return view.filterItems ? view.filterItems(state.items, state.params) : state.items;
  }

  function renderListPane() {
    var view = views[state.view];
    var items = visibleItems();
    var hasDetail = Boolean(state.params.id);
    refs.work.className = 'work' + (hasDetail ? ' split' : '');
    refs.work.textContent = '';

    var ctx = listContext();
    var head = h('div', { class: 'pane-head' });
    var toolNodes = view.tools ? view.tools(ctx) : [];
    head.appendChild(h('div', { class: 'pane-title-row' }, [
      h('h1', { class: 'pane-title' }, [view.title, h('small', { text: state.loading ? '载入中' : (items.length ? '共 ' + items.length + ' 条' : '') })]),
      h('div', { class: 'pane-tools' }, toolNodes.concat([h('button', { class: 'btn btn-quiet btn-sm', onclick: function () { loadList(); } }, '刷新')]))
    ]));
    if (view.search) {
      var search = h('input', { class: 'input', type: 'search', placeholder: view.searchPlaceholder || '搜索用户名或昵称', value: state.params.q || '' });
      search.addEventListener('keydown', function (event) {
        if (event.key === 'Enter') navigate(state.view, withParams({ q: search.value.trim(), id: null }));
      });
      head.appendChild(search);
    }
    var filters = view.dynamicFilters ? (state.extra && state.extra.filters) || [] : view.filters;
    if (filters && filters.length) {
      var active = currentFilter(view);
      head.appendChild(h('div', { class: 'tabs', role: 'tablist' }, filters.map(function (pair) {
        var count = state.counts ? state.counts[pair[0]] : null;
        if (view.dynamicFilters && pair[0] === '') count = state.extra ? state.extra.all : null;
        return h('button', { class: 'tab' + (pair[0] === active ? ' active' : ''), role: 'tab', onclick: function () {
          var extra = { id: null };
          extra[view.filterParam] = pair[0];
          navigate(state.view, withParams(extra));
        } }, [pair[1], count ? h('span', { class: 'n', text: String(count) }) : null]);
      })));
    }
    if (view.bulk) {
      var checkedIds = Object.keys(state.checked).filter(function (id) { return state.checked[id]; });
      if (checkedIds.length) head.appendChild(bulkBar(checkedIds, ctx));
    }

    var list;
    if (state.error) {
      list = h('div', { class: 'error-box', text: errorText(state.error) });
    } else if (state.loading && !items.length) {
      list = h('div', { class: 'loading', text: '载入中' });
    } else if (!items.length) {
      list = emptyNode('这里没有待处理的内容', view.queue ? '队列已清空。' : '换个筛选条件试试。');
    } else {
      list = h('ul', { class: 'rows' }, items.map(function (item) { return renderRow(view, item); }));
    }
    var scroll = h('div', { class: 'list-scroll' }, [list]);
    refs.list = h('section', { class: 'pane pane-list', 'aria-label': view.title }, [head, scroll]);
    refs.work.appendChild(refs.list);
    var selected = refs.list.querySelector('.row.selected');
    if (selected) selected.scrollIntoView({ block: 'nearest' });
  }

  function renderRow(view, item) {
    var spec = view.row(item);
    var isSelected = state.params.id === item.id;
    var li = h('li', { class: 'row' + (isSelected ? ' selected' : ''), dataset: { id: item.id }, tabindex: '0', role: 'button' });
    var lead = h('div', { class: 'row-lead' });
    if (view.bulk && item.status === 'pending') {
      var check = h('input', { type: 'checkbox', class: 'check', checked: Boolean(state.checked[item.id]), 'aria-label': '选择这条记录' });
      check.addEventListener('click', function (event) { event.stopPropagation(); });
      check.addEventListener('change', function () {
        state.checked[item.id] = check.checked;
        if (!check.checked) delete state.checked[item.id];
        renderListPane();
      });
      lead.appendChild(check);
    } else if (spec.status) {
      lead.appendChild(h('i', { class: 'dot row-dot ' + dotTone(spec.status) }));
    }
    li.appendChild(lead);
    var main = h('div', { class: 'row-main' }, [
      h('div', { class: 'row-title' }, [spec.kind ? h('span', { class: 'row-kind', text: spec.kind }) : null, h('b', { text: spec.title || '' })]),
      spec.text ? h('div', { class: 'row-text', text: spec.text }) : null,
      h('div', { class: 'row-meta' }, (spec.meta || []).filter(Boolean).map(function (m) { return h('span', { text: m }); }))
    ]);
    li.appendChild(main);
    li.appendChild(h('div', { class: 'row-side' }, [
      spec.time ? h('span', { title: fmtTime(spec.time), text: (spec.timeLabel ? spec.timeLabel + ' ' : '') + timeAgo(spec.time) }) : null,
      spec.status ? statusNode(spec.status) : null
    ]));
    function open() { navigate(state.view, withParams({ id: item.id })); }
    li.addEventListener('click', open);
    li.addEventListener('keydown', function (event) { if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); open(); } });
    return li;
  }

  function dotTone(status) {
    var entry = STATUS[status];
    return entry ? entry[1] : '';
  }

  function bulkBar(ids, ctx) {
    var reason = h('input', { class: 'input bulk-reason', placeholder: '拒绝原因（批量拒绝时必填）' });
    function run(status) {
      var text = reason.value.trim();
      if (status !== 'approved' && !text) { toast('请填写拒绝原因', true); reason.focus(); return; }
      ctx.run(api('/admin/upload-reviews/batch', { method: 'POST', json: { ids: ids, status: status, reason: text } }).then(function (data) {
        var failed = (data.results || []).filter(function (r) { return !r.ok; }).length;
        if (failed) toast(failed + ' 条没有处理成功（可能已被处理）', true);
        return data;
      }), (status === 'approved' ? '已通过 ' : '已拒绝 ') + ids.length + ' 条', { clearChecked: true });
    }
    return h('div', { class: 'bulkbar' }, [
      h('span', { text: '已选 ' + ids.length + ' 条' }),
      h('button', { class: 'btn btn-good btn-sm', onclick: function () { run('approved'); } }, '全部通过'),
      h('button', { class: 'btn btn-danger btn-sm', onclick: function () { run('rejected'); } }, '全部拒绝'),
      reason,
      h('button', { class: 'btn btn-quiet btn-sm', onclick: function () { state.checked = {}; renderListPane(); } }, '取消选择')
    ]);
  }

  function listContext() {
    return {
      reload: function () { loadList(true); refreshCounts(); },
      run: function (promise, label, options) {
        options = options || {};
        return promise.then(function () {
          toast(label);
          if (options.clearChecked) state.checked = {};
          afterAction(options);
        }).catch(function (error) { toast(errorText(error), true); });
      }
    };
  }

  function afterAction(options) {
    var items = visibleItems();
    var index = -1;
    for (var i = 0; i < items.length; i += 1) if (items[i].id === state.params.id) { index = i; break; }
    var nextId = null;
    if (options.selectId !== undefined) nextId = options.selectId;
    else if (index >= 0 && items[index + 1]) nextId = items[index + 1].id;
    else if (index > 0) nextId = items[index - 1].id;
    delete state.detailCache[state.params.id];
    state.selectAfterLoad = nextId;
    loadList(true);
    refreshCounts();
  }

  function loadList(quiet) {
    var view = views[state.view];
    if (!view || view.custom) return;
    var token = ++state.loadToken;
    state.loading = true;
    state.error = null;
    if (!quiet) { state.items = []; state.counts = {}; state.extra = null; }
    render();
    view.load(state.params).then(function (result) {
      if (token !== state.loadToken) return;
      state.loading = false;
      state.items = result.items || [];
      state.counts = result.counts || {};
      state.extra = result;
      var exists = state.params.id && (state.params.id === 'new' || state.items.some(function (item) { return item.id === state.params.id; }));
      if (state.selectAfterLoad !== undefined) {
        var next = state.selectAfterLoad;
        state.selectAfterLoad = undefined;
        if (next && state.items.some(function (item) { return item.id === next; })) {
          navigate(state.view, withParams({ id: next }), true);
          return;
        }
        if (!exists) { navigate(state.view, withParams({ id: null }), true); return; }
      } else if (state.params.id && !exists) {
        navigate(state.view, withParams({ id: null }), true);
        return;
      }
      render();
    }).catch(function (error) {
      if (token !== state.loadToken) return;
      state.loading = false;
      state.error = error;
      render();
    });
  }

  var detailCtx = null;

  function renderDetailPane() {
    detailCtx = null;
    var view = views[state.view];
    if (!state.params.id) return;
    var item = state.params.id === 'new' ? { id: 'new' } : null;
    if (!item) {
      for (var i = 0; i < state.items.length; i += 1) if (state.items[i].id === state.params.id) { item = state.items[i]; break; }
    }
    var pane = h('aside', { class: 'pane pane-detail', 'aria-label': '详情' });
    var close = h('button', { class: 'btn btn-quiet btn-sm', title: '关闭（Esc）', 'aria-label': '关闭详情', onclick: function () { navigate(state.view, withParams({ id: null })); } }, [svgIcon('close')]);
    pane.appendChild(h('div', { class: 'detail-head' }, [h('h2', { text: state.params.id === 'new' ? '新建招募' : view.title }), close]));
    var body = h('div', { class: 'detail-body' });
    pane.appendChild(body);
    refs.work.appendChild(pane);
    if (!item) {
      body.appendChild(state.loading ? h('div', { class: 'loading', text: '载入中' }) : h('div', { class: 'empty' }, '这条记录不在当前列表里。'));
      return;
    }
    var ctx = listContext();
    ctx.keys = null;
    detailCtx = ctx;
    function paint(data) {
      body.textContent = '';
      append(body, view.detail(item, ctx, data));
      var hints = [['J', '下一条'], ['K', '上一条'], ['Esc', '关闭']];
      if (ctx.keys) {
        if (ctx.keys.a) hints.unshift(['A', '通过或保留']);
        if (ctx.keys.x) hints.splice(1, 0, ['X', '拒绝或移除']);
      }
      body.appendChild(keysHint(hints));
    }
    if (view.detailLoad) {
      var cached = state.detailCache[item.id];
      if (cached) { paint(cached); return; }
      body.appendChild(h('div', { class: 'loading', text: '载入中' }));
      view.detailLoad(item).then(function (data) {
        if (state.params.id !== item.id) return;
        state.detailCache[item.id] = data;
        paint(data);
      }).catch(function (error) {
        body.textContent = '';
        body.appendChild(h('div', { class: 'error-box', text: errorText(error) }));
      });
    } else {
      paint(null);
    }
  }

  /* ------------------------------------------------------------------ 总览 */

  function renderOverview() {
    var data = state.overview;
    var wrap = h('div', { class: 'overview' });
    if (!data) {
      wrap.appendChild(h('div', { class: 'loading', text: '载入中' }));
      return wrap;
    }
    var q = data.queues || {};
    var totals = data.totals || {};
    var services = data.services || {};
    var speech = services.speechToday || {};
    wrap.appendChild(h('div', { class: 'ov-head' }, [
      h('h1', { class: 'pane-title', text: '总览' }),
      h('div', { class: 'totals' }, [
        h('span', null, [h('b', { text: fmtNumber(totals.users) }), '用户']),
        h('span', null, [h('b', { text: fmtNumber(totals.memorials) }), '纪念馆']),
        h('span', null, [h('b', { text: fmtNumber(totals.companions) }), '陪伴对象']),
        h('span', null, [h('b', { text: fmtNumber(totals.posts) }), '公开动态'])
      ])
    ]));

    var rows = [
      ['uploads', '上传审核', q.uploads, '图片、音频等待人工审核', { status: 'pending' }],
      ['community', '社区内容', q.community, (q.communityPosts || 0) + ' 条动态，' + (q.communityComments || 0) + ' 条评论待审', { status: 'pending' }],
      ['reports', '举报处理', q.reports, '用户举报的动态和评论', { status: 'pending' }],
      ['applications', '义工报名', q.volunteerApplications, '等待审核的报名表', { status: 'pending' }],
      ['deletions', '注销申请', q.deletionRequests, q.deletionRequestsProcessing ? '另有 ' + q.deletionRequestsProcessing + ' 条处理中' : '网页表单提交的注销请求', { status: 'pending' }],
      ['assets', '文件删除', q.assetDeletes, q.assetDeletesFailed ? '另有 ' + q.assetDeletesFailed + ' 条删除失败' : '拒绝或注销后待清理的文件', { status: 'pending' }],
      ['live2d', '动态形象生成', q.live2dActive, (q.live2dQueued || 0) + ' 条排队，' + (q.live2dRunning || 0) + ' 条生成中' + (q.live2dFailedRecently ? '，近两周失败 ' + q.live2dFailedRecently + ' 条' : ''), { status: 'active' }],
      ['users', '处置中的用户', q.moderatedUsers, '被屏蔽或封禁、尚未到期的账号', { filter: 'moderated' }]
    ];
    var table = h('table', { class: 'ledger' }, [
      h('thead', null, [h('tr', null, [h('th', { text: '队列' }), h('th', { text: '说明' }), h('th', { class: 'num', text: '待处理' }), h('th', { class: 'act' })])]),
      h('tbody', null, rows.map(function (row) {
        var value = Number(row[2] || 0);
        var informational = row[0] === 'live2d' || row[0] === 'users';
        return h('tr', null, [
          h('td', null, [h('a', { href: hashFor(row[0], row[4]), text: row[1] })]),
          h('td', { class: 'hint', text: row[3] }),
          h('td', { class: 'num' + (value === 0 ? ' zero' : (informational ? '' : ' due')) }, [h('b', { text: String(value) })]),
          h('td', { class: 'act' }, [value ? h('a', { class: 'btn btn-sm', href: hashFor(row[0], row[4]) }, informational ? '查看' : '去处理') : null])
        ]);
      }))
    ]);
    wrap.appendChild(table);

    var activity = data.activity || { days: [] };
    var charts = h('div', { class: 'charts' }, [
      barChart('新注册用户', activity.days, activity.users),
      barChart('社区动态', activity.days, activity.posts),
      barChart('AI 对话消息', activity.days, activity.aiMessages),
      barChart('崩溃上报', activity.days, activity.crashes)
    ]);
    var left = h('div', { class: 'panel' }, [
      h('div', { class: 'panel-title' }, ['近 14 天', h('small', { text: '按北京时间统计，最右一根是今天' })]),
      charts
    ]);
    var servicesPanel = h('div', { class: 'panel' }, [
      h('div', { class: 'panel-title' }, ['服务状态']),
      h('div', { class: 'services' }, [h('ul', null, [
        serviceRow('数据库', services.database === 'mysql' ? 'MySQL' : 'SQLite'),
        serviceRow('语音识别', services.voiceRecognition ? '已开启' : '关闭'),
        serviceRow('语音合成', services.speech ? '已开启' : '关闭'),
        serviceRow('动态形象生成', services.live2d ? '已开启' : '关闭'),
        serviceRow('微信登录', services.wechatLogin ? '已配置' : '未配置'),
        serviceRow('对话模型', services.chatModel || ''),
        serviceRow('记忆整理模型', services.memoryModel || ''),
        serviceRow('今日语音合成', fmtNumber(speech.characters) + ' 字，' + fmtNumber(speech.users) + ' 人，每人上限 ' + fmtNumber(speech.limitPerUser))
      ])])
    ]);
    var auditPanel = h('div', { class: 'panel' }, [
      h('div', { class: 'panel-title' }, ['最近操作', h('a', { href: '#/audit', text: '全部审计日志' })]),
      data.recentAudit && data.recentAudit.length ? auditList(data.recentAudit) : h('p', { class: 'actions-note', text: '还没有记录。' })
    ]);
    wrap.appendChild(h('div', { class: 'ov-grid' }, [left, h('div', { class: 'ov-stack' }, [servicesPanel, auditPanel])]));
    return wrap;
  }

  function serviceRow(label, value) {
    return h('li', null, [h('span', { class: 'k', text: label }), h('span', { text: value })]);
  }

  var tipNode = null;
  function showTip(x, y, text) {
    if (!tipNode) { tipNode = h('div', { class: 'tip' }); doc.body.appendChild(tipNode); }
    tipNode.textContent = text;
    tipNode.style.left = (x + 12) + 'px';
    tipNode.style.top = (y - 30) + 'px';
    tipNode.classList.remove('hidden');
  }
  function hideTip() { if (tipNode) tipNode.classList.add('hidden'); }

  function barChart(title, days, values) {
    values = values || [];
    days = days || [];
    var total = values.reduce(function (sum, v) { return sum + Number(v || 0); }, 0);
    var max = Math.max(1, Math.max.apply(null, values.map(function (v) { return Number(v || 0); })));
    var width = 280, height = 64, gap = 2;
    var n = Math.max(1, days.length);
    var barW = (width - gap * (n - 1)) / n;
    var ns = 'http://www.w3.org/2000/svg';
    var svg = doc.createElementNS(ns, 'svg');
    svg.setAttribute('viewBox', '0 0 ' + width + ' ' + (height + 14));
    svg.setAttribute('role', 'img');
    svg.setAttribute('aria-label', title + '，近 14 天共 ' + total);
    var base = doc.createElementNS(ns, 'line');
    base.setAttribute('x1', '0'); base.setAttribute('x2', String(width));
    base.setAttribute('y1', String(height)); base.setAttribute('y2', String(height));
    base.setAttribute('class', 'baseline');
    svg.appendChild(base);
    days.forEach(function (day, index) {
      var value = Number(values[index] || 0);
      var barH = value === 0 ? 0 : Math.max(2, (value / max) * (height - 4));
      var rect = doc.createElementNS(ns, 'rect');
      rect.setAttribute('x', String(index * (barW + gap)));
      rect.setAttribute('y', String(height - barH));
      rect.setAttribute('width', String(barW));
      rect.setAttribute('height', String(barH));
      rect.setAttribute('rx', '1.5');
      rect.setAttribute('class', 'bar' + (index === days.length - 1 ? ' today' : ''));
      var hit = doc.createElementNS(ns, 'rect');
      hit.setAttribute('x', String(index * (barW + gap)));
      hit.setAttribute('y', '0');
      hit.setAttribute('width', String(barW + gap));
      hit.setAttribute('height', String(height));
      hit.setAttribute('class', 'hit-area');
      hit.addEventListener('mousemove', function (event) { rect.classList.add('hit'); showTip(event.clientX, event.clientY, day.slice(5) + '　' + value); });
      hit.addEventListener('mouseleave', function () { rect.classList.remove('hit'); hideTip(); });
      svg.appendChild(rect);
      svg.appendChild(hit);
      if (index === 0 || index === days.length - 1) {
        var label = doc.createElementNS(ns, 'text');
        label.setAttribute('x', String(index === 0 ? 0 : width));
        label.setAttribute('y', String(height + 11));
        label.setAttribute('text-anchor', index === 0 ? 'start' : 'end');
        label.setAttribute('class', 'axis');
        label.textContent = day.slice(5);
        svg.appendChild(label);
      }
    });
    var table = h('details', { class: 'chart-table' }, [
      h('summary', { text: '查看数据' }),
      h('table', null, [h('tbody', null, days.map(function (day, index) {
        return h('tr', null, [h('td', { text: day }), h('td', { text: String(values[index] || 0) })]);
      }))])
    ]);
    return h('div', { class: 'chart' }, [
      h('div', { class: 'chart-head' }, [h('b', { text: title }), h('span', { text: '合计 ' + fmtNumber(total) })]),
      svg,
      table
    ]);
  }

  /* -------------------------------------------------------------- 键盘操作 */

  function moveSelection(delta) {
    var items = visibleItems();
    if (!items.length) return;
    var index = -1;
    for (var i = 0; i < items.length; i += 1) if (items[i].id === state.params.id) { index = i; break; }
    var next = index < 0 ? (delta > 0 ? 0 : items.length - 1) : Math.max(0, Math.min(items.length - 1, index + delta));
    navigate(state.view, withParams({ id: items[next].id }));
  }

  doc.addEventListener('keydown', function (event) {
    if (!session) return;
    var target = event.target;
    var tag = target && target.tagName ? target.tagName.toLowerCase() : '';
    var typing = tag === 'input' || tag === 'textarea' || tag === 'select' || (target && target.isContentEditable);
    if (event.key === 'Escape') {
      if (typing) { target.blur(); return; }
      if (state.params.id) navigate(state.view, withParams({ id: null }));
      return;
    }
    if (typing || event.metaKey || event.ctrlKey || event.altKey) return;
    var view = views[state.view];
    if (event.key === '/') { event.preventDefault(); refs.search.focus(); refs.search.select(); return; }
    if (!view || view.custom) return;
    if (event.key === 'j' || event.key === 'ArrowDown') { event.preventDefault(); moveSelection(1); return; }
    if (event.key === 'k' || event.key === 'ArrowUp') { event.preventDefault(); moveSelection(-1); return; }
    if (detailCtx && detailCtx.keys && detailCtx.keys[event.key]) { event.preventDefault(); detailCtx.keys[event.key](); }
  });

  /* ------------------------------------------------------------------ 启动 */

  function route() {
    if (!session) { render(); return; }
    var parsed = parseHash();
    var viewChanged = parsed.view !== state.view;
    var previous = state.params;
    state.view = parsed.view;
    state.params = parsed.params;
    var view = views[state.view];
    if (view.custom) { render(); return; }
    var filterChanged = view.filterParam && previous[view.filterParam] !== parsed.params[view.filterParam];
    var searchChanged = view.search && previous.q !== parsed.params.q;
    if (viewChanged || filterChanged || searchChanged) {
      state.checked = {};
      state.detailCache = {};
      loadList(false);
      return;
    }
    render();
  }

  window.addEventListener('hashchange', route);

  session = loadSession();
  if (session) {
    api('/me').then(function (data) {
      if (!data.user || data.user.role !== 'admin') { clearSession(); render(); return; }
      session.user = data.user;
      route();
    }).catch(function () { if (session) route(); });
  } else {
    render();
  }
})();
`;
