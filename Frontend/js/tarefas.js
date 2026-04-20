//
// TAREFAS.JS — Módulo completo de gestão de tarefas
//

const API_TAREFAS = `${API_BASE_URL}/tarefas`;

// responsavelId é @NotNull no backend — sistemas single-admin usam ID 1
const RESPONSAVEL_ID_PADRAO = 1;

class TarefasModule {

    static STATUS_LABELS = {
        PENDENTE:    'Pendente',
        EM_PROCESSO: 'Em Progresso',
        CONCLUIDA:   'Concluída',
        CANCELADA:   'Cancelada',
    };

    static PRIORIDADE_LABELS = {
        BAIXA:   'Baixa',
        MEDIA:   'Média',
        ALTA:    'Alta',
        URGENTE: 'Urgente',
    };

    constructor() {
        this.tarefas          = [];
        this.editingId        = null;
        this.deletingId       = null;
        this.searchTerm       = '';
        this.filterStatus     = '';
        this.filterPrioridade = '';
        this.filterDataInicio = '';
        this.filterDataFim    = '';
        this.filterResponsavel = '';
        this.init();
    }

    init() {
        this.bindElements();
        this.bindEvents();
        this.loadData();
    }

    // ══ AUTH ════════════════════════════════════════════════════════════

    getToken() {
        return localStorage.getItem('crm_token') || localStorage.getItem('token') || null;
    }

    authHeaders() {
        const t = this.getToken();
        return t ? { 'Authorization': `Bearer ${t}`, 'Content-Type': 'application/json' }
                 : { 'Content-Type': 'application/json' };
    }

    // ══ CARREGAMENTO ════════════════════════════════════════════════════

    async loadData() {
        this.showLoading();
        try {
            const res = await fetch(`${API_TAREFAS}?size=500&sort=id,desc`, {
                headers: this.authHeaders()
            });
            if (res.status === 401 || res.status === 403) {
                this.tarefas = [];
                this.toast('danger', 'fas fa-lock',
                    'Sessão expirada. <a href="login.html" style="color:inherit;text-decoration:underline;font-weight:600">Faça login</a>.',
                    10000);
                return;
            }
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const json = await res.json();
            this.tarefas = json.dados?.content ?? json.dados ?? [];
        } catch (e) {
            console.error('[Tarefas] Erro ao carregar:', e.message);
            this.tarefas = [];
            this.toast('danger', 'fas fa-server', `Não foi possível conectar ao backend: ${this.esc(e.message)}`, 10000);
        } finally {
            this.buildResponsavelFilter();
            this.render();
        }
    }

    showLoading() {
        if (!this.$tbody) return;
        this.$empty?.classList.add('hidden');
        this.$tbody.innerHTML = `
            <tr class="loading-row">
                <td colspan="7">
                    <div class="table-loading">
                        <i class="fas fa-spinner"></i>
                        <span>Carregando tarefas...</span>
                    </div>
                </td>
            </tr>`;
    }

    // ══ API ═════════════════════════════════════════════════════════════

    async apiCreate(data) {
        const res = await fetch(API_TAREFAS, {
            method: 'POST', headers: this.authHeaders(), body: JSON.stringify(data)
        });
        return (await window.CRMAuth.handleApi(res)).dados;
    }

    async apiUpdate(id, data) {
        const res = await fetch(`${API_TAREFAS}/${id}`, {
            method: 'PUT', headers: this.authHeaders(), body: JSON.stringify(data)
        });
        return (await window.CRMAuth.handleApi(res)).dados;
    }

    async apiDelete(id) {
        const res = await fetch(`${API_TAREFAS}/${id}`, {
            method: 'DELETE', headers: this.authHeaders()
        });
        await window.CRMAuth.handleApi(res);
    }

    async apiConcluir(id) {
        const res = await fetch(`${API_TAREFAS}/${id}/concluir`, {
            method: 'PATCH', headers: this.authHeaders()
        });
        return (await window.CRMAuth.handleApi(res)).dados;
    }

    // ══ BIND ════════════════════════════════════════════════════════════

