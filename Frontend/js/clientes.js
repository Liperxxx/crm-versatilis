//
// CLIENTES.JS — Módulo completo de gestão de clientes
//

const API_CLIENTES = `${API_BASE_URL}/clientes`;

class ClientesModule {

    // ── Dados mock (usados quando o backend não está disponível) ─────────
    static MOCK_DATA = [
        { id: 1, nome: 'Fiat Chrysler Brasil',      email: 'contato@fiat.com.br',        telefone: '(11) 3900-4500',   segmento: 'Indústria',    status: 'Ativo',      cidade: 'Betim - MG',          observacoes: '',                             desde: '2022-03-15' },
        { id: 2, nome: 'Mercado Livre',              email: 'parceiros@mercadolivre.com', telefone: '(11) 4003-8000',   segmento: 'Tecnologia',   status: 'Ativo',      cidade: 'São Paulo - SP',      observacoes: 'Contrato anual renovado.',      desde: '2021-07-01' },
        { id: 3, nome: 'Grupo Carrefour',            email: 'comercial@carrefour.com.br', telefone: '(11) 3779-3000',   segmento: 'Varejo',       status: 'Prospecto',  cidade: 'Boulogne - SP',       observacoes: 'Aguardando proposta.',          desde: '2024-01-20' },
        { id: 4, nome: 'Vivo Telecomunicações',      email: 'b2b@vivo.com.br',            telefone: '(11) 3430-5000',   segmento: 'Serviços',     status: 'Inativo',    cidade: 'São Paulo - SP',      observacoes: 'Contrato encerrado em Jan/25.', desde: '2020-05-10' },
        { id: 5, nome: 'Agropecuária Santa Helena',  email: 'financeiro@santahelena.agr', telefone: '(34) 9 9812-3456', segmento: 'Agronegócio', status: 'Ativo',      cidade: 'Uberlândia - MG',     observacoes: '',                             desde: '2023-09-05' },
        { id: 6, nome: 'TechSolutions Ltda',         email: 'rh@techsolutions.com.br',    telefone: '(21) 3002-7000',   segmento: 'Tecnologia',   status: 'Prospecto',  cidade: 'Rio de Janeiro - RJ', observacoes: 'Enviar deck comercial.',        desde: '2025-02-14' },
    ];

    // Paletas de cores para avatar — derivadas da paleta Versatilis
    static AVATAR_COLORS = [
        '#CD5A26','#306EB4','#112251','#B84E1F','#122749','#FF9300','#FAA532','#10b981'
    ];

    constructor() {
        this.clientes       = [];
        this.nextId         = 1;   // usado apenas no modo mock
        this.editingId      = null;
        this.deletingId     = null;
        this.searchTerm     = '';
        this.filterStatus   = '';
        this.filterSegmento = '';
        this.usingMock      = false;
        this.init();
    }

    // ══ INICIALIZAÇÃO ════════════════════════════════════════════════════

    init() {
        this.bindElements();
        this.bindEvents();
        this.loadData(); // carrega da API ou mock
    }

    // ══ TOKEN ══════════════════════════════════════════════════════════════

    getToken() {
        return localStorage.getItem('crm_token')
            || localStorage.getItem('token')
            || localStorage.getItem('jwtToken')
            || null;
    }

