//
// LEADS.JS — Módulo completo de gestão de leads
//

const API_LEADS = `${API_BASE_URL}/leads`;
const API_CONVERSOES = `${API_BASE_URL}/conversoes`;

class LeadsModule {

    static AVATAR_COLORS = [
        '#CD5A26','#306EB4','#112251','#B84E1F','#122749','#FF9300','#FAA532','#10b981'
    ];

    static ORIGEM_LABELS = {
        SITE: 'Site', TELEFONE: 'Telefone', EMAIL: 'E-mail',
        EVENTO: 'Evento', INDICACAO: 'Indicação', OUTRO: 'Outro'
    };

    static STATUS_LABELS = {
        NOVO: 'Novo', QUALIFICADO: 'Qualificado', EM_CONTATO: 'Em Contato',
        PROPOSTA_ENVIADA: 'Proposta Enviada', PERDIDO: 'Perdido', CONVERTIDO: 'Convertido'
    };

    constructor() {
        this.leads          = [];
        this.editingId      = null;
        this.deletingId     = null;
        this.convertingId   = null;
        this.searchTerm     = '';
        this.filterStatus   = '';
        this.filterOrigem   = '';
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
            const res = await apiFetch(`${API_LEADS}?size=500&sort=id,desc`);
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const json = await res.json();
            // ResponseDTO<Page<LeadDTO>> → dados.content
            this.leads = json.dados?.content ?? json.dados ?? [];
        } catch (e) {
            if (e.isAuthError) return;
            console.error('[Leads] Erro ao carregar:', e.message);
            this.leads = [];
            this.toast('danger', 'fas fa-server',
                `Não foi possível conectar ao backend: ${this.esc(e.message)}`, 10000);
        } finally {
            this.render();
        }
    }

    showLoading() {
        if (!this.$tbody) return;
        this.$empty?.classList.add('hidden');
        this.$tbody.innerHTML = `
            <tr class="loading-row">
                <td colspan="8">
                    <div class="table-loading">
                        <i class="fas fa-spinner"></i>
                        <span>Carregando leads...</span>
                    </div>
                </td>
            </tr>`;
    }

    // ══ API ═════════════════════════════════════════════════════════════

    async apiCreate(data) {
        const res = await apiFetch(API_LEADS, { method: 'POST', body: JSON.stringify(data) });
        if (!res.ok) { const e = await res.json().catch(() => ({})); throw new Error(e.mensagem || `HTTP ${res.status}`); }
        return (await res.json()).dados;
    }

    async apiUpdate(id, data) {
        const res = await apiFetch(`${API_LEADS}/${id}`, { method: 'PUT', body: JSON.stringify(data) });
        if (!res.ok) { const e = await res.json().catch(() => ({})); throw new Error(e.mensagem || `HTTP ${res.status}`); }
        return (await res.json()).dados;
    }

    async apiDelete(id) {
        const res = await apiFetch(`${API_LEADS}/${id}`, { method: 'DELETE' });
        if (!res.ok) { const e = await res.json().catch(() => ({})); throw new Error(e.mensagem || `HTTP ${res.status}`); }
    }

    async apiConvert(leadId, usuarioId, motivoConversao) {
        const params = new URLSearchParams({ leadId, usuarioId });
        if (motivoConversao) params.append('motivoConversao', motivoConversao);
        const res = await apiFetch(`${API_CONVERSOES}/lead-cliente?${params}`, { method: 'POST' });
        if (!res.ok) { const e = await res.json().catch(() => ({})); throw new Error(e.mensagem || `HTTP ${res.status}`); }
        return (await res.json()).dados;
    }

    // ══ BIND ════════════════════════════════════════════════════════════

    bindElements() {
        this.$tbody          = document.getElementById('leadsTableBody');
        this.$empty          = document.getElementById('leadsEmpty');
        this.$count          = document.getElementById('leadCount');
        this.$kpiTotal       = document.getElementById('leadKpiTotal');
        this.$kpiNovo        = document.getElementById('leadKpiNovo');
        this.$kpiQualif      = document.getElementById('leadKpiQualificado');
        this.$kpiConvert     = document.getElementById('leadKpiConvertido');
        this.$search         = document.getElementById('leadSearch');
        this.$searchClear    = document.getElementById('leadSearchClear');
        this.$filterStatus   = document.getElementById('leadFilterStatus');
        this.$filterOrigem   = document.getElementById('leadFilterOrigem');
        // Modal principal
        this.$modal          = document.getElementById('leadModalBackdrop');
        this.$modalTitle     = document.getElementById('leadModalTitle');
        this.$form           = document.getElementById('leadForm');
        this.$fId            = document.getElementById('leadId');
        this.$fNome          = document.getElementById('leadNomeContato');
        this.$fEmpresa       = document.getElementById('leadEmpresa');
        this.$fEmail         = document.getElementById('leadEmail');
        this.$fTelefone      = document.getElementById('leadTelefone');
        this.$fOrigem        = document.getElementById('leadOrigem');
        this.$fStatus        = document.getElementById('leadStatus');
        this.$fScore         = document.getElementById('leadScore');
        this.$fResponsavel   = document.getElementById('leadResponsavelId');
        this.$fObs           = document.getElementById('leadObservacoes');
        // Modal exclusão
        this.$deleteModal    = document.getElementById('leadDeleteModalBackdrop');
        this.$deleteName     = document.getElementById('leadDeleteName');
        // Modal conversão
        this.$convertModal   = document.getElementById('leadConvertModalBackdrop');
        this.$convertName    = document.getElementById('leadConvertName');
        this.$convertMotivo  = document.getElementById('leadConvertMotivo');
    }

    bindEvents() {
        // Novo lead
        document.getElementById('btnNovoLead').addEventListener('click',     () => this.openModal());
        document.getElementById('btnLeadEmptyNovo').addEventListener('click', () => this.openModal());
        // Modal CRUD
        document.getElementById('leadModalSave').addEventListener('click',    () => this.save());
        document.getElementById('leadModalCancel').addEventListener('click',  () => this.closeModal());
        document.getElementById('leadModalClose').addEventListener('click',   () => this.closeModal());
        this.$modal.addEventListener('click', e => { if (e.target === this.$modal) this.closeModal(); });
        // Modal exclusão
        document.getElementById('leadDeleteModalConfirm').addEventListener('click', () => this.confirmDelete());
        document.getElementById('leadDeleteModalCancel').addEventListener('click',  () => this.closeDeleteModal());
        document.getElementById('leadDeleteModalClose').addEventListener('click',   () => this.closeDeleteModal());
        this.$deleteModal.addEventListener('click', e => { if (e.target === this.$deleteModal) this.closeDeleteModal(); });
        // Modal conversão
        document.getElementById('leadConvertModalConfirm').addEventListener('click', () => this.confirmConvert());
        document.getElementById('leadConvertModalCancel').addEventListener('click',  () => this.closeConvertModal());
        document.getElementById('leadConvertModalClose').addEventListener('click',   () => this.closeConvertModal());
        this.$convertModal.addEventListener('click', e => { if (e.target === this.$convertModal) this.closeConvertModal(); });

        document.addEventListener('keydown', e => {
            if (e.key === 'Escape') {
                if (!this.$modal.classList.contains('hidden'))        this.closeModal();
                if (!this.$deleteModal.classList.contains('hidden'))  this.closeDeleteModal();
                if (!this.$convertModal.classList.contains('hidden')) this.closeConvertModal();
            }
        });

        // Busca
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

        // Filtros
        this.$filterStatus.addEventListener('change', () => { this.filterStatus = this.$filterStatus.value; this.render(); });
        this.$filterOrigem.addEventListener('change', () => { this.filterOrigem = this.$filterOrigem.value; this.render(); });

        // Máscaras
        this.$fTelefone.addEventListener('input', () => {
            this.$fTelefone.value = this.maskPhone(this.$fTelefone.value);
        });
    }

    // ══ FILTRO ══════════════════════════════════════════════════════════

    getFiltered() {
        return this.leads.filter(l => {
            const s = this.searchTerm;
            const matchSearch = !s
                || (l.nomeContato     || '').toLowerCase().includes(s)
                || (l.empresa         || '').toLowerCase().includes(s)
                || (l.email           || '').toLowerCase().includes(s)
                || (l.responsavelNome || '').toLowerCase().includes(s);
            return matchSearch
                && (!this.filterStatus || l.status === this.filterStatus)
                && (!this.filterOrigem || l.origem === this.filterOrigem);
        });
    }

    // ══ RENDER ══════════════════════════════════════════════════════════

    render() {
        const total = this.leads.length;
        this.$kpiTotal.textContent   = total;
        this.$kpiNovo.textContent    = this.leads.filter(l => l.status === 'NOVO').length;
        this.$kpiQualif.textContent  = this.leads.filter(l => l.status === 'QUALIFICADO').length;
        this.$kpiConvert.textContent = this.leads.filter(l => l.status === 'CONVERTIDO').length;

        const filtered = this.getFiltered();
        this.$count.textContent = `${filtered.length} lead${filtered.length !== 1 ? 's' : ''}`;

        if (filtered.length === 0) {
            this.$tbody.innerHTML = '';
            this.$empty.classList.remove('hidden');
        } else {
            this.$empty.classList.add('hidden');
            this.$tbody.innerHTML = filtered.map(l => this.renderRow(l)).join('');
            this.bindRowEvents();
        }
    }

    renderRow(l) {
        const initials = (l.nomeContato || '?').split(' ').slice(0, 2).map(w => w[0]).join('').toUpperCase();
        const color    = LeadsModule.AVATAR_COLORS[l.id % LeadsModule.AVATAR_COLORS.length];
        const origem   = LeadsModule.ORIGEM_LABELS[l.origem] || l.origem || '—';
        const resp     = l.responsavelNome
            ? this.esc(l.responsavelNome)
            : '<span class="text-muted">—</span>';
        const isConverted = l.status === 'CONVERTIDO';

        const convertBtn = isConverted
            ? `<button class="btn btn-sm btn-secondary" disabled title="Já convertido"><i class="fas fa-check"></i></button>`
            : `<button class="btn btn-sm btn-convert" data-id="${l.id}" title="Converter em cliente" style="background:var(--color-success);color:#fff;border:none;border-radius:var(--radius-md);padding:.35rem .55rem;cursor:pointer"><i class="fas fa-user-check"></i></button>`;

        return `
        <tr data-id="${l.id}">
            <td>
                <div class="client-cell">
                    <div class="avatar" style="background:${color}">${initials}</div>
                    <div>
                        <div class="client-name">${this.esc(l.nomeContato)}</div>
                        <div class="client-id">#${l.id.toString().padStart(4,'0')}</div>
                    </div>
                </div>
            </td>
            <td>${l.empresa ? this.esc(l.empresa) : '<span class="text-muted">—</span>'}</td>
            <td>${l.email ? `<a href="mailto:${this.esc(l.email)}">${this.esc(l.email)}</a>` : '<span class="text-muted">—</span>'}</td>
            <td><span class="badge badge-neutral">${this.esc(origem)}</span></td>
            <td>${this.statusBadge(l.status)}</td>
            <td><span class="text-muted">${l.score ?? 0}</span></td>
            <td>${resp}</td>
            <td style="text-align:center">
                <div class="actions" style="gap:.35rem">
                    <button class="btn btn-sm btn-secondary btn-edit" data-id="${l.id}" title="Editar">
                        <i class="fas fa-pen"></i>
                    </button>
                    ${convertBtn}
                    <button class="btn btn-sm btn-danger btn-delete" data-id="${l.id}" title="Excluir">
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
        this.$tbody.querySelectorAll('.btn-convert').forEach(btn =>
            btn.addEventListener('click', () => this.openConvertModal(parseInt(btn.dataset.id))));
    }

    // ══ MODAL ═══════════════════════════════════════════════════════════

    openModal(id = null) {
        this.editingId = id;
        this.clearFormErrors();

        if (id !== null) {
            const l = this.leads.find(x => x.id === id);
            if (!l) return;
            this.$modalTitle.textContent = 'Editar Lead';
            this.$fId.value           = l.id;
            this.$fNome.value         = l.nomeContato     || '';
            this.$fEmpresa.value      = l.empresa         || '';
            this.$fEmail.value        = l.email           || '';
            this.$fTelefone.value     = l.telefone        || '';
            this.$fOrigem.value       = l.origem          || '';
            this.$fStatus.value       = l.status          || 'NOVO';
            this.$fScore.value        = l.score ?? '';
            this.$fResponsavel.value  = l.responsavelId   ?? '';
            this.$fObs.value          = l.observacoes     || '';
        } else {
            this.$modalTitle.textContent = 'Novo Lead';
            this.$form.reset();
            this.$fId.value = '';
        }

        this.$modal.classList.remove('hidden');
        setTimeout(() => this.$fNome.focus(), 50);
    }

    closeModal() { this.$modal.classList.add('hidden'); this.editingId = null; }

    async save() {
        if (!this.validateForm()) return;

        const respVal = this.$fResponsavel.value.trim();
        const data = {
            nomeContato:   this.$fNome.value.trim(),
            empresa:       this.$fEmpresa.value.trim()   || null,
            email:         this.$fEmail.value.trim()     || null,
            telefone:      this.$fTelefone.value.trim()  || null,
            origem:        this.$fOrigem.value,
            status:        this.$fStatus.value            || 'NOVO',
            score:         this.$fScore.value ? parseInt(this.$fScore.value) : 0,
            observacoes:   this.$fObs.value.trim()       || null,
            responsavelId: respVal ? parseInt(respVal)    : null,
        };

        const btn = document.getElementById('leadModalSave');
        btn.disabled = true;
        try {
            if (this.editingId !== null) {
                const updated = await this.apiUpdate(this.editingId, data);
                const idx = this.leads.findIndex(l => l.id === this.editingId);
                if (idx > -1) this.leads[idx] = updated;
                this.toast('success', 'fas fa-check-circle', `Lead <strong>${this.esc(data.nomeContato)}</strong> atualizado.`);
            } else {
                const created = await this.apiCreate(data);
                this.leads.unshift(created);
                this.toast('success', 'fas fa-check-circle', `Lead <strong>${this.esc(data.nomeContato)}</strong> criado.`);
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
        if (!this.$fNome.value.trim()) { this.showFieldError(this.$fNome, 'leadErrNome'); ok = false; }
        if (!this.$fOrigem.value)      { this.showFieldError(this.$fOrigem, 'leadErrOrigem'); ok = false; }
        const email = this.$fEmail.value.trim();
        if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
            this.showFieldError(this.$fEmail, 'leadErrEmail'); ok = false;
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
        const l = this.leads.find(x => x.id === id);
        if (!l) return;
        this.deletingId = id;
        this.$deleteName.textContent = l.nomeContato;
        this.$deleteModal.classList.remove('hidden');
    }

    closeDeleteModal() { this.$deleteModal.classList.add('hidden'); this.deletingId = null; }

    async confirmDelete() {
        const l = this.leads.find(x => x.id === this.deletingId);
        if (!l) return;
        const btn = document.getElementById('leadDeleteModalConfirm');
        btn.disabled = true;
        try {
            await this.apiDelete(this.deletingId);
            this.closeDeleteModal();
            this.toast('success', 'fas fa-check-circle', `Lead <strong>${this.esc(l.nomeContato)}</strong> excluído.`);
            await this.loadData();
        } catch (e) {
            if (e.isAuthError) return;
            this.toast('danger', 'fas fa-exclamation-circle', `Erro ao excluir: ${this.esc(e.message)}`);
            this.closeDeleteModal();
        } finally {
            btn.disabled = false;
        }
    }

    // ══ MODAL CONVERSÃO ═════════════════════════════════════════════════

    openConvertModal(id) {
        const l = this.leads.find(x => x.id === id);
        if (!l) return;
        if (l.status === 'CONVERTIDO') {
            this.toast('warning', 'fas fa-info-circle', 'Este lead já foi convertido.');
            return;
        }
        this.convertingId = id;
        this.$convertName.textContent = l.nomeContato;
        this.$convertMotivo.value = '';
        this.$convertModal.classList.remove('hidden');
    }

    closeConvertModal() { this.$convertModal.classList.add('hidden'); this.convertingId = null; }

    async confirmConvert() {
        const l = this.leads.find(x => x.id === this.convertingId);
        if (!l) return;
        const btn = document.getElementById('leadConvertModalConfirm');
        btn.disabled = true;
        try {
            const motivo = this.$convertMotivo.value.trim() || null;
            const usuarioId = l.responsavelId || 1; // usa responsável do lead ou admin
            const clienteCriado = await this.apiConvert(this.convertingId, usuarioId, motivo);
            this.closeConvertModal();
            this.toast('success', 'fas fa-user-check',
                `Lead <strong>${this.esc(l.nomeContato)}</strong> convertido em cliente <strong>#${clienteCriado.id}</strong>.`, 6000);
            await this.loadData();
        } catch (e) {
            if (e.isAuthError) return;
            this.toast('danger', 'fas fa-exclamation-circle', `Erro ao converter: ${this.esc(e.message)}`);
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

    statusBadge(status) {
        const map = {
            'NOVO':             { cls: 'badge-info',    icon: 'fa-circle-dot'   },
            'QUALIFICADO':      { cls: 'badge-primary', icon: 'fa-star'         },
            'EM_CONTATO':       { cls: 'badge-warning', icon: 'fa-phone'        },
            'PROPOSTA_ENVIADA': { cls: 'badge-warning', icon: 'fa-file-alt'     },
            'PERDIDO':          { cls: 'badge-danger',  icon: 'fa-circle-xmark' },
            'CONVERTIDO':       { cls: 'badge-success', icon: 'fa-circle-check' },
        };
        const s = map[status] || { cls: 'badge-neutral', icon: 'fa-circle' };
        const label = LeadsModule.STATUS_LABELS[status] || status;
        return `<span class="badge ${s.cls}"><i class="fas ${s.icon}"></i> ${label}</span>`;
    }

    maskPhone(v) {
        v = v.replace(/\D/g, '').slice(0, 11);
        if (v.length > 10) return v.replace(/^(\d{2})(\d{5})(\d{4})$/, '($1) $2-$3');
        if (v.length > 6)  return v.replace(/^(\d{2})(\d{4,5})(\d{0,4})$/, '($1) $2-$3');
        if (v.length > 2)  return v.replace(/^(\d{2})(\d+)$/, '($1) $2');
        return v;
    }

    esc(str) {
        return String(str)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;')
            .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }
}

window.addEventListener('app:ready', () => {
    if (window.appSessionValid) {
        window.leadsModule = new LeadsModule();
    }
});