    bindElements() {
        this.$tbody               = document.getElementById('tarefasTableBody');
        this.$empty               = document.getElementById('tarefasEmpty');
        this.$count               = document.getElementById('tarCount');
        this.$kpiTotal            = document.getElementById('tarKpiTotal');
        this.$kpiPendente         = document.getElementById('tarKpiPendente');
        this.$kpiEmProcesso       = document.getElementById('tarKpiEmProcesso');
        this.$kpiConcluida        = document.getElementById('tarKpiConcluida');
        this.$search              = document.getElementById('tarSearch');
        this.$searchClear         = document.getElementById('tarSearchClear');
        this.$filterStatus        = document.getElementById('tarFilterStatus');
        this.$filterPrioridade    = document.getElementById('tarFilterPrioridade');
        this.$filterDataInicio    = document.getElementById('tarFilterDataInicio');
        this.$filterDataFim       = document.getElementById('tarFilterDataFim');
        this.$filterResponsavel   = document.getElementById('tarFilterResponsavel');
        this.$modal               = document.getElementById('tarModalBackdrop');
        this.$modalTitle          = document.getElementById('tarModalTitle');
        this.$form                = document.getElementById('tarForm');
        this.$fId                 = document.getElementById('tarId');
        this.$fTitulo             = document.getElementById('tarTitulo');
        this.$fPrioridade         = document.getElementById('tarPrioridade');
        this.$fStatus             = document.getElementById('tarStatus');
        this.$fVencimento         = document.getElementById('tarVencimento');
        this.$fResponsavelId      = document.getElementById('tarResponsavelId');
        this.$fVinculoTipo        = document.getElementById('tarVinculoTipo');
        this.$fVinculoId          = document.getElementById('tarVinculoId');
        this.$fDescricao          = document.getElementById('tarDescricao');
        this.$fObservacoes        = document.getElementById('tarObservacoes');
        this.$deleteModal         = document.getElementById('tarDeleteModalBackdrop');
        this.$deleteName          = document.getElementById('tarDeleteName');
    }

    bindEvents() {
        document.getElementById('btnNovaTarefa').addEventListener('click',    () => this.openModal());
        document.getElementById('btnTarEmptyNova').addEventListener('click',  () => this.openModal());
        document.getElementById('tarModalSave').addEventListener('click',     () => this.save());
        document.getElementById('tarModalCancel').addEventListener('click',   () => this.closeModal());
        document.getElementById('tarModalClose').addEventListener('click',    () => this.closeModal());
        this.$modal.addEventListener('click', e => { if (e.target === this.$modal) this.closeModal(); });

        document.getElementById('tarDeleteModalConfirm').addEventListener('click', () => this.confirmDelete());
        document.getElementById('tarDeleteModalCancel').addEventListener('click',  () => this.closeDeleteModal());
        document.getElementById('tarDeleteModalClose').addEventListener('click',   () => this.closeDeleteModal());
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

        // Filters
        this.$filterStatus.addEventListener('change', () => {
            this.filterStatus = this.$filterStatus.value; this.render();
        });
        this.$filterPrioridade.addEventListener('change', () => {
            this.filterPrioridade = this.$filterPrioridade.value; this.render();
        });
        this.$filterDataInicio.addEventListener('change', () => {
            this.filterDataInicio = this.$filterDataInicio.value; this.render();
        });
        this.$filterDataFim.addEventListener('change', () => {
            this.filterDataFim = this.$filterDataFim.value; this.render();
        });
        this.$filterResponsavel.addEventListener('change', () => {
            this.filterResponsavel = this.$filterResponsavel.value; this.render();
        });

        // Vínculo tipo toggle
        this.$fVinculoTipo.addEventListener('change', () => {
            const hasType = !!this.$fVinculoTipo.value;
            this.$fVinculoId.disabled = !hasType;
            if (!hasType) this.$fVinculoId.value = '';
        });
    }

    // ══ RESPONSÁVEL FILTER ══════════════════════════════════════════════

    buildResponsavelFilter() {
        const map = new Map();
        this.tarefas.forEach(t => {
            if (t.responsavelId && t.responsavelNome) {
                map.set(String(t.responsavelId), t.responsavelNome);
            }
        });
        const current = this.$filterResponsavel.value;
        this.$filterResponsavel.innerHTML = '<option value="">Todos os responsáveis</option>';
        map.forEach((nome, id) => {
            const opt = document.createElement('option');
            opt.value = id;
            opt.textContent = nome;
            this.$filterResponsavel.appendChild(opt);
        });
        this.$filterResponsavel.value = current;
    }

    // ══ FILTRO ══════════════════════════════════════════════════════════

