//
// OPORTUNIDADES.JS — Funil de Vendas com Kanban + Tabela
//

const API_OPORTUNIDADES = `${API_BASE_URL}/oportunidades`;

class OportunidadesModule {

    static ETAPAS = [
        'QUALIFICACAO',
        'ANALISE_NECESSIDADES',
        'PROPOSTA',
        'NEGOCIACAO',
        'FECHAMENTO',
    ];

    static ETAPA_LABELS = {
        QUALIFICACAO:         'Qualificação',
        ANALISE_NECESSIDADES: 'Análise',
        PROPOSTA:             'Proposta',
        NEGOCIACAO:           'Negociação',
        FECHAMENTO:           'Fechamento',
    };

    static ETAPA_ICONS = {
        QUALIFICACAO:         'fa-magnifying-glass',
        ANALISE_NECESSIDADES: 'fa-clipboard-list',
        PROPOSTA:             'fa-file-alt',
        NEGOCIACAO:           'fa-handshake',
        FECHAMENTO:           'fa-circle-check',
    };

    static STATUS_LABELS = {
        ABERTA:    'Aberta',
        GANHA:     'Ganha',
        PERDIDA:   'Perdida',
        ARQUIVADA: 'Arquivada',
    };

    constructor() {
        this.oportunidades = [];
        this.editingId     = null;
        this.deletingId    = null;
        this.searchTerm    = '';
        this.filterStatus  = '';
        this.view          = 'kanban'; // 'kanban' | 'table'
        this.dragId        = null;
        this.init();
    }

    init() {
        this.bindElements();
        this.bindEvents();
        this.loadData();
    }

    // ══ CARREGAMENTO ════════════════════════════════════════════════════

    async loadData() {
        this.showLoading();
        try {
            const res = await apiFetch(`${API_OPORTUNIDADES}?size=500&sort=id,desc`);
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const json = await res.json();
            this.oportunidades = json.dados?.content ?? json.dados ?? [];
        } catch (e) {
            if (e.isAuthError) return;
            console.error('[Oportunidades] Erro ao carregar:', e.message);
            this.oportunidades = [];
            this.toast('danger', 'fas fa-server', `Não foi possível conectar ao backend: ${this.esc(e.message)}`, 10000);
        } finally {
            this.render();
        }
    }

    showLoading() {
        this.$empty?.classList.add('hidden');
        if (this.view === 'kanban') {
            this.$kanban.innerHTML = '<div style="text-align:center;padding:3rem;color:var(--color-text-muted)"><i class="fas fa-spinner fa-spin" style="font-size:24px"></i><br>Carregando...</div>';
        } else if (this.$tbody) {
            this.$tbody.innerHTML = `<tr class="loading-row"><td colspan="7"><div class="table-loading"><i class="fas fa-spinner"></i><span>Carregando...</span></div></td></tr>`;
        }
    }

    // ══ API ═════════════════════════════════════════════════════════════

    async apiCreate(data) {
        const res = await apiFetch(API_OPORTUNIDADES, { method: 'POST', body: JSON.stringify(data) });
        if (!res.ok) { const e = await res.json().catch(() => ({})); throw new Error(e.mensagem || `HTTP ${res.status}`); }
        return (await res.json()).dados;
    }

    async apiUpdate(id, data) {
        const res = await apiFetch(`${API_OPORTUNIDADES}/${id}`, { method: 'PUT', body: JSON.stringify(data) });
        if (!res.ok) { const e = await res.json().catch(() => ({})); throw new Error(e.mensagem || `HTTP ${res.status}`); }
        return (await res.json()).dados;
    }

    async apiDelete(id) {
        const res = await apiFetch(`${API_OPORTUNIDADES}/${id}`, { method: 'DELETE' });
        if (!res.ok) { const e = await res.json().catch(() => ({})); throw new Error(e.mensagem || `HTTP ${res.status}`); }
    }

    // ══ BIND ════════════════════════════════════════════════════════════