    authHeaders() {
        const token = this.getToken();
        return token ? { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' }
                     : { 'Content-Type': 'application/json' };
    }

    // ══ CARREGAMENTO DE DADOS (sempre via API) ════════════════════════════

    async loadData() {
        this.showLoading();
        try {
            const res = await fetch(`${API_CLIENTES}?size=500&sort=id,desc`, {
                headers: this.authHeaders()
            });
            if (res.status === 401 || res.status === 403) {
                this.clientes = [];
                this.toast('danger', 'fas fa-lock',
                    'Sessão expirada ou não autenticado. <a href="login.html" style="color:inherit;text-decoration:underline;font-weight:600">Faça login</a>.',
                    10000);
                return;
            }
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const json = await res.json();
            // ResponseDTO<Page<ClienteDTO>> → dados.content
            this.clientes = json.dados?.content ?? json.dados ?? [];
        } catch (e) {
            console.error('[Clientes] Erro ao carregar:', e.message);
            this.clientes = [];
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
                <td colspan="7">
                    <div class="table-loading">
                        <i class="fas fa-spinner"></i>
                        <span>Carregando clientes...</span>
                    </div>
                </td>
            </tr>`;
    }

    // ══ CRUD ASSÍNCRONO (API) ══════════════════════════════════════════════

    async apiCreate(data) {
        const res = await fetch(API_CLIENTES, {
            method: 'POST',
            headers: this.authHeaders(),
            body: JSON.stringify(data)
        });
        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            throw new Error(err.mensagem || `HTTP ${res.status}`);
        }
        const json = await res.json();
        return json.dados;
    }

    async apiUpdate(id, data) {
        const res = await fetch(`${API_CLIENTES}/${id}`, {
            method: 'PUT',
            headers: this.authHeaders(),
            body: JSON.stringify(data)
        });
        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            throw new Error(err.mensagem || `HTTP ${res.status}`);
        }
        const json = await res.json();
        return json.dados;
    }

    async apiDelete(id) {
        const res = await fetch(`${API_CLIENTES}/${id}`, {
            method: 'DELETE',
            headers: this.authHeaders()
        });
        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            throw new Error(err.mensagem || `HTTP ${res.status}`);
        }
    }

    bindElements() {
        // Tabela
        this.$tbody         = document.getElementById('clientesTableBody');
        this.$empty         = document.getElementById('clientesEmpty');
        this.$count         = document.getElementById('clienteCount');

        // KPIs
        this.$kpiTotal      = document.getElementById('kpiTotal');
        this.$kpiAtivo      = document.getElementById('kpiAtivo');
        this.$kpiProspecto  = document.getElementById('kpiProspecto');
        this.$kpiInativo    = document.getElementById('kpiInativo');

        // Toolbar
        this.$search        = document.getElementById('clienteSearch');
        this.$searchClear   = document.getElementById('clienteSearchClear');
        this.$filterStatus  = document.getElementById('clienteFilterStatus');
        this.$filterSeg     = document.getElementById('clienteFilterSegmento');

        // Página do formulário
        this.$formTitle     = document.getElementById('cliFormTitle');
        this.$form          = document.getElementById('clienteForm');
        this.$fId           = document.getElementById('clienteId');
        this.$fNome         = document.getElementById('clienteNome');
        this.$fStatus       = document.getElementById('clienteStatus');
        this.$fEmail        = document.getElementById('clienteEmail');
        this.$fTelefone     = document.getElementById('clienteTelefone');
        this.$fSegmento     = document.getElementById('clienteSegmento');
        this.$fCidade       = document.getElementById('clienteCidade');
        this.$fObs          = document.getElementById('clienteObservacoes');
        this.$fTipoDoc      = document.getElementById('clienteTipoDoc');
        this.$fDocumento    = document.getElementById('clienteDocumento');
        this.$fCnpj         = document.getElementById('clienteCnpj'); // campo legado (hidden)

        // Modal exclusão
        this.$deleteModal   = document.getElementById('deleteModalBackdrop');
        this.$deleteName    = document.getElementById('deleteClienteName');
    }

    bindEvents() {
        // Botão "Novo Cliente"
        document.getElementById('btnNovoCliente').addEventListener('click', () => this.openModal());
        document.getElementById('btnEmptyNovo').addEventListener('click',   () => this.openModal());

        // Botões da página de formulário
        document.getElementById('cliFormSave').addEventListener('click',   () => this.saveCliente());
        document.getElementById('cliFormCancel').addEventListener('click', () => this.closeModal());
        document.getElementById('cliFormBack').addEventListener('click',   () => this.closeModal());

        // Modal exclusão — confirmar / cancelar / fechar
        document.getElementById('deleteModalConfirm').addEventListener('click',  () => this.confirmDelete());
        document.getElementById('deleteModalCancel').addEventListener('click',   () => this.closeDeleteModal());
        document.getElementById('deleteModalClose').addEventListener('click',    () => this.closeDeleteModal());
        this.$deleteModal.addEventListener('click', e => { if (e.target === this.$deleteModal) this.closeDeleteModal(); });

        // Fechar modal exclusão com Escape
        document.addEventListener('keydown', e => {
            if (e.key === 'Escape') {
                if (window.navigationManager?.currentModule === 'cliente-form') this.closeModal();
                if (!this.$deleteModal.classList.contains('hidden')) this.closeDeleteModal();
            }
        });

        // Busca
        this.$search.addEventListener('input', () => {
            this.searchTerm = this.$search.value.trim().toLowerCase();
            this.$searchClear.classList.toggle('hidden', !this.searchTerm);
            this.render();
        });

        this.$searchClear.addEventListener('click', () => {
            this.$search.value = '';
            this.searchTerm = '';
            this.$searchClear.classList.add('hidden');
            this.$search.focus();
            this.render();
        });

        // Filtros
        this.$filterStatus.addEventListener('change', () => {
            this.filterStatus = this.$filterStatus.value;
            this.render();
        });

        this.$filterSeg.addEventListener('change', () => {
            this.filterSegmento = this.$filterSeg.value;
            this.render();
        });

        // Máscara simples de telefone
        this.$fTelefone.addEventListener('input', () => {
            this.$fTelefone.value = this.maskPhone(this.$fTelefone.value);
        });

        // Troca de tipo de documento (CNPJ ↔ CPF)
        this.$fTipoDoc?.addEventListener('change', () => {
            this.updateDocumentField();
        });

        // Máscara dinâmica do campo de documento
        this.$fDocumento?.addEventListener('input', () => {
            const tipo = this.$fTipoDoc?.value || 'cnpj';
            this.$fDocumento.value = tipo === 'cpf'
                ? this.maskCpf(this.$fDocumento.value)
                : this.maskCnpj(this.$fDocumento.value);
        });

        // Máscara simples de CNPJ (campo legado hidden — mantido para compatibilidade)
        this.$fCnpj?.addEventListener('input', () => {
            this.$fCnpj.value = this.maskCnpj(this.$fCnpj.value);
        });
    }

    // ══ FILTRAGEM ════════════════════════════════════════════════════════

    getFiltered() {
        return this.clientes.filter(c => {
            const matchSearch = !this.searchTerm
                || c.nome.toLowerCase().includes(this.searchTerm)
                || (c.email         || '').toLowerCase().includes(this.searchTerm)
                || (c.telefone      || '').toLowerCase().includes(this.searchTerm)
                || (c.cnpj         || '').toLowerCase().includes(this.searchTerm)
                || (c.cpf          || '').toLowerCase().includes(this.searchTerm)
                || (c.cidade       || '').toLowerCase().includes(this.searchTerm);

            const matchStatus   = !this.filterStatus   || c.status   === this.filterStatus;
            const matchSegmento = !this.filterSegmento || c.segmento === this.filterSegmento;

            return matchSearch && matchStatus && matchSegmento;
        });
    }

    // ══ RENDER ═══════════════════════════════════════════════════════════

    render() {
        const filtered = this.getFiltered();

        // KPIs
        const total   = this.clientes.length;
        const ativos  = this.clientes.filter(c => c.status === 'Ativo').length;
        const prosp   = this.clientes.filter(c => c.status === 'Prospecto').length;
        const inat    = this.clientes.filter(c => c.status === 'Inativo').length;

        this.$kpiTotal.textContent     = total;
        this.$kpiAtivo.textContent     = ativos;
        this.$kpiProspecto.textContent = prosp;
        this.$kpiInativo.textContent   = inat;

        // Contagem
        const count = filtered.length;
        this.$count.textContent = count === 1 ? '1 cliente' : `${count} clientes`;

        // Empty state vs tabela
        if (total === 0) {
            this.$tbody.innerHTML = '';
            this.$empty.classList.remove('hidden');
        } else {
            this.$empty.classList.add('hidden');
            this.$tbody.innerHTML = filtered.map(c => this.renderRow(c)).join('');
            this.bindRowEvents();
        }
    }

    renderRow(c) {
        const initials = c.nome.split(' ').slice(0, 2).map(w => w[0]).join('').toUpperCase();
        const color    = ClientesModule.AVATAR_COLORS[c.id % ClientesModule.AVATAR_COLORS.length];
        const badge    = this.statusBadge(c.status);
        const desde    = this.formatDate(c.desde);

        return `
        <tr data-id="${c.id}">
            <td>
                <div class="client-cell">
                    <div class="avatar" style="background:${color}">${initials}</div>
                    <div>
                        <div class="client-name">${this.esc(c.nome)}</div>
                        <div class="client-id">#${c.id.toString().padStart(4,'0')}</div>
                    </div>
                </div>
            </td>
            <td>${c.email ? `<a href="mailto:${this.esc(c.email)}">${this.esc(c.email)}</a>` : '<span class="text-muted">—</span>'}</td>
            <td>${c.telefone ? this.esc(c.telefone) : '<span class="text-muted">—</span>'}</td>
            <td>${c.segmento ? this.esc(c.segmento) : '<span class="text-muted">—</span>'}</td>
            <td>${badge}</td>
            <td><span class="text-muted">${desde}</span></td>
            <td style="text-align:center">
                <div class="actions">
                    <button class="btn btn-sm btn-secondary btn-edit" data-id="${c.id}" title="Editar">
                        <i class="fas fa-pen"></i>
                    </button>
                    <button class="btn btn-sm btn-danger btn-delete" data-id="${c.id}" title="Excluir">
                        <i class="fas fa-trash-alt"></i>
                    </button>
                </div>
            </td>
        </tr>`;
    }

    bindRowEvents() {
        this.$tbody.querySelectorAll('.btn-edit').forEach(btn => {
            btn.addEventListener('click', () => this.openModal(parseInt(btn.dataset.id)));
        });
        this.$tbody.querySelectorAll('.btn-delete').forEach(btn => {
            btn.addEventListener('click', () => this.openDeleteModal(parseInt(btn.dataset.id)));
        });
    }

    // ══ MODAL CLIENTE ════════════════════════════════════════════════════

    openModal(id = null) {
        this.editingId = id;
        this.clearFormErrors();

        const pageLabel = id !== null ? 'Editar Cliente' : 'Novo Cliente';
        if (this.$formTitle) this.$formTitle.textContent = pageLabel;
        const sub = document.getElementById('cliFormBreadcrumbSub');
        if (sub) sub.textContent = pageLabel;

        if (id !== null) {
            const c = this.clientes.find(x => x.id === id);
            if (!c) return;
            this.$fId.value         = c.id;
            this.$fNome.value       = c.nome;
            this.$fStatus.value     = c.status;
            this.$fEmail.value      = c.email        || '';
            this.$fTelefone.value   = c.telefone     || '';
            if (c.cpf) {
                if (this.$fTipoDoc) this.$fTipoDoc.value = 'cpf';
                if (this.$fDocumento) this.$fDocumento.value = c.cpf;
            } else {
                if (this.$fTipoDoc) this.$fTipoDoc.value = 'cnpj';
                if (this.$fDocumento) this.$fDocumento.value = c.cnpj || '';
            }
            this.updateDocumentField();
            this.$fSegmento.value   = c.segmento     || '';
            this.$fCidade.value     = c.cidade       || '';
            this.$fObs.value        = c.observacoes  || '';
        } else {
            this.$form.reset();
            this.$fId.value = '';
            this.updateDocumentField();
        }

        if (window.navigationManager) window.navigationManager.navigateTo('cliente-form');
        setTimeout(() => this.$fNome?.focus(), 80);
    }

    closeModal() {
        this.editingId = null;
        if (window.navigationManager) window.navigationManager.navigateTo('clientes');
    }

    async saveCliente() {
        if (!this.validateForm()) return;

        const tipoDoc = this.$fTipoDoc?.value || 'cnpj';
        const docVal  = this.$fDocumento?.value?.trim() || null;
        const data = {
            nome:         this.$fNome.value.trim(),
            status:       this.$fStatus.value,
            email:        this.$fEmail.value.trim(),
            telefone:     this.$fTelefone.value.trim(),
            cnpj:         tipoDoc === 'cnpj' ? docVal : null,
            cpf:          tipoDoc === 'cpf'  ? docVal : null,
            segmento:     this.$fSegmento.value,
            cidade:       this.$fCidade.value.trim(),
            observacoes:  this.$fObs.value.trim(),
        };

        const btn = document.getElementById('cliFormSave');
        btn.disabled = true;
        try {
            if (this.editingId !== null) {
                const updated = await this.apiUpdate(this.editingId, data);
                const idx = this.clientes.findIndex(c => c.id === this.editingId);
                if (idx > -1) this.clientes[idx] = updated;
                this.toast('success', 'fas fa-check-circle', `Cliente <strong>${this.esc(data.nome)}</strong> atualizado.`);
            } else {
                const created = await this.apiCreate(data);
                this.clientes.unshift(created);
                this.toast('success', 'fas fa-check-circle', `Cliente <strong>${this.esc(data.nome)}</strong> cadastrado.`);
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

        if (!this.$fNome.value.trim()) {
            this.showFieldError(this.$fNome, 'errNome');
            ok = false;
        }

        if (!this.$fStatus.value) {
            this.showFieldError(this.$fStatus, 'errStatus');
            ok = false;
        }

        const email = this.$fEmail.value.trim();
        if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
            this.showFieldError(this.$fEmail, 'errEmail');
            ok = false;
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
        const c = this.clientes.find(x => x.id === id);
        if (!c) return;
        this.deletingId = id;
        this.$deleteName.textContent = c.nome;
        this.$deleteModal.classList.remove('hidden');
    }

    closeDeleteModal() {
        this.$deleteModal.classList.add('hidden');
        this.deletingId = null;
    }

    async confirmDelete() {
        const c = this.clientes.find(x => x.id === this.deletingId);
        if (!c) return;

        const btn = document.getElementById('deleteModalConfirm');
        btn.disabled = true;
        try {
            await this.apiDelete(this.deletingId);
            this.closeDeleteModal();
            this.toast('success', 'fas fa-check-circle', `Cliente <strong>${this.esc(c.nome)}</strong> excluído com sucesso.`);
            await this.loadData(); // ressincronia com o servidor
        } catch (e) {
            this.toast('danger', 'fas fa-exclamation-circle', `Erro ao excluir: ${this.esc(e.message)}`);
            this.closeDeleteModal();
        } finally {
            btn.disabled = false;
        }
    }

    // ══ TOASTS ═══════════════════════════════════════════════════════════

    toast(type, icon, message, duration = 4000) {
        const container = document.getElementById('toastContainer');
        const el = document.createElement('div');
        el.className = `toast toast-${type}`;
        el.innerHTML = `
            <i class="${icon}"></i>
            <span class="toast-msg">${message}</span>
            <button class="toast-close" aria-label="Fechar"><i class="fas fa-times"></i></button>
        `;

        const close = () => {
            el.classList.add('removing');
            el.addEventListener('animationend', () => el.remove(), { once: true });
        };

        el.querySelector('.toast-close').addEventListener('click', close);
        container.appendChild(el);
        setTimeout(close, duration);
    }

    // ══ HELPERS ══════════════════════════════════════════════════════════

    statusBadge(status) {
        const map = {
            'Ativo':     { cls: 'badge-success', icon: 'fa-circle-check' },
            'Inativo':   { cls: 'badge-danger',  icon: 'fa-circle-xmark' },
            'Prospecto': { cls: 'badge-warning',  icon: 'fa-circle-dot'  },
        };
        const s = map[status] || { cls: 'badge-neutral', icon: 'fa-circle' };
        return `<span class="badge ${s.cls}"><i class="fas ${s.icon}"></i> ${status}</span>`;
    }

    formatDate(dateStr) {
        if (!dateStr) return '—';
        const [y, m, d] = dateStr.split('-');
        return `${d}/${m}/${y}`;
    }

    maskPhone(v) {
        v = v.replace(/\D/g, '').slice(0, 11);
        if (v.length > 10) return v.replace(/^(\d{2})(\d{5})(\d{4})$/, '($1) $2-$3');
        if (v.length > 6)  return v.replace(/^(\d{2})(\d{4,5})(\d{0,4})$/, '($1) $2-$3');
        if (v.length > 2)  return v.replace(/^(\d{2})(\d+)$/, '($1) $2');
        return v;
    }

    maskCnpj(v) {
        v = v.replace(/\D/g, '').slice(0, 14);
        if (v.length > 12) return v.replace(/^(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})$/, '$1.$2.$3/$4-$5');
        if (v.length > 8)  return v.replace(/^(\d{2})(\d{3})(\d{3})(\d*)$/, '$1.$2.$3/$4');
        if (v.length > 5)  return v.replace(/^(\d{2})(\d{3})(\d*)$/, '$1.$2.$3');
        if (v.length > 2)  return v.replace(/^(\d{2})(\d*)$/, '$1.$2');
        return v;
    }

    maskCpf(v) {
        v = v.replace(/\D/g, '').slice(0, 11);
        if (v.length > 9) return v.replace(/^(\d{3})(\d{3})(\d{3})(\d{0,2})$/, '$1.$2.$3-$4');
        if (v.length > 6) return v.replace(/^(\d{3})(\d{3})(\d*)$/, '$1.$2.$3');
        if (v.length > 3) return v.replace(/^(\d{3})(\d*)$/, '$1.$2');
        return v;
    }

    updateDocumentField() {
        if (!this.$fTipoDoc || !this.$fDocumento) return;
        const isCpf = this.$fTipoDoc.value === 'cpf';
        this.$fDocumento.placeholder = isCpf ? '000.000.000-00' : '00.000.000/0000-00';
        this.$fDocumento.maxLength   = isCpf ? 14 : 18;
        // re-apply mask to current value when switching types
        if (this.$fDocumento.value) {
            this.$fDocumento.value = isCpf
                ? this.maskCpf(this.$fDocumento.value)
                : this.maskCnpj(this.$fDocumento.value);
        }
    }

    esc(str) {
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }
}

// Inicializa quando o DOM estiver pronto
document.addEventListener('DOMContentLoaded', () => {
    window.clientesModule = new ClientesModule();
});