    getFiltered() {
        return this.tarefas.filter(t => {
            const s = this.searchTerm;
            const matchSearch = !s
                || (t.titulo            || '').toLowerCase().includes(s)
                || (t.clienteNome       || '').toLowerCase().includes(s)
                || (t.leadNome          || '').toLowerCase().includes(s)
                || (t.oportunidadeTitulo|| '').toLowerCase().includes(s)
                || (t.responsavelNome   || '').toLowerCase().includes(s);

            const matchStatus     = !this.filterStatus     || t.status     === this.filterStatus;
            const matchPrioridade = !this.filterPrioridade || t.prioridade === this.filterPrioridade;
            const matchResponsavel = !this.filterResponsavel || String(t.responsavelId) === this.filterResponsavel;

            let matchData = true;
            if (this.filterDataInicio && t.dataVencimento) {
                matchData = t.dataVencimento >= this.filterDataInicio;
            } else if (this.filterDataInicio && !t.dataVencimento) {
                matchData = false;
            }
            if (matchData && this.filterDataFim && t.dataVencimento) {
                matchData = t.dataVencimento <= this.filterDataFim;
            } else if (this.filterDataFim && !t.dataVencimento) {
                matchData = false;
            }

            return matchSearch && matchStatus && matchPrioridade && matchResponsavel && matchData;
        });
    }

    // ══ RENDER ══════════════════════════════════════════════════════════

    render() {
        const total = this.tarefas.length;
        this.$kpiTotal.textContent      = total;
        this.$kpiPendente.textContent   = this.tarefas.filter(t => t.status === 'PENDENTE').length;
        this.$kpiEmProcesso.textContent = this.tarefas.filter(t => t.status === 'EM_PROCESSO').length;
        this.$kpiConcluida.textContent  = this.tarefas.filter(t => t.status === 'CONCLUIDA').length;

        const filtered = this.getFiltered();
        this.$count.textContent = `${filtered.length} tarefa${filtered.length !== 1 ? 's' : ''}`;

        if (filtered.length === 0) {
            this.$tbody.innerHTML = '';
            this.$empty.classList.remove('hidden');
        } else {
            this.$empty.classList.add('hidden');
            this.$tbody.innerHTML = filtered.map(t => this.renderRow(t)).join('');
            this.bindRowEvents();
        }
    }

    renderRow(t) {
        const venc = t.dataVencimento ? this.formatDate(t.dataVencimento) : '<span class="text-muted">—</span>';
        const vencida = t.dataVencimento && new Date(t.dataVencimento) < new Date() && t.status !== 'CONCLUIDA' && t.status !== 'CANCELADA';
        const vinculo = this.renderVinculo(t);
        const responsavel = t.responsavelNome ? this.esc(t.responsavelNome) : '<span class="text-muted">—</span>';
        const isConcluida = t.status === 'CONCLUIDA' || t.status === 'CANCELADA';

        return `
        <tr data-id="${t.id}" class="${isConcluida ? 'row-concluida' : ''}">
            <td>
                <div class="client-name">${this.esc(t.titulo)}</div>
                <div class="client-id">#${t.id.toString().padStart(4,'0')}</div>
            </td>
            <td>${this.prioridadeBadge(t.prioridade)}</td>
            <td>${this.statusBadge(t.status)}</td>
            <td>
                <span class="${vencida ? 'text-danger' : ''}" title="${t.dataVencimento || ''}">
                    ${venc}
                    ${vencida ? '<i class="fas fa-triangle-exclamation" style="margin-left:.25rem;color:var(--color-danger)"></i>' : ''}
                </span>
            </td>
            <td>${vinculo}</td>
            <td>${responsavel}</td>
            <td style="text-align:center">
                <div class="actions">
                    ${!isConcluida ? `<button class="btn btn-sm btn-success btn-concluir" data-id="${t.id}" title="Marcar como concluída">
                        <i class="fas fa-check"></i>
                    </button>` : ''}
                    <button class="btn btn-sm btn-secondary btn-edit" data-id="${t.id}" title="Editar">
                        <i class="fas fa-pen"></i>
                    </button>
                    <button class="btn btn-sm btn-danger btn-delete" data-id="${t.id}" title="Excluir">
                        <i class="fas fa-trash-alt"></i>
                    </button>
                </div>
            </td>
        </tr>`;
    }

    renderVinculo(t) {
        if (t.clienteNome) {
            return `<span class="badge badge-info" title="Cliente #${t.clienteId}"><i class="fas fa-building"></i> ${this.esc(t.clienteNome)}</span>`;
        }
        if (t.leadNome) {
            return `<span class="badge badge-warning" title="Lead #${t.leadId}"><i class="fas fa-user-plus"></i> ${this.esc(t.leadNome)}</span>`;
        }
        if (t.oportunidadeTitulo) {
            return `<span class="badge badge-success" title="Oportunidade #${t.oportunidadeId}"><i class="fas fa-handshake"></i> ${this.esc(t.oportunidadeTitulo)}</span>`;
        }
        return '<span class="text-muted">—</span>';
    }

