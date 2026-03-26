/*
 * Licensed under the Apache License, Version 2.0
 *
 * Gateway Admin Dashboard — Vue 3 app logic (no PrimeVue dependency)
 *
 * Served as a static classpath resource at /gateway-ui/app.js.
 * Loaded by index.html after Vue 3 has been loaded from CDN.
 */
(function () {
  'use strict';

  /* ── 1. Form helpers ────────────────────────────────────────────────────── */
  function emptyForm() {
    return {
      id: null,
      name: '',
      'path-pattern': '',
      'routing-type': 'PATH',
      'strip-prefix': '',
      enabled: true,
      'timeout-ms': 5000,
      'load-balancer-type': 'ROUND_ROBIN',
      targets: [{ url: '', weight: 1 }],
      'rate-limit-policy': {
        enabled: false,
        'requests-per-second': 100,
        burst: 20,
        'timeout-duration-ms': 0,
      },
      'circuit-breaker-policy': {
        enabled: false,
        'failure-rate-threshold': 50,
        'wait-duration-seconds': 60,
        'sliding-window-size': 10,
      },
      'cache-policy': {
        enabled: false,
        'ttl-seconds': 300,
        'cache-key-strategy': 'METHOD_PATH_QUERY',
      },
      'header-rules': {
        'add-request': {},
        'exclude-request': [],
        'add-response': {},
        'exclude-response': [],
        'dedupe-response-headers': [],
      },
      'auth-forward-headers': [],
      'audit-enabled': false,
      'audit-store': 'database',
      'header-match-name': '',
      'header-match-value': '',
    };
  }

  function routeToForm(route) {
    const base   = emptyForm();
    const copy   = JSON.parse(JSON.stringify(route));
    const merged = Object.assign(base, copy);
    merged['rate-limit-policy']      = Object.assign({}, base['rate-limit-policy'],      route['rate-limit-policy']      || {});
    merged['circuit-breaker-policy'] = Object.assign({}, base['circuit-breaker-policy'], route['circuit-breaker-policy'] || {});
    merged['cache-policy']           = Object.assign({}, base['cache-policy'],           route['cache-policy']           || {});
    merged['header-rules']           = Object.assign({}, base['header-rules'],           route['header-rules']           || {});
    merged['auth-forward-headers']   = Array.isArray(route['auth-forward-headers']) ? [...route['auth-forward-headers']] : [];
    merged.targets = Array.isArray(route.targets) && route.targets.length
      ? JSON.parse(JSON.stringify(route.targets))
      : [{ url: '', weight: 1 }];
    return merged;
  }

  /* ── 2. Vue application ─────────────────────────────────────────────────── */
  if (typeof Vue === 'undefined') {
    document.getElementById('app').innerHTML =
      '<div style="padding:2rem;color:#f87171;font-family:monospace">' +
      '⚠ Vue 3 failed to load from CDN. Check your network connection and reload.</div>';
    return;
  }

  const { createApp } = Vue;

  const app = createApp({
    data() {
      return {
        routes:         [],
        health:         null,
        loading:        false,
        search:         '',
        dlgVisible:     false,
        dlgMode:        'create',   // 'create' | 'edit'
        saving:         false,
        activeTab:      0,
        form:           emptyForm(),
        reqHdrEntries:  [],
        respHdrEntries: [],
        newExclReq:     '',
        newExclResp:    '',
        newAuth:        '',

        // Sort state
        sortField: 'name',
        sortDir:   'asc',

        // Pagination
        currentPage: 1,
        pageSize:    10,


        // Custom toast notifications
        notifications:  [],
        notifCounter:   0,

        // Custom confirm dialog
        confirmVisible:   false,
        confirmMsg:       '',
        confirmCallback:  null,

        // Options
        rtOpts: [
          { label: 'PATH',          value: 'PATH'          },
          { label: 'REGEX',         value: 'REGEX'         },
          { label: 'HEADER',        value: 'HEADER'        },
          { label: 'TRAFFIC_SPLIT', value: 'TRAFFIC_SPLIT' },
        ],
        lbOpts: [
          { label: 'Round Robin', value: 'ROUND_ROBIN' },
          { label: 'Weighted',    value: 'WEIGHTED'    },
          { label: 'Random',      value: 'RANDOM'      },
        ],
        ckOpts: [
          { label: 'Method + Path',         value: 'METHOD_PATH'       },
          { label: 'Method + Path + Query', value: 'METHOD_PATH_QUERY' },
        ],
        auditOpts: [
          { label: 'Database', value: 'database' },
          { label: 'File',     value: 'file'     },
        ],
      };
    },

    computed: {
      filteredRoutes() {
        let list = this.routes;

        // Text search
        if (this.search) {
          const q = this.search.toLowerCase();
          list = list.filter(r =>
            (r.name            || '').toLowerCase().includes(q) ||
            (r['path-pattern'] || '').toLowerCase().includes(q) ||
            (r['routing-type'] || '').toLowerCase().includes(q)
          );
        }

        // Sort
        const field = this.sortField;
        const dir   = this.sortDir === 'asc' ? 1 : -1;
        return [...list].sort((a, b) => {
          const av = (a[field] || '').toString().toLowerCase();
          const bv = (b[field] || '').toString().toLowerCase();
          return av < bv ? -dir : av > bv ? dir : 0;
        });
      },
      activeCount()   { return this.routes.filter(r =>  r.enabled).length; },
      disabledCount() { return this.routes.filter(r => !r.enabled).length; },
      healthLabel()   { return this.health?.status || 'UNKNOWN'; },


      totalPages() {
        return Math.max(1, Math.ceil(this.filteredRoutes.length / this.pageSize));
      },
      pagedRoutes() {
        const start = (this.currentPage - 1) * this.pageSize;
        return this.filteredRoutes.slice(start, start + this.pageSize);
      },
      visiblePages() {
        const total = this.totalPages;
        const cur   = this.currentPage;
        const pages = [];
        const start = Math.max(1, cur - 2);
        const end   = Math.min(total, cur + 2);
        for (let i = start; i <= end; i++) pages.push(i);
        return pages;
      },
      paginationLabel() {
        const total = this.filteredRoutes.length;
        const start = (this.currentPage - 1) * this.pageSize + 1;
        const end   = Math.min(this.currentPage * this.pageSize, total);
        return total === 0 ? '' : `${start}–${end} of ${total}`;
      },
    },

    watch: {
      search()    { this.currentPage = 1; },
      sortField() { this.currentPage = 1; },
    },

    methods: {
      /* ── Data loading ─────────────────────────────────────────────────── */
      async loadAll() { await Promise.all([this.loadRoutes(), this.loadHealth()]); },

      async loadRoutes() {
        this.loading = true;
        try {
          const res = await fetch('/gateway/admin/routes');
          if (!res.ok) throw new Error(`HTTP ${res.status}`);
          this.routes = await res.json();
        } catch (e) {
          this.notify('error', 'Load failed', e.message || 'Unable to fetch routes');
        } finally {
          this.loading = false;
        }
      },

      async loadHealth() {
        try {
          const res   = await fetch('/gateway/health');
          this.health = res.ok ? await res.json() : null;
        } catch { this.health = null; }
      },

      async reloadGateway() {
        try {
          const res  = await fetch('/gateway/admin/reload', { method: 'POST' });
          const data = await res.json();
          this.notify('success', 'Reloaded', `${data.routes ?? 0} routes active`);
          await this.loadRoutes();
        } catch (e) {
          this.notify('error', 'Reload failed', e.message);
        }
      },

      /* ── Sort ─────────────────────────────────────────────────────────── */
      sortBy(field) {
        if (this.sortField === field) {
          this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
        } else {
          this.sortField = field;
          this.sortDir   = 'asc';
        }
      },


      /* ── Dialog open helpers ──────────────────────────────────────────── */
      openCreate() {
        this.dlgMode    = 'create';
        this.form       = emptyForm();
        this.activeTab  = 0;
        this.syncToHelpers();
        this.dlgVisible = true;
      },

      openEdit(route) {
        this.dlgMode    = 'edit';
        this.form       = routeToForm(route);
        this.activeTab  = 0;
        this.syncToHelpers();
        this.dlgVisible = true;
      },

      syncToHelpers() {
        const hr = this.form['header-rules'];
        this.reqHdrEntries  = Object.entries(hr['add-request']  || {}).map(([k, v]) => ({ key: k, value: v }));
        this.respHdrEntries = Object.entries(hr['add-response'] || {}).map(([k, v]) => ({ key: k, value: v }));
      },

      syncFromHelpers() {
        this.form['header-rules']['add-request']  = Object.fromEntries(
          this.reqHdrEntries.filter(e => e.key).map(e => [e.key, e.value])
        );
        this.form['header-rules']['add-response'] = Object.fromEntries(
          this.respHdrEntries.filter(e => e.key).map(e => [e.key, e.value])
        );
      },

      /* ── CRUD ─────────────────────────────────────────────────────────── */
      async saveRoute() {
        if (!this.form.name || !this.form['path-pattern']) {
          this.notify('warn', 'Validation', 'Name and Path Pattern are required');
          this.activeTab = 0;
          return;
        }
        const validTargets = this.form.targets.filter(t => t.url);
        if (!validTargets.length) {
          this.notify('warn', 'Validation', 'Add at least one target URL');
          this.activeTab = 1;
          return;
        }
        this.syncFromHelpers();
        this.saving = true;
        try {
          const isCreate = this.dlgMode === 'create';
          const url      = isCreate
            ? '/gateway/admin/routes'
            : `/gateway/admin/routes/${this.form.id}`;
          const method   = isCreate ? 'POST' : 'PUT';
          const res      = await fetch(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify(this.form),
          });
          if (!res.ok) throw new Error(await res.text());
          this.notify('success', 'Saved', isCreate ? 'Route created' : 'Route updated');
          this.dlgVisible = false;
          await this.loadRoutes();
        } catch (e) {
          this.notify('error', 'Save failed', e.message || 'Unknown error');
        } finally {
          this.saving = false;
        }
      },

      async toggleRoute(route) {
        const action = route.enabled ? 'disable' : 'enable';
        try {
          const res = await fetch(`/gateway/admin/routes/${route.id}/${action}`, { method: 'PATCH' });
          if (!res.ok) throw new Error(`HTTP ${res.status}`);
          this.notify('success', 'Updated', `Route ${action}d`);
          await this.loadRoutes();
        } catch (e) {
          this.notify('error', 'Toggle failed', e.message);
        }
      },

      confirmDelete(route) {
        this.showConfirm(
          `Delete route "${route.name}"? This cannot be undone.`,
          () => this.deleteRoute(route.id)
        );
      },

      async deleteRoute(id) {
        try {
          const res = await fetch(`/gateway/admin/routes/${id}`, { method: 'DELETE' });
          if (!res.ok && res.status !== 204) throw new Error(`HTTP ${res.status}`);
          this.notify('success', 'Deleted', 'Route removed');
          await this.loadRoutes();
        } catch (e) {
          this.notify('error', 'Delete failed', e.message);
        }
      },

      /* ── Target rows ──────────────────────────────────────────────────── */
      addTarget()     { this.form.targets.push({ url: '', weight: 1 }); },
      removeTarget(i) { this.form.targets.splice(i, 1); },

      /* ── Header entry rows ────────────────────────────────────────────── */
      addReqHdr()       { this.reqHdrEntries.push({ key: '', value: '' }); },
      removeReqHdr(i)   { this.reqHdrEntries.splice(i, 1); },
      addRespHdr()      { this.respHdrEntries.push({ key: '', value: '' }); },
      removeRespHdr(i)  { this.respHdrEntries.splice(i, 1); },

      addExclReq()  { if (this.newExclReq)  { this.form['header-rules']['exclude-request'].push(this.newExclReq);  this.newExclReq  = ''; } },
      addExclResp() { if (this.newExclResp) { this.form['header-rules']['exclude-response'].push(this.newExclResp); this.newExclResp = ''; } },
      addAuth()     { if (this.newAuth)     { this.form['auth-forward-headers'].push(this.newAuth);                this.newAuth     = ''; } },

      /* ── Display helpers ──────────────────────────────────────────────── */
      routeTypeSeverity(type) {
        return { PATH: 'tag-info', REGEX: 'tag-warning', HEADER: 'tag-secondary', TRAFFIC_SPLIT: 'tag-contrast' }[type] || 'tag-info';
      },

      formatTargets(targets) {
        if (!targets?.length) return '—';
        const urls = targets.map(t => t.url).filter(Boolean);
        if (!urls.length) return '—';
        return urls.slice(0, 2).join(', ') + (urls.length > 2 ? ` (+${urls.length - 2} more)` : '');
      },

      shortLb(lb) {
        return { ROUND_ROBIN: 'RR', WEIGHTED: 'W', RANDOM: 'RND' }[lb] || lb || '—';
      },

      /* ── Native toast notifications ───────────────────────────────────── */
      notify(severity, summary, detail) {
        const id = ++this.notifCounter;
        this.notifications.push({ id, severity, summary, detail });
        setTimeout(() => this.dismissNotif(id), severity === 'error' ? 6000 : 3500);
      },

      dismissNotif(id) {
        const idx = this.notifications.findIndex(n => n.id === id);
        if (idx !== -1) this.notifications.splice(idx, 1);
      },

      /* ── Native confirm dialog ────────────────────────────────────────── */
      showConfirm(msg, callback) {
        this.confirmMsg      = msg;
        this.confirmCallback = callback;
        this.confirmVisible  = true;
      },

      confirmAccept() {
        this.confirmVisible = false;
        if (this.confirmCallback) {
          this.confirmCallback();
          this.confirmCallback = null;
        }
      },

      confirmReject() {
        this.confirmVisible = false;
        this.confirmCallback = null;
      },
    },

    mounted() {
      this.loadAll();
    },
  });

  /* ── 3. Mount ───────────────────────────────────────────────────────────── */
  app.config.errorHandler = (err, instance, info) => {
    console.error('[Gateway UI] Vue error:', err, '\nInfo:', info);
  };
  app.mount('#app');

})();
