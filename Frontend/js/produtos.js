//
// PRODUTOS.JS — Módulo completo de gestão de produtos
//

const API_PRODUTOS = `${API_BASE_URL}/produtos`;

class ProdutosModule {

    static AVATAR_COLORS = [
        '#CD5A26','#306EB4','#112251','#B84E1F','#122749','#FF9300','#FAA532','#10b981'
    ];

    static STATUS_LABELS = {
        DISPONIVEL:     'Disponível',
        INDISPONIVEL:   'Indisponível',
        EM_FALTA:       'Em Falta',
        DESCONTINUADO:  'Descontinuado',
    };

    constructor() {
        this.produtos        = [];
        this.editingId       = null;
        this.deletingId      = null;
        this.searchTerm      = '';
        this.filterStatus    = '';
        this.filterCategoria = '';
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
            const res = await fetch(`${API_PRODUTOS}?size=500&sort=id,desc`, {
                headers: this.authHeaders()
            });
            if (res.status === 401 || res.status === 403) {
                this.produtos = [];
                this.toast('danger', 'fas fa-lock',
                    'Sessão expirada. <a href="login.html" style="color:inherit;text-decoration:underline;font-weight:600">Faça login</a>.',
                    10000);
                return;
            }
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const json = await res.json();
            this.produtos = json.dados?.content ?? json.dados ?? [];
        } catch (e) {
            console.error('[Produtos] Erro ao carregar:', e.message);
            this.produtos = [];
            this.toast('danger', 'fas fa-server', `Não foi possível conectar ao backend: ${this.esc(e.message)}`, 10000);
        } finally {
            this.render();
        }
    }

    showLoading() {
        if (!this.$tbody) return;
        this.$empty?.classList.add('hidden');
        this.$tbody.innerHTML = `
            <tr class="loading-row">
                <td colspan="6">
                    <div class="table-loading">
                        <i class="fas fa-spinner"></i>
                        <span>Carregando produtos...</span>
                    </div>
                </td>
            </tr>`;
    }

    // ══ API ═════════════════════════════════════════════════════════════

    async apiCreate(data) {
        const res = await fetch(API_PRODUTOS, {
            method: 'POST', headers: this.authHeaders(), body: JSON.stringify(data)
        });
        if (!res.ok) { const e = await res.json().catch(() => ({})); throw new Error(e.mensagem || `HTTP ${res.status}`); }
        return (await res.json()).dados;
    }

    async apiUpdate(id, data) {
        const res = await fetch(`${API_PRODUTOS}/${id}`, {
            method: 'PUT', headers: this.authHeaders(), body: JSON.stringify(data)
        });
        if (!res.ok) { const e = await res.json().catch(() => ({})); throw new Error(e.mensagem || `HTTP ${res.status}`); }
        return (await res.json()).dados;
    }

    async apiDelete(id) {
        const res = await fetch(`${API_PRODUTOS}/${id}`, {
            method: 'DELETE', headers: this.authHeaders()
        });
        if (!res.ok) { const e = await res.json().catch(() => ({})); throw new Error(e.mensagem || `HTTP ${res.status}`); }
    }

    // ══ BIND ════════════════════════════════════════════════════════════

    bindElements() {
        this.$tbody           = document.getElementById('produtosTableBody');
        this.$empty           = document.getElementById('produtosEmpty');
        this.$count           = document.getElementById('prodCount');
        this.$kpiTotal        = document.getElementById('prodKpiTotal');
        this.$kpiDisponivel   = document.getElementById('prodKpiDisponivel');
        this.$kpiEmFalta      = document.getElementById('prodKpiEmFalta');
        this.$kpiDescont      = document.getElementById('prodKpiDescontinuado');
        this.$search          = document.getElementById('prodSearch');
        this.$searchClear     = document.getElementById('prodSearchClear');
        this.$filterCategoria = document.getElementById('prodFilterCategoria');
        this.$filterStatus    = document.getElementById('prodFilterStatus');
        this.$modal           = document.getElementById('prodModalBackdrop');
        this.$modalTitle      = document.getElementById('prodModalTitle');
        this.$form            = document.getElementById('prodForm');
        this.$fId             = document.getElementById('prodId');
        this.$fNome           = document.getElementById('prodNome');
        this.$fPreco          = document.getElementById('prodPreco');
        this.$fCategoria      = document.getElementById('prodCategoria');
        this.$fEstoque        = document.getElementById('prodEstoque');
        this.$fStatus         = document.getElementById('prodStatus');
        this.$fDescricao      = document.getElementById('prodDescricao');
        this.$deleteModal     = document.getElementById('prodDeleteModalBackdrop');
        this.$deleteName      = document.getElementById('prodDeleteName');
    }

    bindEvents() {
        document.getElementById('btnNovoProduto').addEventListener('click',  () => this.openModal());
        document.getElementById('btnProdEmptyNovo').addEventListener('click', () => this.openModal());
        document.getElementById('prodModalSave').addEventListener('click',   () => this.save());
        document.getElementById('prodModalCancel').addEventListener('click', () => this.closeModal());
        document.getElementById('prodModalClose').addEventListener('click',  () => this.closeModal());
        this.$modal.addEventListener('click', e => { if (e.target === this.$modal) this.closeModal(); });

        document.getElementById('prodDeleteModalConfirm').addEventListener('click', () => this.confirmDelete());
        document.getElementById('prodDeleteModalCancel').addEventListener('click',  () => this.closeDeleteModal());
        document.getElementById('prodDeleteModalClose').addEventListener('click',   () => this.closeDeleteModal());
        this.$deleteModal.addEventListener('click', e => { if (e.target === this.$deleteModal) this.closeDeleteModal(); });

        document.addEventListener('keydown', e => {
            if (e.key === 'Escape') {
                if (!this.$modal.classList.contains('hidden'))       this.closeModal();
                if (!this.$deleteModal.classList.contains('hidden')) this.closeDeleteModal();
            }
        });

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

        this.$filterCategoria.addEventListener('change', () => { this.filterCategoria = this.$filterCategoria.value; this.render(); });
        this.$filterStatus.addEventListener('change', () => { this.filterStatus = this.$filterStatus.value; this.render(); });
    }

    // ══ FILTRO ══════════════════════════════════════════════════════════

    updateCategoryFilter() {
        const cats = [...new Set(this.produtos.map(p => p.categoria).filter(Boolean))].sort();
        const prev = this.$filterCategoria.value;
        this.$filterCategoria.innerHTML = '<option value="">Todas as categorias</option>'
            + cats.map(c => `<option value="${this.esc(c)}"${c === prev ? ' selected' : ''}>${this.esc(c)}</option>`).join('');
    }

    getFiltered() {
        return this.produtos.filter(p => {
            const s = this.searchTerm;
            const matchSearch = !s
                || (p.nome      || '').toLowerCase().includes(s)
                || (p.categoria || '').toLowerCase().includes(s);
            return matchSearch
                && (!this.filterCategoria || (p.categoria || '') === this.filterCategoria)
                && (!this.filterStatus    || p.status === this.filterStatus);
        });
    }

    // ══ RENDER ══════════════════════════════════════════════════════════

    render() {
        const total = this.produtos.length;
        this.$kpiTotal.textContent      = total;
        this.$kpiDisponivel.textContent = this.produtos.filter(p => p.status === 'DISPONIVEL').length;
        this.$kpiEmFalta.textContent    = this.produtos.filter(p => p.status === 'EM_FALTA').length;
        this.$kpiDescont.textContent    = this.produtos.filter(p => p.status === 'DESCONTINUADO').length;

        this.updateCategoryFilter();

        const filtered = this.getFiltered();
        this.$count.textContent = `${filtered.length} produto${filtered.length !== 1 ? 's' : ''}`;

        if (filtered.length === 0) {
            this.$tbody.innerHTML = '';
            this.$empty.classList.remove('hidden');
        } else {
            this.$empty.classList.add('hidden');
            this.$tbody.innerHTML = filtered.map(p => this.renderRow(p)).join('');
            this.bindRowEvents();
        }
    }

    renderRow(p) {
        const initials = (p.nome || '?').split(' ').slice(0, 2).map(w => w[0]).join('').toUpperCase();
        const color    = ProdutosModule.AVATAR_COLORS[p.id % ProdutosModule.AVATAR_COLORS.length];
        const preco    = this.formatCurrency(p.precoUnitario);
        const estoque  = p.estoque != null ? p.estoque : '—';

        return `
        <tr data-id="${p.id}">
            <td>
                <div class="client-cell">
                    <div class="avatar" style="background:${color};border-radius:var(--radius-lg)">${initials}</div>
                    <div>
                        <div class="client-name">${this.esc(p.nome)}</div>
                        <div class="client-id">#${p.id.toString().padStart(4,'0')}</div>
                    </div>
                </div>
            </td>
            <td>${p.categoria ? this.esc(p.categoria) : '<span class="text-muted">—</span>'}</td>
            <td><strong>${preco}</strong></td>
            <td>
                <span class="${p.estoque === 0 ? 'text-muted' : ''}">${estoque}</span>
            </td>
            <td>${this.statusBadge(p.status)}</td>
            <td style="text-align:center">
                <div class="actions">
                    <button class="btn btn-sm btn-secondary btn-edit" data-id="${p.id}" title="Editar">
                        <i class="fas fa-pen"></i>
                    </button>
                    <button class="btn btn-sm btn-danger btn-delete" data-id="${p.id}" title="Excluir">
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

        if (id !== null) {
            const p = this.produtos.find(x => x.id === id);
            if (!p) return;
            this.$modalTitle.textContent = 'Editar Produto';
            this.$fId.value         = p.id;
            this.$fNome.value       = p.nome           || '';
            this.$fPreco.value      = p.precoUnitario  ?? '';
            this.$fCategoria.value  = p.categoria      || '';
            this.$fEstoque.value    = p.estoque ?? '';
            this.$fStatus.value     = p.status         || 'DISPONIVEL';
            this.$fDescricao.value  = p.descricao      || '';
        } else {
            this.$modalTitle.textContent = 'Novo Produto';
            this.$form.reset();
            this.$fId.value = '';
        }

        this.$modal.classList.remove('hidden');
        setTimeout(() => this.$fNome.focus(), 50);
    }

    closeModal() { this.$modal.classList.add('hidden'); this.editingId = null; }

    async save() {
        if (!this.validateForm()) return;

        const preco = parseFloat(this.$fPreco.value);
        const data = {
            nome:          this.$fNome.value.trim(),
            descricao:     this.$fDescricao.value.trim() || null,
            precoUnitario: preco,
            categoria:     this.$fCategoria.value.trim() || null,
            estoque:       this.$fEstoque.value !== '' ? parseInt(this.$fEstoque.value) : 0,
            status:        this.$fStatus.value || 'DISPONIVEL',
        };

        const btn = document.getElementById('prodModalSave');
        btn.disabled = true;
        try {
            if (this.editingId !== null) {
                const updated = await this.apiUpdate(this.editingId, data);
                const idx = this.produtos.findIndex(p => p.id === this.editingId);
                if (idx > -1) this.produtos[idx] = updated;
                this.toast('success', 'fas fa-check-circle', `Produto <strong>${this.esc(data.nome)}</strong> atualizado.`);
            } else {
                const created = await this.apiCreate(data);
                this.produtos.unshift(created);
                this.toast('success', 'fas fa-check-circle', `Produto <strong>${this.esc(data.nome)}</strong> cadastrado.`);
            }
            this.closeModal();
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
        if (!this.$fNome.value.trim()) { this.showFieldError(this.$fNome, 'prodErrNome'); ok = false; }
        const preco = parseFloat(this.$fPreco.value);
        if (!this.$fPreco.value || isNaN(preco) || preco <= 0) {
            this.showFieldError(this.$fPreco, 'prodErrPreco'); ok = false;
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
        const p = this.produtos.find(x => x.id === id);
        if (!p) return;
        this.deletingId = id;
        this.$deleteName.textContent = p.nome;
        this.$deleteModal.classList.remove('hidden');
    }

    closeDeleteModal() { this.$deleteModal.classList.add('hidden'); this.deletingId = null; }

    async confirmDelete() {
        const p = this.produtos.find(x => x.id === this.deletingId);
        if (!p) return;
        const btn = document.getElementById('prodDeleteModalConfirm');
        btn.disabled = true;
        try {
            await this.apiDelete(this.deletingId);
            this.closeDeleteModal();
            this.toast('success', 'fas fa-check-circle', `Produto <strong>${this.esc(p.nome)}</strong> excluído.`);
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

    statusBadge(status) {
        const map = {
            'DISPONIVEL':    { cls: 'badge-success', icon: 'fa-circle-check'      },
            'INDISPONIVEL':  { cls: 'badge-neutral',  icon: 'fa-circle-xmark'     },
            'EM_FALTA':      { cls: 'badge-warning', icon: 'fa-triangle-exclamation' },
            'DESCONTINUADO': { cls: 'badge-danger',  icon: 'fa-ban'               },
        };
        const s = map[status] || { cls: 'badge-neutral', icon: 'fa-circle' };
        const label = ProdutosModule.STATUS_LABELS[status] || status;
        return `<span class="badge ${s.cls}"><i class="fas ${s.icon}"></i> ${label}</span>`;
    }

    formatCurrency(value) {
        if (value == null) return '—';
        return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
    }

    esc(str) {
        return String(str)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;')
            .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }
}

document.addEventListener('DOMContentLoaded', () => {
    window.produtosModule = new ProdutosModule();
});