    bindRowEvents() {
        this.$tbody.querySelectorAll('.btn-edit').forEach(btn =>
            btn.addEventListener('click', () => this.openModal(parseInt(btn.dataset.id))));
        this.$tbody.querySelectorAll('.btn-delete').forEach(btn =>
            btn.addEventListener('click', () => this.openDeleteModal(parseInt(btn.dataset.id))));
        this.$tbody.querySelectorAll('.btn-concluir').forEach(btn =>
            btn.addEventListener('click', () => this.concluir(parseInt(btn.dataset.id))));
    }

    // ══ CONCLUSÃO RÁPIDA ════════════════════════════════════════════════

    async concluir(id) {
        const t = this.tarefas.find(x => x.id === id);
        if (!t) return;
        try {
            const updated = await this.apiConcluir(id);
            const idx = this.tarefas.findIndex(x => x.id === id);
            if (idx > -1) this.tarefas[idx] = updated;
            this.toast('success', 'fas fa-circle-check', `Tarefa <strong>${this.esc(t.titulo)}</strong> concluída!`);
            this.render();
        } catch (e) {
            this.toast('danger', 'fas fa-exclamation-circle', `Erro: ${this.esc(e.message)}`);
        }
    }

    // ══ MODAL ═══════════════════════════════════════════════════════════

    openModal(id = null) {
        this.editingId = id;
        this.clearFormErrors();

        if (id !== null) {
            const t = this.tarefas.find(x => x.id === id);
            if (!t) return;
            this.$modalTitle.textContent = 'Editar Tarefa';
            this.$fId.value             = t.id;
            this.$fTitulo.value         = t.titulo          || '';
            this.$fPrioridade.value     = t.prioridade      || '';
            this.$fStatus.value         = t.status          || 'PENDENTE';
            this.$fVencimento.value     = t.dataVencimento  || '';
            this.$fResponsavelId.value  = t.responsavelId   ?? RESPONSAVEL_ID_PADRAO;
            this.$fDescricao.value      = t.descricao       || '';
            this.$fObservacoes.value    = t.observacoes      || '';

            // Restore vínculo
            if (t.clienteId) {
                this.$fVinculoTipo.value = 'cliente';
                this.$fVinculoId.value   = t.clienteId;
                this.$fVinculoId.disabled = false;
            } else if (t.leadId) {
                this.$fVinculoTipo.value = 'lead';
                this.$fVinculoId.value   = t.leadId;
                this.$fVinculoId.disabled = false;
            } else if (t.oportunidadeId) {
                this.$fVinculoTipo.value = 'oportunidade';
                this.$fVinculoId.value   = t.oportunidadeId;
                this.$fVinculoId.disabled = false;
            } else {
                this.$fVinculoTipo.value = '';
                this.$fVinculoId.value   = '';
                this.$fVinculoId.disabled = true;
            }
        } else {
            this.$modalTitle.textContent = 'Nova Tarefa';
            this.$form.reset();
            this.$fId.value = '';
            this.$fResponsavelId.value = RESPONSAVEL_ID_PADRAO;
            this.$fVinculoId.disabled = true;
        }

        this.$modal.classList.remove('hidden');
        setTimeout(() => this.$fTitulo.focus(), 50);
    }

    closeModal() { this.$modal.classList.add('hidden'); this.editingId = null; }

    async save() {
        if (!this.validateForm()) return;

        const vinculoTipo = this.$fVinculoTipo.value;
        const vinculoIdVal = this.$fVinculoId.value.trim();
        const vinculoId = vinculoIdVal ? parseInt(vinculoIdVal) : null;

        const data = {
            titulo:          this.$fTitulo.value.trim(),
            descricao:       this.$fDescricao.value.trim() || null,
            observacoes:     this.$fObservacoes.value.trim() || null,
            dataVencimento:  this.$fVencimento.value       || null,
            prioridade:      this.$fPrioridade.value,
            status:          this.$fStatus.value           || 'PENDENTE',
            responsavelId:   parseInt(this.$fResponsavelId.value) || RESPONSAVEL_ID_PADRAO,
            clienteId:       vinculoTipo === 'cliente'       ? vinculoId : null,
            leadId:          vinculoTipo === 'lead'          ? vinculoId : null,
            oportunidadeId:  vinculoTipo === 'oportunidade'  ? vinculoId : null,
        };

        const btn = document.getElementById('tarModalSave');
        btn.disabled = true;
        try {
            if (this.editingId !== null) {
                const updated = await this.apiUpdate(this.editingId, data);
                const idx = this.tarefas.findIndex(t => t.id === this.editingId);
                if (idx > -1) this.tarefas[idx] = updated;
                this.toast('success', 'fas fa-check-circle', `Tarefa <strong>${this.esc(data.titulo)}</strong> atualizada.`);
            } else {
                const created = await this.apiCreate(data);
                this.tarefas.unshift(created);
                this.toast('success', 'fas fa-check-circle', `Tarefa <strong>${this.esc(data.titulo)}</strong> criada.`);
            }
            this.closeModal();
            this.buildResponsavelFilter();
            this.render();
        } catch (e) {
            this.toast('danger', 'fas fa-exclamation-circle', `Erro: ${this.esc(e.message)}`);
        } finally {
            btn.disabled = false;
        }
    }