    bindElements() {
        this.$kanban           = document.getElementById('opKanbanBoard');
        this.$tableView        = document.getElementById('opTableView');
        this.$tbody            = document.getElementById('oportunidadesTableBody');
        this.$empty            = document.getElementById('oportunidadesEmpty');
        this.$count            = document.getElementById('opCount');
        this.$kpiTotal         = document.getElementById('opKpiTotal');
        this.$kpiAbertas       = document.getElementById('opKpiAbertas');
        this.$kpiGanhas        = document.getElementById('opKpiGanhas');
        this.$kpiValor         = document.getElementById('opKpiValor');
        this.$search           = document.getElementById('opSearch');
        this.$searchClear      = document.getElementById('opSearchClear');
        this.$filterStatus     = document.getElementById('opFilterStatus');
        // Modal
        this.$modal            = document.getElementById('opModalBackdrop');
        this.$modalTitle       = document.getElementById('opModalTitle');
        this.$form             = document.getElementById('opForm');
        this.$fId              = document.getElementById('opId');
        this.$fTitulo          = document.getElementById('opTitulo');
        this.$fClienteId       = document.getElementById('opClienteId');
        this.$fValor           = document.getElementById('opValor');
        this.$fEtapa           = document.getElementById('opEtapa');
        this.$fStatus          = document.getElementById('opStatus');
        this.$fProb            = document.getElementById('opProb');
        this.$fDataFechamento  = document.getElementById('opDataFechamento');
        this.$fResponsavel     = document.getElementById('opResponsavelId');
        this.$fObs             = document.getElementById('opObservacoes');
        // Delete modal
        this.$deleteModal      = document.getElementById('opDeleteModalBackdrop');
        this.$deleteName       = document.getElementById('opDeleteName');
        // Gerar Orçamento button
        this.$btnOrcamento     = document.getElementById('opModalOrcamento');
    }

    bindEvents() {
        // New
        document.getElementById('btnNovaOportunidade').addEventListener('click', () => this.openModal());
        document.getElementById('btnOpEmptyNova').addEventListener('click',      () => this.openModal());
        document.getElementById('btnIrParaOrcamentos')?.addEventListener('click', () => {
            if (window.navigationManager) window.navigationManager.navigateTo('orcamentos');
        });
        // Modal CRUD
        document.getElementById('opModalSave').addEventListener('click',   () => this.save());
        document.getElementById('opModalCancel').addEventListener('click', () => this.closeModal());
        document.getElementById('opModalClose').addEventListener('click',  () => this.closeModal());
        this.$modal.addEventListener('click', e => { if (e.target === this.$modal) this.closeModal(); });
        // Gerar Orçamento
        this.$btnOrcamento.addEventListener('click', () => this.gerarOrcamento());
        // Delete modal
        document.getElementById('opDeleteModalConfirm').addEventListener('click', () => this.confirmDelete());
        document.getElementById('opDeleteModalCancel').addEventListener('click',  () => this.closeDeleteModal());
        document.getElementById('opDeleteModalClose').addEventListener('click',   () => this.closeDeleteModal());
        this.$deleteModal.addEventListener('click', e => { if (e.target === this.$deleteModal) this.closeDeleteModal(); });

        document.addEventListener('keydown', e => {
            if (e.key === 'Escape') {
                if (!this.$modal.classList.contains('hidden'))       this.closeModal();
                if (!this.$deleteModal.classList.contains('hidden')) this.closeDeleteModal();
            }
        });

        // Search
        this.$search.addEventListener('input', () => {
            this.searchTerm = this.$search.value.trim().toLowerCase();
            this.$searchClear.classList.toggle('hidden', !this.searchTerm);
            this.render();
        });
        this.$searchClear.addEventListener('click', () => {
            this.$search.value = ''; this.searchTerm = '';
            this.$searchClear.classList.add('hidden');
            this.$search.focus(); this.render();
        });

        // Status filter
        this.$filterStatus.addEventListener('change', () => { this.filterStatus = this.$filterStatus.value; this.render(); });

        // View toggle
        document.getElementById('opViewKanban').addEventListener('click', () => this.setView('kanban'));
        document.getElementById('opViewTable').addEventListener('click',  () => this.setView('table'));
    }

    // ══ VIEW TOGGLE ═════════════════════════════════════════════════════

    setView(v) {
        this.view = v;
        document.getElementById('opViewKanban').classList.toggle('active', v === 'kanban');
        document.getElementById('opViewTable').classList.toggle('active', v === 'table');
        this.$kanban.classList.toggle('hidden', v !== 'kanban');
        this.$tableView.classList.toggle('hidden', v !== 'table');
        this.render();
    }

    // ══ FILTER ══════════════════════════════════════════════════════════

    getFiltered() {
        return this.oportunidades.filter(o => {
            const s = this.searchTerm;
            const matchSearch = !s
                || (o.titulo         || '').toLowerCase().includes(s)
                || (o.clienteNome    || '').toLowerCase().includes(s)
                || (o.responsavelNome|| '').toLowerCase().includes(s);
            return matchSearch
                && (!this.filterStatus || o.status === this.filterStatus);
        });
    }

    // ══ RENDER ══════════════════════════════════════════════════════════

    render() {
        const all        = this.oportunidades;
        const abertas    = all.filter(o => o.status === 'ABERTA');
        const pipeline   = abertas.reduce((s, o) => s + (o.valorEstimado || 0), 0);

        this.$kpiTotal.textContent   = all.length;
        this.$kpiAbertas.textContent = abertas.length;
        this.$kpiGanhas.textContent  = all.filter(o => o.status === 'GANHA').length;
        this.$kpiValor.textContent   = this.formatCompact(pipeline);

        const filtered = this.getFiltered();
        this.$count.textContent = `${filtered.length} oportunidade${filtered.length !== 1 ? 's' : ''}`;

        if (filtered.length === 0) {
            this.$kanban.innerHTML = '';
            if (this.$tbody) this.$tbody.innerHTML = '';
            this.$empty.classList.remove('hidden');
        } else {
            this.$empty.classList.add('hidden');
            if (this.view === 'kanban') {
                this.renderKanban(filtered);
            } else {
                this.renderTable(filtered);
            }
        }
    }

    // ── KANBAN ──────────────────────────────────────────────────────────

    renderKanban(items) {
        this.$kanban.innerHTML = OportunidadesModule.ETAPAS.map(etapa => {
            const colItems = items.filter(o => o.etapa === etapa);
            const colValue = colItems.reduce((s, o) => s + (o.valorEstimado || 0), 0);
            const label    = OportunidadesModule.ETAPA_LABELS[etapa];
            const icon     = OportunidadesModule.ETAPA_ICONS[etapa];

            return `
            <div class="kanban-column" data-etapa="${etapa}">
                <div class="kanban-column-header">
                    <i class="fas ${icon}" style="margin-right:4px"></i> ${label}
                    <span class="col-count">${colItems.length}</span>
                    <span class="col-value">${this.formatCompact(colValue)}</span>
                </div>
                <div class="kanban-cards" data-etapa="${etapa}">
                    ${colItems.length
                        ? colItems.map(o => this.renderCard(o)).join('')
                        : '<div class="kanban-empty"><i class="fas fa-inbox"></i>Nenhuma oportunidade</div>'}
                </div>
            </div>`;
        }).join('');

        this.bindKanbanEvents();
    }

    renderCard(o) {
        const valor   = o.valorEstimado ? this.formatCurrency(o.valorEstimado) : '—';
        const cliente = o.clienteNome ? this.esc(o.clienteNome) : `#${o.clienteId || '?'}`;
        const resp    = o.responsavelNome ? this.esc(o.responsavelNome) : '';
        const prob    = o.probabilidadeFechamento != null ? `${o.probabilidadeFechamento}%` : '';

        return `
        <div class="kanban-card" draggable="true" data-id="${o.id}">
            <div class="kanban-card-actions">
                <button class="btn-card-edit" data-id="${o.id}" title="Editar"><i class="fas fa-pen"></i></button>
                <button class="btn-card-del" data-id="${o.id}" title="Excluir"><i class="fas fa-trash-alt"></i></button>
            </div>
            <div class="kanban-card-title">${this.esc(o.titulo)}</div>
            <div class="kanban-card-client"><i class="fas fa-building"></i> ${cliente}</div>
            <div class="kanban-card-footer">
                <span class="kanban-card-value">${valor}</span>
                <span class="kanban-card-meta">
                    ${prob ? `<span title="Probabilidade"><i class="fas fa-chart-line"></i> ${prob}</span>` : ''}
                    ${resp ? `<span title="Responsável"><i class="fas fa-user"></i> ${resp}</span>` : ''}
                </span>
            </div>
        </div>`;
    }