    validateForm() {
        let ok = true;
        this.clearFormErrors();
        if (!this.$fTitulo.value.trim())     { this.showFieldError(this.$fTitulo,        'tarErrTitulo');     ok = false; }
        if (!this.$fPrioridade.value)        { this.showFieldError(this.$fPrioridade,    'tarErrPrioridade'); ok = false; }
        if (!this.$fResponsavelId.value)     { this.showFieldError(this.$fResponsavelId, 'tarErrResponsavel');ok = false; }
        return ok;
    }

    showFieldError(input, errId) {
        input.classList.add('invalid');
        const el = document.getElementById(errId);
        if (el) el.classList.remove('hidden');
    }

    clearFormErrors() {
        this.$form.querySelectorAll('.invalid').forEach(el => el.classList.remove('invalid'));
        this.$form.querySelectorAll('.form-error').forEach(el => el.classList.add('hidden'));
    }

    // ══ MODAL EXCLUSÃO ═══════════════════════════════════════════════════

    openDeleteModal(id) {
        const t = this.tarefas.find(x => x.id === id);
        if (!t) return;
        this.deletingId = id;
        this.$deleteName.textContent = t.titulo;
        this.$deleteModal.classList.remove('hidden');
    }

    closeDeleteModal() { this.$deleteModal.classList.add('hidden'); this.deletingId = null; }

    async confirmDelete() {
        const t = this.tarefas.find(x => x.id === this.deletingId);
        if (!t) return;
        const btn = document.getElementById('tarDeleteModalConfirm');
        btn.disabled = true;
        try {
            await this.apiDelete(this.deletingId);
            this.closeDeleteModal();
            this.toast('success', 'fas fa-check-circle', `Tarefa <strong>${this.esc(t.titulo)}</strong> excluída.`);
            await this.loadData();
        } catch (e) {
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

    prioridadeBadge(prioridade) {
        const map = {
            'BAIXA':   { cls: 'badge-neutral', icon: 'fa-arrow-down'   },
            'MEDIA':   { cls: 'badge-info',    icon: 'fa-arrow-right'  },
            'ALTA':    { cls: 'badge-warning', icon: 'fa-arrow-up'     },
            'URGENTE': { cls: 'badge-danger',  icon: 'fa-fire'         },
        };
        const s = map[prioridade] || { cls: 'badge-neutral', icon: 'fa-circle' };
        const label = TarefasModule.PRIORIDADE_LABELS[prioridade] || prioridade;
        return `<span class="badge ${s.cls}"><i class="fas ${s.icon}"></i> ${label}</span>`;
    }

    statusBadge(status) {
        const map = {
            'PENDENTE':    { cls: 'badge-neutral', icon: 'fa-clock'         },
            'EM_PROCESSO': { cls: 'badge-warning', icon: 'fa-rotate'        },
            'CONCLUIDA':   { cls: 'badge-success', icon: 'fa-circle-check'  },
            'CANCELADA':   { cls: 'badge-danger',  icon: 'fa-circle-xmark'  },
        };
        const s = map[status] || { cls: 'badge-neutral', icon: 'fa-circle' };
        const label = TarefasModule.STATUS_LABELS[status] || status;
        return `<span class="badge ${s.cls}"><i class="fas ${s.icon}"></i> ${label}</span>`;
    }

    formatDate(dateStr) {
        if (!dateStr) return '—';
        const [y, m, d] = dateStr.split('-');
        return `${d}/${m}/${y}`;
    }

    esc(str) {
        return String(str)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;')
            .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }
}

document.addEventListener('DOMContentLoaded', () => {
    window.tarefasModule = new TarefasModule();
});