    bindKanbanEvents() {
        // Card edit / delete buttons
        this.$kanban.querySelectorAll('.btn-card-edit').forEach(btn =>
            btn.addEventListener('click', e => { e.stopPropagation(); this.openModal(parseInt(btn.dataset.id)); }));
        this.$kanban.querySelectorAll('.btn-card-del').forEach(btn =>
            btn.addEventListener('click', e => { e.stopPropagation(); this.openDeleteModal(parseInt(btn.dataset.id)); }));
        // Card click → open edit
        this.$kanban.querySelectorAll('.kanban-card').forEach(card =>
            card.addEventListener('click', () => this.openModal(parseInt(card.dataset.id))));

        // Drag & Drop
        this.$kanban.querySelectorAll('.kanban-card').forEach(card => {
            card.addEventListener('dragstart', e => {
                this.dragId = parseInt(card.dataset.id);
                card.classList.add('dragging');
                e.dataTransfer.effectAllowed = 'move';
            });
            card.addEventListener('dragend', () => {
                card.classList.remove('dragging');
                this.dragId = null;
                this.$kanban.querySelectorAll('.kanban-cards').forEach(c => c.classList.remove('drag-over'));
            });
        });

        this.$kanban.querySelectorAll('.kanban-cards').forEach(zone => {
            zone.addEventListener('dragover', e => { e.preventDefault(); e.dataTransfer.dropEffect = 'move'; zone.classList.add('drag-over'); });
            zone.addEventListener('dragleave', () => zone.classList.remove('drag-over'));
            zone.addEventListener('drop', e => {
                e.preventDefault();
                zone.classList.remove('drag-over');
                if (this.dragId == null) return;
                const newEtapa = zone.dataset.etapa;
                this.moveToEtapa(this.dragId, newEtapa);
            });
        });
    }

    async moveToEtapa(id, newEtapa) {
        const o = this.oportunidades.find(x => x.id === id);
        if (!o || o.etapa === newEtapa) return;

        const oldEtapa = o.etapa;
        // Optimistic update for snappy UI
        o.etapa = newEtapa;
        this.render();

        try {
            const data = {
                titulo:                  o.titulo,
                valorEstimado:           o.valorEstimado,
                probabilidadeFechamento: o.probabilidadeFechamento,
                etapa:                   newEtapa,
                status:                  o.status,
                dataFechamentoPrevista:  o.dataFechamentoPrevista || null,
                clienteId:               o.clienteId,
                responsavelId:           o.responsavelId || null,
                observacoes:             o.observacoes || null,
            };
            const updated = await this.apiUpdate(id, data);
            const idx = this.oportunidades.findIndex(x => x.id === id);
            if (idx > -1) this.oportunidades[idx] = updated;
            this.toast('success', 'fas fa-arrows-alt',
                `<strong>${this.esc(o.titulo)}</strong> movida para ${OportunidadesModule.ETAPA_LABELS[newEtapa]}.`);
            this.render();
        } catch (e) {
            if (e.isAuthError) return;
            // Rollback
            o.etapa = oldEtapa;
            this.render();
            this.toast('danger', 'fas fa-exclamation-circle', `Erro ao mover: ${this.esc(e.message)}`);
        }
    }

    // ── TABLE ───────────────────────────────────────────────────────────

    renderTable(items) {
        if (!this.$tbody) return;
        this.$tbody.innerHTML = items.map(o => this.renderRow(o)).join('');
        this.bindRowEvents();
    }

    renderRow(o) {
        const valor   = o.valorEstimado ? this.formatCurrency(o.valorEstimado) : '<span class="text-muted">—</span>';
        const prob    = o.probabilidadeFechamento != null ? `${o.probabilidadeFechamento}%` : '<span class="text-muted">—</span>';
        const cliente = o.clienteNome ? this.esc(o.clienteNome) : `<span class="text-muted">#${o.clienteId || '?'}</span>`;

        return `
        <tr data-id="${o.id}">
            <td>
                <div class="client-name">${this.esc(o.titulo)}</div>
                <div class="client-id">#${o.id.toString().padStart(4,'0')}</div>
            </td>
            <td>${cliente}</td>
            <td>${this.etapaBadge(o.etapa)}</td>
            <td><strong>${valor}</strong></td>
            <td>${prob}</td>
            <td>${this.statusBadge(o.status)}</td>
            <td style="text-align:center">
                <div class="actions">
                    <button class="btn btn-sm btn-secondary btn-edit" data-id="${o.id}" title="Editar">
                        <i class="fas fa-pen"></i>
                    </button>
                    <button class="btn btn-sm btn-danger btn-delete" data-id="${o.id}" title="Excluir">
                        <i class="fas fa-trash-alt"></i>
                    </button>
                </div>
            </td>
        </tr>`;
    }

    bindRowEvents() {
        this.$tbody.querySelectorAll('.btn-edit').forEach(btn =>
            btn.addEventListener('click', () => this.openModal(parseInt(btn.dataset.id))));
        this.$tbody.querySelectorAll('.btn-delete').forEach(btn =>
            btn.addEventListener('click', () => this.openDeleteModal(parseInt(btn.dataset.id))));
    }

    // ══ MODAL ═══════════════════════════════════════════════════════════

    openModal(id = null) {
        this.editingId = id;
        this.clearFormErrors();

        this.$btnOrcamento.classList.toggle('hidden', id === null);

        if (id !== null) {
            const o = this.oportunidades.find(x => x.id === id);
            if (!o) return;
            this.$modalTitle.textContent    = 'Editar Oportunidade';
            this.$fId.value                 = o.id;
            this.$fTitulo.value             = o.titulo                    || '';
            this.$fClienteId.value          = o.clienteId                 ?? '';
            this.$fValor.value              = o.valorEstimado             ?? '';
            this.$fEtapa.value              = o.etapa                     || '';
            this.$fStatus.value             = o.status                    || 'ABERTA';
            this.$fProb.value               = o.probabilidadeFechamento   ?? '';
            this.$fDataFechamento.value     = o.dataFechamentoPrevista    || '';
            this.$fResponsavel.value        = o.responsavelId             ?? '';
            this.$fObs.value                = o.observacoes               || '';
        } else {
            this.$modalTitle.textContent = 'Nova Oportunidade';
            this.$form.reset();
            this.$fId.value = '';
        }

        this.$modal.classList.remove('hidden');
        setTimeout(() => this.$fTitulo.focus(), 50);
    }

    closeModal() { this.$modal.classList.add('hidden'); this.editingId = null; }

    gerarOrcamento() {
        const o = this.oportunidades.find(x => x.id === this.editingId);
        if (!o) {
            this.toast('danger', 'fas fa-exclamation-circle', 'Oportunidade não encontrada. Recarregue a página e tente novamente.');
            return;
        }
        this.closeModal();
        if (!window.orcamentosModule) {
            this.toast('danger', 'fas fa-exclamation-circle', 'Módulo de orçamentos não disponível. Recarregue a página.');
            return;
        }
        window.orcamentosModule.openModalFromOportunidade(o);
    }

    async save() {
        if (!this.validateForm()) return;

        const respVal = this.$fResponsavel.value.trim();
        const data = {
            titulo:                   this.$fTitulo.value.trim(),
            valorEstimado:            parseFloat(this.$fValor.value),
            probabilidadeFechamento:  this.$fProb.value !== '' ? parseInt(this.$fProb.value) : null,
            etapa:                    this.$fEtapa.value,
            status:                   this.$fStatus.value             || 'ABERTA',
            dataFechamentoPrevista:   this.$fDataFechamento.value     || null,
            clienteId:                parseInt(this.$fClienteId.value),
            responsavelId:            respVal ? parseInt(respVal)     : null,
            observacoes:              this.$fObs.value.trim()         || null,
        };

        const btn = document.getElementById('opModalSave');
        btn.disabled = true;
        try {
            if (this.editingId !== null) {
                const updated = await this.apiUpdate(this.editingId, data);
                const idx = this.oportunidades.findIndex(o => o.id === this.editingId);
                if (idx > -1) this.oportunidades[idx] = updated;
                this.toast('success', 'fas fa-check-circle', `Oportunidade <strong>${this.esc(data.titulo)}</strong> atualizada.`);
            } else {
                const created = await this.apiCreate(data);
                this.oportunidades.unshift(created);
                this.toast('success', 'fas fa-check-circle', `Oportunidade <strong>${this.esc(data.titulo)}</strong> criada.`);
            }
            this.closeModal();
            this.render();
        } catch (e) {
            if (e.isAuthError) return;
            this.toast('danger', 'fas fa-exclamation-circle', `Erro: ${this.esc(e.message)}`);
        } finally {
            btn.disabled = false;
        }
    }

    validateForm() {
        let ok = true;
        this.clearFormErrors();
        if (!this.$fTitulo.value.trim())    { this.showFieldError(this.$fTitulo,    'opErrTitulo'); ok = false; }
        const cli = this.$fClienteId.value.trim();
        if (!cli || isNaN(parseInt(cli)))   { this.showFieldError(this.$fClienteId, 'opErrCliente'); ok = false; }
        if (!this.$fEtapa.value)            { this.showFieldError(this.$fEtapa,     'opErrEtapa');  ok = false; }
        const val = parseFloat(this.$fValor.value);
        if (!this.$fValor.value || isNaN(val) || val <= 0) {
            this.showFieldError(this.$fValor, 'opErrValor'); ok = false;
        }
        return ok;
    }

    showFieldError(input, errId) {
        input.classList.add('invalid');
        document.getElementById(errId).classList.remove('hidden');
    }

    clearFormErrors() {
        this.$form.querySelectorAll('.invalid').forEach(el => el.classList.remove('invalid'));
        this.$form.querySelectorAll('.form-error').forEach(el => el.classList.add('hidden'));
    }

    // ══ MODAL EXCLUSÃO ═══════════════════════════════════════════════════

    openDeleteModal(id) {
        const o = this.oportunidades.find(x => x.id === id);
        if (!o) return;
        this.deletingId = id;
        this.$deleteName.textContent = o.titulo;
        this.$deleteModal.classList.remove('hidden');
    }

    closeDeleteModal() { this.$deleteModal.classList.add('hidden'); this.deletingId = null; }

    async confirmDelete() {
        const o = this.oportunidades.find(x => x.id === this.deletingId);
        if (!o) return;
        const btn = document.getElementById('opDeleteModalConfirm');
        btn.disabled = true;
        try {
            await this.apiDelete(this.deletingId);
            this.closeDeleteModal();
            this.toast('success', 'fas fa-check-circle', `Oportunidade <strong>${this.esc(o.titulo)}</strong> excluída.`);
            await this.loadData();
        } catch (e) {
            if (e.isAuthError) return;
            this.toast('danger', 'fas fa-exclamation-circle', `Erro ao excluir: ${this.esc(e.message)}`);
            this.closeDeleteModal();
        } finally {
            btn.disabled = false;
        }
    }

    // ══ TOASTS ══════════════════════════════════════════════════════════

    toast(type, icon, message, duration = 4000) {
        const container = document.getElementById('toastContainer');
        const el = document.createElement('div');
        el.className = `toast toast-${type}`;
        el.innerHTML = `
            <i class="${icon}"></i>
            <span class="toast-msg">${message}</span>
            <button class="toast-close" aria-label="Fechar"><i class="fas fa-times"></i></button>`;
        const close = () => {
            el.classList.add('removing');
            el.addEventListener('animationend', () => el.remove(), { once: true });
        };
        el.querySelector('.toast-close').addEventListener('click', close);
        container.appendChild(el);
        setTimeout(close, duration);
    }

    // ══ HELPERS ═════════════════════════════════════════════════════════

    etapaBadge(etapa) {
        const map = {
            'QUALIFICACAO':         { cls: 'badge-info',    icon: 'fa-magnifying-glass' },
            'ANALISE_NECESSIDADES': { cls: 'badge-primary', icon: 'fa-clipboard-list'  },
            'PROPOSTA':             { cls: 'badge-warning', icon: 'fa-file-alt'         },
            'NEGOCIACAO':           { cls: 'badge-warning', icon: 'fa-handshake'        },
            'FECHAMENTO':           { cls: 'badge-success', icon: 'fa-circle-check'     },
        };
        const s = map[etapa] || { cls: 'badge-neutral', icon: 'fa-circle' };
        const label = OportunidadesModule.ETAPA_LABELS[etapa] || etapa;
        return `<span class="badge ${s.cls}"><i class="fas ${s.icon}"></i> ${label}</span>`;
    }

    statusBadge(status) {
        const map = {
            'ABERTA':    { cls: 'badge-info',    icon: 'fa-circle-dot'   },
            'GANHA':     { cls: 'badge-success', icon: 'fa-trophy'       },
            'PERDIDA':   { cls: 'badge-danger',  icon: 'fa-circle-xmark' },
            'ARQUIVADA': { cls: 'badge-neutral', icon: 'fa-box-archive'  },
        };
        const s = map[status] || { cls: 'badge-neutral', icon: 'fa-circle' };
        const label = OportunidadesModule.STATUS_LABELS[status] || status;
        return `<span class="badge ${s.cls}"><i class="fas ${s.icon}"></i> ${label}</span>`;
    }

    formatCurrency(value) {
        if (value == null) return '—';
        return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
    }

    formatCompact(value) {
        if (value == null || value === 0) return 'R$ 0';
        if (value >= 1_000_000) return `R$ ${(value / 1_000_000).toFixed(1)}M`;
        if (value >= 1_000)     return `R$ ${(value / 1_000).toFixed(1)}K`;
        return this.formatCurrency(value);
    }

    esc(str) {
        return String(str)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;')
            .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }
}

document.addEventListener('DOMContentLoaded', () => {
    window.oportunidadesModule = new OportunidadesModule();
});
