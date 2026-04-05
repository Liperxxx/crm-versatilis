//
// ORCAMENTOS.JS — Módulo de Orçamentos / Propostas Comerciais
//

const API_ORCAMENTOS = `${API_BASE_URL}/orcamentos`;
// API_PRODUTOS já declarada em produtos.js (escopo global compartilhado)

class OrcamentosModule {

    static STATUS_LABELS = {
        RASCUNHO: 'Rascunho',
        ENVIADO:  'Enviado',
        APROVADO: 'Aprovado',
        RECUSADO: 'Recusado',
    };

    // ══════════════════════════════════════════════════════════════════
    // PAPEL TIMBRADO — Edite estas constantes para personalizar
    // ══════════════════════════════════════════════════════════════════
    static EMPRESA = {
        nome:     'Versatilis',
        sigla:    'V',
        cnpj:     '00.000.000/0001-00',
        endereco: 'Endereço da empresa',
        cidade:   'Cidade - UF',
        telefone: '(00) 0000-0000',
        email:    'contato@versatilis.com.br',
        site:     'www.versatilis.com.br',
    };

    constructor() {
        this.orcamentos  = [];
        this.editingId    = null;
        this.deletingId  = null;
        this.searchTerm  = '';
        this.filterStatus  = '';
        this.filterCliente = '';
        this.filterDataDe  = '';
        this.filterDataAte = '';
        this.itemSeq     = 0;
        this.clientesCache = [];
        this.init();
    }

    init() {
        try {
            this.bindElements();
            console.log('[Orcamentos] bindElements OK');
            this.bindEvents();
            console.log('[Orcamentos] bindEvents OK — btnNovoOrcamento listener attached');
            this.loadClientes();
            this.loadData();
        } catch (e) {
            console.error('[Orcamentos] INIT FAILED:', e);
        }
    }

    // ══ AUTH ════════════════════════════════════════════════════════════

    getToken() { return localStorage.getItem('crm_token') || localStorage.getItem('token') || null; }

    authHeaders() {
        const t = this.getToken();
        return t ? { 'Authorization': `Bearer ${t}`, 'Content-Type': 'application/json' }
                 : { 'Content-Type': 'application/json' };
    }

    // ══ LOAD ════════════════════════════════════════════════════════════

    async loadData() {
        this.showLoading();
        try {
            const res = await fetch(`${API_ORCAMENTOS}?size=500&sort=id,desc`, { headers: this.authHeaders() });
            if (res.status === 401 || res.status === 403) {
                this.orcamentos = [];
                this.toast('danger', 'fas fa-lock',
                    'Sessão expirada. <a href="login.html" style="color:inherit;text-decoration:underline;font-weight:600">Faça login</a>.', 10000);
                return;
            }
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const json = await res.json();
            this.orcamentos = json.dados?.content ?? json.dados ?? [];
        } catch (e) {
            console.error('[Orcamentos] Erro ao carregar:', e.message);
            this.orcamentos = [];
            this.toast('danger', 'fas fa-server', `Erro ao carregar orçamentos: ${this.esc(e.message)}`, 10000);
        } finally {
            this.render();
        }
    }

    showLoading() {
        this.$empty?.classList.add('hidden');
        if (this.$tbody) {
            this.$tbody.innerHTML = `<tr class="loading-row"><td colspan="8"><div class="table-loading"><i class="fas fa-spinner"></i><span>Carregando...</span></div></td></tr>`;
        }
    }

    // ══ API ═════════════════════════════════════════════════════════════

    async apiCreate(data) {
        const res = await fetch(API_ORCAMENTOS, { method: 'POST', headers: this.authHeaders(), body: JSON.stringify(data) });
        if (!res.ok) { const e = await res.json().catch(()=>({})); throw new Error(e.mensagem || `HTTP ${res.status}`); }
        return (await res.json()).dados;
    }

    async apiUpdate(id, data) {
        const res = await fetch(`${API_ORCAMENTOS}/${id}`, { method: 'PUT', headers: this.authHeaders(), body: JSON.stringify(data) });
        if (!res.ok) { const e = await res.json().catch(()=>({})); throw new Error(e.mensagem || `HTTP ${res.status}`); }
        return (await res.json()).dados;
    }

    async apiDelete(id) {
        const res = await fetch(`${API_ORCAMENTOS}/${id}`, { method: 'DELETE', headers: this.authHeaders() });
        if (!res.ok) { const e = await res.json().catch(()=>({})); throw new Error(e.mensagem || `HTTP ${res.status}`); }
    }

    async apiGetById(id) {
        const res = await fetch(`${API_ORCAMENTOS}/${id}`, { headers: this.authHeaders() });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return (await res.json()).dados;
    }

    async apiGetProduto(id) {
        const res = await fetch(`${API_PRODUTOS}/${id}`, { headers: this.authHeaders() });
        if (!res.ok) return null;
        return (await res.json()).dados;
    }

    // ══ BIND ════════════════════════════════════════════════════════════

    bindElements() {
        this.$tbody       = document.getElementById('orcamentosTableBody');
        this.$empty       = document.getElementById('orcamentosEmpty');
        this.$count       = document.getElementById('orcCount');
        this.$kpiTotal    = document.getElementById('orcKpiTotal');
        this.$kpiRascunho = document.getElementById('orcKpiRascunho');
        this.$kpiAprovados= document.getElementById('orcKpiAprovados');
        this.$kpiValor    = document.getElementById('orcKpiValor');
        this.$search         = document.getElementById('orcSearch');
        this.$searchClear    = document.getElementById('orcSearchClear');
        this.$filterStatus   = document.getElementById('orcFilterStatus');
        this.$filterCliente  = document.getElementById('orcFilterCliente');
        this.$filterDataDe   = document.getElementById('orcFilterDataDe');
        this.$filterDataAte  = document.getElementById('orcFilterDataAte');
        // Página do formulário
        this.$formTitle  = document.getElementById('orcFormTitle');
        this.$form       = document.getElementById('orcForm');
        // Bloco 1 — Cabeçalho
        this.$fId         = document.getElementById('orcId');
        this.$fNumero     = document.getElementById('orcNumero');
        this.$fEmissao    = document.getElementById('orcDataEmissao');
        this.$fValidade   = document.getElementById('orcDataValidade');
        this.$fStatus     = document.getElementById('orcStatus');
        // Bloco 2 — Cliente
        this.$fClienteId  = document.getElementById('orcClienteId');
        this.$fOpId       = document.getElementById('orcOportunidadeId');
        this.$clienteInfo = document.getElementById('orcClienteInfo');
        this.$fCliEndereco= document.getElementById('orcClienteEnderecoDisplay');
        this.$fCliTelefone= document.getElementById('orcClienteTelefoneDisplay');
        this.$fCliEmail   = document.getElementById('orcClienteEmailDisplay');
        // Bloco 3 — Serviço
        this.$fDescDetalhada    = document.getElementById('orcDescricaoDetalhada');
        this.$fTipoSistema      = document.getElementById('orcTipoSistema');
        this.$fQtdUnidades      = document.getElementById('orcQuantidadeUnidades');
        this.$fPrazoExecucao    = document.getElementById('orcPrazoExecucao');
        this.$fGarantia         = document.getElementById('orcGarantia');
        // Bloco 4 — Itens (serviços incluídos)
        this.$itensBody   = document.getElementById('orcItensBody');
        // Bloco 5 — Valores
        this.$fValorTotal = document.getElementById('orcValorTotal');
        this.$fDesconto   = document.getElementById('orcDesconto');
        this.$fTotal      = document.getElementById('orcTotal');
        this.$fCondicao1  = document.getElementById('orcCondicao1');
        this.$fCondicao2  = document.getElementById('orcCondicao2');
        // Bloco 6 — Assinaturas
        this.$fEmpresaAssin     = document.getElementById('orcEmpresaAssin');
        this.$fRespComercial    = document.getElementById('orcResponsavelComercial');
        this.$fTextoJuridico    = document.getElementById('orcTextoJuridico');
        // Campos legados (hidden)
        this.$fObsHidden  = document.getElementById('orcObservacoes');
        this.$fRodapeHidden = document.getElementById('orcRodape');
        // Delete modal
        this.$deleteModal = document.getElementById('orcDeleteModalBackdrop');
        this.$deleteName  = document.getElementById('orcDeleteName');
        // Preview
        this.$previewModal= document.getElementById('orcPreviewBackdrop');
        this.$printArea   = document.getElementById('orcPrintArea');
        // Email modal
        this.$emailModal        = document.getElementById('orcEmailModalBackdrop');
        this.$emailDestinatario = document.getElementById('orcEmailDestinatario');
        this.$emailMensagem     = document.getElementById('orcEmailMensagem');
        this.$emailClienteInfo  = document.getElementById('orcEmailClienteInfo');
        // Current orçamento shown in preview
        this.currentPreviewOrc  = null;
    }

    /** Safe addEventListener — never throws if element is null */
    _on(idOrEl, event, handler) {
        const el = typeof idOrEl === 'string' ? document.getElementById(idOrEl) : idOrEl;
        if (el) { el.addEventListener(event, handler); }
        else    { console.warn(`[Orcamentos] Elemento não encontrado: ${idOrEl}`); }
    }

    bindEvents() {
        // ─── Novo orçamento ───
        const openSafe = () => {
            console.log('[Orcamentos] btnNovoOrcamento CLICKED');
            this.openModal().catch(e => {
                console.error('[Orcamentos] openModal falhou:', e);
                this.toast('danger', 'fas fa-exclamation-circle', `Erro ao abrir formulário: ${this.esc(e.message)}`);
            });
        };
        this._on('btnNovoOrcamento', 'click', openSafe);
        this._on('btnOrcEmptyNovo',  'click', openSafe);

        // Botões da página de formulário
        this._on('orcFormSave', 'click', () => this.save().catch(e => {
            console.error('[Orcamentos] save() falhou:', e);
            this.toast('danger', 'fas fa-exclamation-circle', `Erro ao salvar: ${this.esc(e.message)}`);
        }));
        this._on('orcFormCancel', 'click', () => this.closeModal());
        this._on('orcFormBack',   'click', () => this.closeModal());

        // Delete modal
        this._on('orcDeleteModalConfirm', 'click', () => this.confirmDelete());
        this._on('orcDeleteModalCancel',  'click', () => this.closeDeleteModal());
        this._on('orcDeleteModalClose',   'click', () => this.closeDeleteModal());
        this._on(this.$deleteModal, 'click', e => { if (e.target === this.$deleteModal) this.closeDeleteModal(); });

        // Preview modal
        this._on('orcPreviewClose', 'click', () => this.closePreview());
        this._on(this.$previewModal, 'click', e => { if (e.target === this.$previewModal) this.closePreview(); });
        this._on('orcPrintBtn',     'click', () => window.print());
        this._on('orcDocxBtn',      'click', () => this.downloadDocx());
        this._on('orcWhatsAppBtn',  'click', () => this.sendWhatsApp());
        this._on('orcEmailBtn',     'click', () => this.openEmailModal());

        // Email modal
        this._on('orcEmailModalClose',   'click', () => this.closeEmailModal());
        this._on('orcEmailModalCancel',  'click', () => this.closeEmailModal());
        this._on(this.$emailModal, 'click', e => { if (e.target === this.$emailModal) this.closeEmailModal(); });
        this._on('orcEmailModalSend', 'click', () => this.doSendEmail());

        // Esc
        document.addEventListener('keydown', e => {
            if (e.key === 'Escape') {
                if (window.navigationManager?.currentModule === 'orcamento-form') this.closeModal();
                if (this.$deleteModal && !this.$deleteModal.classList.contains('hidden')) this.closeDeleteModal();
                if (this.$previewModal && !this.$previewModal.classList.contains('hidden')) this.closePreview();
                if (this.$emailModal && !this.$emailModal.classList.contains('hidden')) this.closeEmailModal();
            }
        });

        // Search
        this._on(this.$search, 'input', () => {
            this.searchTerm = this.$search.value.trim().toLowerCase();
            this.$searchClear.classList.toggle('hidden', !this.searchTerm);
            this.render();
        });
        this._on(this.$searchClear, 'click', () => {
            this.$search.value = ''; this.searchTerm = '';
            this.$searchClear.classList.add('hidden');
            this.$search.focus(); this.render();
        });

        // Filters
        this._on(this.$filterStatus,  'change', () => { this.filterStatus = this.$filterStatus.value; this.render(); });
        this._on(this.$filterCliente, 'input',  () => { this.filterCliente = this.$filterCliente.value.trim().toLowerCase(); this.render(); });
        this._on(this.$filterDataDe,  'change', () => { this.filterDataDe  = this.$filterDataDe.value;  this.render(); });
        this._on(this.$filterDataAte, 'change', () => { this.filterDataAte = this.$filterDataAte.value; this.render(); });
        this._on('orcFilterLimpar', 'click', () => this.clearFilters());

        // Add service item
        this._on('orcAddItem', 'click', () => this.addItemRow());

        // Recalculate totals when values change
        this._on(this.$fValorTotal, 'input', () => this.recalcTotals());
        this._on(this.$fDesconto,   'input', () => this.recalcTotals());

        // Auto-fill client info on selection
        this._on(this.$fClienteId, 'change', () => this.onClienteSelected());
    }

    // ══ CLIENTES (é carregado no init) ══════════════════════════════════════════════

    async loadClientes() {
        try {
            const res = await fetch(`${API_BASE_URL}/clientes?size=500&sort=nomeEmpresa,asc`, { headers: this.authHeaders() });
            if (!res.ok) return;
            const json = await res.json();
            const clientes = json.dados?.content ?? json.dados ?? [];
            this.clientesCache = clientes;
            clientes.forEach(c => {
                const opt = document.createElement('option');
                opt.value = c.id;
                opt.textContent = c.nome || `Cliente #${c.id}`;
                this.$fClienteId.appendChild(opt);
            });
        } catch (e) {
            console.warn('[Orcamentos] Não foi possível carregar lista de clientes:', e.message);
        }
    }

    onClienteSelected(orcData = null) {
        const id = parseInt(this.$fClienteId.value);
        if (!id) { this.$clienteInfo.classList.add('hidden'); return; }
        let c;
        if (orcData) {
            c = { endereco: orcData.clienteEndereco, cidade: orcData.clienteCidade, estado: orcData.clienteEstado,
                  telefone: orcData.clienteTelefone, email: orcData.clienteEmail };
        } else {
            c = this.clientesCache.find(x => x.id === id);
        }
        if (!c) { this.$clienteInfo.classList.add('hidden'); return; }
        const endParts = [c.endereco, c.cidade, c.estado].filter(Boolean);
        this.$fCliEndereco.value  = endParts.join(', ') || '';
        this.$fCliTelefone.value  = c.telefone || '';
        this.$fCliEmail.value     = c.email    || '';
        const hasSomething = endParts.length || c.telefone || c.email;
        this.$clienteInfo.classList.toggle('hidden', !hasSomething);
    }

    parseFieldData(raw) {
        if (!raw || typeof raw !== 'string') return {};
        try { const p = JSON.parse(raw); return (p && typeof p === 'object') ? p : {}; } catch { return {}; }
    }

    // ══ FILTER ══════════════════════════════════════════════════════════

    clearFilters() {
        this.searchTerm = ''; this.$search.value = ''; this.$searchClear.classList.add('hidden');
        this.filterStatus  = ''; this.$filterStatus.value  = '';
        this.filterCliente = ''; this.$filterCliente.value = '';
        this.filterDataDe  = ''; this.$filterDataDe.value  = '';
        this.filterDataAte = ''; this.$filterDataAte.value = '';
        this.render();
    }

    getFiltered() {
        return this.orcamentos.filter(o => {
            const s = this.searchTerm;
            const matchSearch  = !s || (o.numero || '').toLowerCase().includes(s) || (o.clienteNome || '').toLowerCase().includes(s);
            const matchStatus  = !this.filterStatus  || o.status === this.filterStatus;
            const matchCliente = !this.filterCliente || (o.clienteNome || '').toLowerCase().includes(this.filterCliente);
            const matchDataDe  = !this.filterDataDe  || (o.dataEmissao && o.dataEmissao >= this.filterDataDe);
            const matchDataAte = !this.filterDataAte || (o.dataEmissao && o.dataEmissao <= this.filterDataAte);
            return matchSearch && matchStatus && matchCliente && matchDataDe && matchDataAte;
        });
    }

    // ══ RENDER ══════════════════════════════════════════════════════════

    render() {
        const all       = this.orcamentos;
        const aprovados = all.filter(o => o.status === 'APROVADO');
        const valorAprov= aprovados.reduce((s, o) => s + (o.total || 0), 0);

        this.$kpiTotal.textContent    = all.length;
        this.$kpiRascunho.textContent = all.filter(o => o.status === 'RASCUNHO').length;
        this.$kpiAprovados.textContent= aprovados.length;
        this.$kpiValor.textContent    = this.formatCompact(valorAprov);

        const filtered = this.getFiltered();
        this.$count.textContent = `${filtered.length} orçamento${filtered.length !== 1 ? 's' : ''}`;

        if (filtered.length === 0) {
            if (this.$tbody) this.$tbody.innerHTML = '';
            this.$empty.classList.remove('hidden');
        } else {
            this.$empty.classList.add('hidden');
            if (this.$tbody) {
                this.$tbody.innerHTML = filtered.map(o => this.renderRow(o)).join('');
                this.bindRowEvents();
            }
        }
    }

    renderRow(o) {
        const cliente = o.clienteNome ? this.esc(o.clienteNome) : `#${o.clienteId || '?'}`;
        const opTitulo = o.oportunidadeTitulo ? this.esc(o.oportunidadeTitulo) : '<span class="text-muted">—</span>';
        const total = o.total != null ? this.formatCurrency(o.total) : '—';

        return `
        <tr data-id="${o.id}">
            <td><strong>${this.esc(o.numero || '—')}</strong></td>
            <td>${cliente}</td>
            <td>${opTitulo}</td>
            <td>${this.formatDate(o.dataEmissao)}</td>
            <td>${this.formatDate(o.dataValidade)}</td>
            <td><strong>${total}</strong></td>
            <td>${this.statusBadge(o.status)}</td>
            <td style="text-align:center">
                <div class="actions">
                    <button class="btn btn-sm btn-secondary btn-orc-view" data-id="${o.id}" title="Visualizar"><i class="fas fa-eye"></i></button>
                    <button class="btn btn-sm btn-secondary btn-orc-edit" data-id="${o.id}" title="Editar"><i class="fas fa-pen"></i></button>
                    <button class="btn btn-sm btn-danger btn-orc-del" data-id="${o.id}" title="Excluir"><i class="fas fa-trash-alt"></i></button>
                </div>
            </td>
        </tr>`;
    }

    bindRowEvents() {
        this.$tbody.querySelectorAll('.btn-orc-edit').forEach(btn =>
            btn.addEventListener('click', () => this.openModal(parseInt(btn.dataset.id))));
        this.$tbody.querySelectorAll('.btn-orc-del').forEach(btn =>
            btn.addEventListener('click', () => this.openDeleteModal(parseInt(btn.dataset.id))));
        this.$tbody.querySelectorAll('.btn-orc-view').forEach(btn =>
            btn.addEventListener('click', () => this.openPreview(parseInt(btn.dataset.id))));
    }

    // ══ MODAL ═══════════════════════════════════════════════════════════

    async openModal(id = null) {
        console.log('[Orcamentos] openModal called, id=', id);
        this.editingId = id;
        this.clearFormErrors();
        if (this.$itensBody) this.$itensBody.innerHTML = '';
        this.itemSeq = 0;
        if (this.$clienteInfo) this.$clienteInfo.classList.add('hidden');

        const pageLabel = id !== null ? 'Editar Orçamento' : 'Novo Orçamento';
        if (this.$formTitle) this.$formTitle.textContent = pageLabel;
        // Atualiza o sub-item do breadcrumb se já estiver visível
        const sub = document.getElementById('orcFormBreadcrumbSub');
        if (sub) sub.textContent = pageLabel;

        if (id !== null) {
            if (window.navigationManager) window.navigationManager.navigateTo('orcamento-form');
            try {
                const o = await this.apiGetById(id);
                if (this.$formTitle) this.$formTitle.textContent = `Editar Orçamento ${o.numero}`;
                const sub2 = document.getElementById('orcFormBreadcrumbSub');
                if (sub2) sub2.textContent = `Editar Orçamento ${o.numero}`;
                this.$fId.value        = o.id || '';
                this.$fNumero.value    = o.numero || '';
                this.$fEmissao.value   = o.dataEmissao || '';
                this.$fValidade.value  = o.dataValidade || '';
                this.$fStatus.value    = o.status || 'RASCUNHO';
                this.$fClienteId.value = o.clienteId ?? '';
                this.$fOpId.value      = o.oportunidadeId ?? '';
                this.$fDesconto.value  = o.desconto ?? 0;
                this.onClienteSelected(o);

                const obs = this.parseFieldData(o.observacoesComerciais);
                if (obs.v === 2) {
                    this.$fDescDetalhada.value = obs.descricaoDetalhada || '';
                    this.$fTipoSistema.value   = obs.tipoSistema        || '';
                    this.$fQtdUnidades.value   = obs.quantidadeUnidades  != null ? obs.quantidadeUnidades : '';
                    this.$fPrazoExecucao.value  = obs.prazoExecucao      || '';
                    this.$fGarantia.value       = obs.garantia            || '';
                    this.$fCondicao1.value       = obs.condicao1          || '';
                    this.$fCondicao2.value       = obs.condicao2          || '';
                } else {
                    this.$fDescDetalhada.value = o.observacoesComerciais || '';
                }

                const rod = this.parseFieldData(o.rodapeInstitucional);
                if (rod.v === 2) {
                    this.$fEmpresaAssin.value   = rod.empresa              || 'Versatilis';
                    this.$fRespComercial.value  = rod.responsavelComercial || '';
                    this.$fTextoJuridico.value  = rod.textoJuridico        || '';
                } else {
                    this.$fEmpresaAssin.value  = 'Versatilis';
                    this.$fTextoJuridico.value = o.rodapeInstitucional || '';
                }

                const serviceItems = (o.itens || []).filter(i => i.descricao !== 'VALOR_TOTAL');
                const totalItem    = (o.itens || []).find(i => i.descricao === 'VALOR_TOTAL');
                serviceItems.forEach(item => this.addItemRow(item.descricao));
                if (serviceItems.length === 0) this.addItemRow();
                this.$fValorTotal.value = totalItem ? (totalItem.valorUnitario ?? o.total ?? 0) : (o.total ?? 0);
                this.recalcTotals();

            } catch (e) {
                this.toast('danger', 'fas fa-exclamation-circle', `Erro ao carregar orçamento: ${this.esc(e.message)}`);
                this.closeModal(); return;
            }
        } else {
            console.log('[Orcamentos] openModal: NEW mode (id=null)');
            this.$form.reset();
            this.$fId.value       = '';
            this.$fNumero.value   = '';
            this.$fEmissao.value  = new Date().toISOString().split('T')[0];
            const v = new Date(); v.setDate(v.getDate() + 30);
            this.$fValidade.value = v.toISOString().split('T')[0];
            this.$fStatus.value   = 'RASCUNHO';
            this.$fTotal.value    = 'R$ 0,00';
            this.$fEmpresaAssin.value = 'Versatilis';
            this.$fTextoJuridico.value = 'Com a assinatura desse orçamento a Versatilis está autorizada a iniciar esse serviço, conforme o termo deste.\nO aceite deste orçamento por via digital no Whatsapp tem o mesmo efeito jurídico do presente documento assinado.';
            this.addItemRow();
            console.log('[Orcamentos] About to navigateTo orcamento-form, navigationManager=', !!window.navigationManager);
            if (window.navigationManager) window.navigationManager.navigateTo('orcamento-form');
            console.log('[Orcamentos] navigateTo completed');
        }

        setTimeout(() => this.$fClienteId?.focus(), 80);
    }

    closeModal() {
        this.editingId = null;
        if (window.navigationManager) window.navigationManager.navigateTo('orcamentos');
    }

    openModalFromOportunidade(oportunidade) {
        try {
            this.editingId = null;
            this.clearFormErrors();
            if (this.$itensBody) this.$itensBody.innerHTML = '';
            this.itemSeq = 0;
            if (this.$clienteInfo) this.$clienteInfo.classList.add('hidden');
            const pageLabel = 'Novo Orçamento';
            if (this.$formTitle) this.$formTitle.textContent = pageLabel;
            if (this.$form) this.$form.reset();
            this.$fId.value        = '';
            this.$fNumero.value    = '';
            this.$fEmissao.value   = new Date().toISOString().split('T')[0];
            const v = new Date(); v.setDate(v.getDate() + 30);
            this.$fValidade.value  = v.toISOString().split('T')[0];
            this.$fStatus.value    = 'RASCUNHO';
            this.$fClienteId.value = oportunidade.clienteId ?? '';
            this.$fOpId.value      = oportunidade.id        ?? '';
            this.$fTotal.value     = 'R$ 0,00';
            this.$fEmpresaAssin.value = 'Versatilis';
            this.$fTextoJuridico.value = 'Com a assinatura desse orçamento a Versatilis está autorizada a iniciar esse serviço, conforme o termo deste.\nO aceite deste orçamento por via digital no Whatsapp tem o mesmo efeito jurídico do presente documento assinado.';
            // Pre-fill client info from cache
            this.onClienteSelected();
            this.addItemRow();

            if (window.navigationManager) window.navigationManager.navigateTo('orcamento-form');
            setTimeout(() => this.$fClienteId?.focus(), 80);
        } catch (err) {
            console.error('[Orcamentos] Erro ao abrir modal:', err);
            this.toast('danger', 'fas fa-exclamation-circle', 'Erro ao abrir tela de orçamento.');
        }
    }

    // -- Serviços incluídos (lista dinâmica de texto) ---

    addItemRow(descricaoInicial = null) {
        if (descricaoInicial === 'VALOR_TOTAL') return;
        this.itemSeq++;
        const div = document.createElement('div');
        div.className = 'orc-servico-row';
        const valEsc = typeof descricaoInicial === 'string'
            ? descricaoInicial.replace(/"/g, '&quot;')
            : '';
        div.innerHTML = `
            <input type="text" class="form-control item-servico-desc"
                   placeholder="Descreva o serviço incluído (ex.: Limpeza e lubrificação do sistema)..."
                   value="${valEsc}">
            <button type="button" class="btn-remove-item" title="Remover"><i class="fas fa-times"></i></button>`;
        this.$itensBody.appendChild(div);
        div.querySelector('.btn-remove-item').addEventListener('click', () => div.remove());
    }

    recalcTotals() {
        const total    = parseFloat(this.$fValorTotal?.value) || 0;
        const desconto = parseFloat(this.$fDesconto?.value)  || 0;
        if (this.$fTotal) this.$fTotal.value = this.formatCurrency(Math.max(0, total - desconto));
    }

    collectItens() {
        const itens = [];
        this.$itensBody.querySelectorAll('.item-servico-desc').forEach(input => {
            const desc = input.value.trim();
            if (desc) itens.push({ descricao: desc, quantidade: 1, valorUnitario: 0, valorTotal: 0 });
        });
        return itens;
    }

    // ── Save ────────────────────────────────────────────────────

    async save() {
        if (!this.validateForm()) return;

        const totalValue = parseFloat(this.$fValorTotal.value) || 0;
        const desconto   = parseFloat(this.$fDesconto.value)   || 0;
        const opVal      = this.$fOpId.value.trim();

        const itens = this.collectItens();
        itens.push({ descricao: 'VALOR_TOTAL', quantidade: 1, valorUnitario: totalValue, valorTotal: totalValue });

        const obsData = {
            v: 2,
            descricaoDetalhada:  this.$fDescDetalhada.value.trim()  || null,
            tipoSistema:         this.$fTipoSistema.value.trim()     || null,
            quantidadeUnidades:  parseInt(this.$fQtdUnidades.value)  || null,
            prazoExecucao:       this.$fPrazoExecucao.value.trim()   || null,
            garantia:            this.$fGarantia.value.trim()        || null,
            condicao1:           this.$fCondicao1.value.trim()       || null,
            condicao2:           this.$fCondicao2.value.trim()       || null,
        };

        const rodapeData = {
            v: 2,
            empresa:              this.$fEmpresaAssin.value.trim()  || 'Versatilis',
            responsavelComercial: this.$fRespComercial.value.trim() || null,
            textoJuridico:        this.$fTextoJuridico.value.trim() || null,
        };

        const data = {
            dataEmissao:           this.$fEmissao.value,
            dataValidade:          this.$fValidade.value,
            status:                this.$fStatus.value || 'RASCUNHO',
            clienteId:             parseInt(this.$fClienteId.value),
            oportunidadeId:        opVal ? parseInt(opVal) : null,
            desconto,
            observacoesComerciais: JSON.stringify(obsData),
            rodapeInstitucional:   JSON.stringify(rodapeData),
            itens,
        };

        const btn = document.getElementById('orcFormSave');
        btn.disabled = true;
        try {
            if (this.editingId !== null) {
                const updated = await this.apiUpdate(this.editingId, data);
                const idx = this.orcamentos.findIndex(o => o.id === this.editingId);
                if (idx > -1) this.orcamentos[idx] = updated;
                this.toast('success', 'fas fa-check-circle', `Orçamento <strong>${this.esc(updated.numero)}</strong> atualizado.`);
            } else {
                const created = await this.apiCreate(data);
                this.orcamentos.unshift(created);
                this.toast('success', 'fas fa-check-circle', `Orçamento <strong>${this.esc(created.numero)}</strong> criado.`);
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
        const cli = this.$fClienteId.value.trim();
        if (!cli || isNaN(parseInt(cli))) { this.showFieldError(this.$fClienteId, 'orcErrCliente'); ok = false; }
        if (!this.$fEmissao.value)  { this.showFieldError(this.$fEmissao, 'orcErrEmissao');   ok = false; }
        if (!this.$fValidade.value) { this.showFieldError(this.$fValidade, 'orcErrValidade');  ok = false; }
        const tot = parseFloat(this.$fValorTotal.value) || 0;
        if (tot <= 0) { this.showFieldError(this.$fValorTotal, 'orcErrValor'); ok = false; }
        return ok;
    }

    showFieldError(input, errId) {
        if (input) input.classList.add('invalid');
        const el = document.getElementById(errId);
        if (el) el.classList.remove('hidden');
    }

    clearFormErrors() {
        if (!this.$form) return;
        this.$form.querySelectorAll('.invalid').forEach(el => el.classList.remove('invalid'));
        this.$form.querySelectorAll('.form-error').forEach(el => el.classList.add('hidden'));
    }

    // ══ MODAL EXCLUSÃO ═════════════════════════════════════════════════

    openDeleteModal(id) {
        const o = this.orcamentos.find(x => x.id === id);
        if (!o) return;
        this.deletingId = id;
        this.$deleteName.textContent = o.numero || `#${id}`;
        this.$deleteModal.classList.remove('hidden');
    }

    closeDeleteModal() { this.$deleteModal.classList.add('hidden'); this.deletingId = null; }

    async confirmDelete() {
        const o = this.orcamentos.find(x => x.id === this.deletingId);
        if (!o) return;
        const btn = document.getElementById('orcDeleteModalConfirm');
        btn.disabled = true;
        try {
            await this.apiDelete(this.deletingId);
            this.closeDeleteModal();
            this.toast('success', 'fas fa-check-circle', `Orçamento <strong>${this.esc(o.numero || '')}</strong> excluído.`);
            await this.loadData();
        } catch (e) {
            this.toast('danger', 'fas fa-exclamation-circle', `Erro ao excluir: ${this.esc(e.message)}`);
            this.closeDeleteModal();
        } finally {
            btn.disabled = false;
        }
    }

    // ══ PREVIEW / IMPRESSÃO ═════════════════════════════════════════════

    async openPreview(id) {
        try {
            const o = await this.apiGetById(id);
            this.currentPreviewOrc = o;
            this.renderDocument(o);
            this.$previewModal.classList.remove('hidden');
        } catch (e) {
            this.toast('danger', 'fas fa-exclamation-circle', `Erro ao carregar orçamento: ${this.esc(e.message)}`);
        }
    }

    closePreview() { this.$previewModal.classList.add('hidden'); }

    // ── Download DOCX (template preenchido) ──────────────────────────

    async downloadDocx() {
        const o = this.currentPreviewOrc;
        if (!o) return;
        const btn = document.getElementById('orcDocxBtn');
        const origHtml = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
        try {
            const token = this.getToken();
            const res = await fetch(`${API_ORCAMENTOS}/${o.id}/docx`, {
                headers: token ? { 'Authorization': `Bearer ${token}` } : {},
            });
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.mensagem || `HTTP ${res.status}`);
            }
            const blob = await res.blob();
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `orcamento-${o.numero || o.id}.docx`;
            document.body.appendChild(a);
            a.click();
            a.remove();
            URL.revokeObjectURL(url);
            this.toast('success', 'fas fa-file-word', `Documento <strong>${this.esc(o.numero || '')}</strong> baixado.`);
        } catch (e) {
            const isTemplateMissing = e.message && e.message.includes('não encontrado');
            this.toast('danger', 'fas fa-exclamation-circle',
                isTemplateMissing
                    ? 'Template não configurado. Coloque <strong>orcamento.docx</strong> em <code>Backend/src/main/resources/templates/</code>.'
                    : `Erro ao gerar DOCX: ${this.esc(e.message)}`
            );
        } finally {
            btn.disabled = false;
            btn.innerHTML = origHtml;
        }
    }

    // ── WhatsApp ─────────────────────────────────────────────────

    sendWhatsApp() {
        const o = this.currentPreviewOrc;
        if (!o) return;

        const tel      = (o.clienteTelefone || '').replace(/\D/g, '');
        const total    = this.formatCurrency(o.total || 0);
        const emissao  = this.formatDate(o.dataEmissao);
        const validade = this.formatDate(o.dataValidade);

        // Serviços incluídos (excluindo marcador interno)
        const serviceItems = (o.itens || []).filter(i => i.descricao !== 'VALOR_TOTAL');
        const itensTexto = serviceItems.slice(0, 6)
            .map((item, i) => `  • ${item.descricao || '—'}`)
            .join('\n');
        const maisItens = serviceItems.length > 6
            ? `\n  ... e mais ${serviceItems.length - 6} serviço(s)`
            : '';

        // Desconto (só se houver)
        const descontoLinha = (o.desconto && o.desconto > 0)
            ? `\n💸 *Desconto:* − ${this.formatCurrency(o.desconto)}`
            : '';

        // Oportunidade (contexto)
        const oportunidadeLinha = o.oportunidadeTitulo
            ? `\n🎯 *Ref.:* ${o.oportunidadeTitulo}`
            : '';

        const servicosBloco = itensTexto
            ? `\n\n*Serviços incluídos:*\n${itensTexto}${maisItens}`
            : '';

        const msg = [
            `Olá ${o.clienteNome || ''},`,
            ``,
            `A Versatilis preparou uma proposta comercial para você:`,
            ``,
            `📄 *Orçamento:* ${o.numero || '—'}${oportunidadeLinha}`,
            `📅 *Emissão:* ${emissao}  |  *Válido até:* ${validade}`,
            servicosBloco,
            ``,
            `💰 *Valor total: ${total}*${descontoLinha}`,
            ``,
            `O orçamento completo em PDF será enviado por email.`,
            `Qualquer dúvida, estamos à disposição! 😊`,
        ].join('\n');

        const url = tel
            ? `https://wa.me/55${tel}?text=${encodeURIComponent(msg)}`
            : `https://web.whatsapp.com/send?text=${encodeURIComponent(msg)}`;
        window.open(url, '_blank', 'noopener,noreferrer');
    }

    // ── Email modal ──────────────────────────────────────────────

    openEmailModal() {
        const o = this.currentPreviewOrc;
        if (!o) return;
        this.$emailDestinatario.value = o.clienteEmail || '';
        this.$emailMensagem.value = '';
        this.$emailClienteInfo.textContent = o.clienteEmail
            ? `Email do cliente: ${o.clienteEmail}`
            : 'O cliente não possui email cadastrado — informe o destinatário acima.';
        this.$emailModal.classList.remove('hidden');
        setTimeout(() => this.$emailDestinatario.focus(), 50);
    }

    closeEmailModal() { this.$emailModal.classList.add('hidden'); }

    async doSendEmail() {
        const o = this.currentPreviewOrc;
        if (!o) return;
        const destinatario = this.$emailDestinatario.value.trim();
        if (!destinatario || !destinatario.includes('@')) {
            this.$emailDestinatario.classList.add('invalid');
            this.$emailDestinatario.focus();
            return;
        }
        this.$emailDestinatario.classList.remove('invalid');
        const btn = document.getElementById('orcEmailModalSend');
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Enviando...';
        try {
            await this.apiEnviarEmail(o.id, destinatario, this.$emailMensagem.value.trim());
            this.closeEmailModal();
            this.closePreview();
            this.toast('success', 'fas fa-envelope', `Email enviado com sucesso para <strong>${this.esc(destinatario)}</strong>.`);
            // Atualizar lista local para refletir novo status
            await this.loadData();
        } catch (e) {
            this.toast('danger', 'fas fa-exclamation-circle', `Erro ao enviar: ${this.esc(e.message)}`);
        } finally {
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-paper-plane"></i> Enviar';
        }
    }

    async apiEnviarEmail(id, destinatario, mensagemAdicional) {
        const res = await fetch(`${API_ORCAMENTOS}/${id}/enviar-email`, {
            method: 'POST',
            headers: this.authHeaders(),
            body: JSON.stringify({ destinatario, mensagemAdicional: mensagemAdicional || null }),
        });
        if (!res.ok) { const e = await res.json().catch(() => ({})); throw new Error(e.mensagem || `HTTP ${res.status}`); }
        return (await res.json());
    }

    renderDocument(o) {
        const emp = OrcamentosModule.EMPRESA;

        // --- Parse structured fields ---
        const obs    = this.parseFieldData(o.observacoesComerciais);
        const rodape = this.parseFieldData(o.rodapeInstitucional);

        const descricaoDetalhada = obs.v === 2 ? (obs.descricaoDetalhada || '') : (o.observacoesComerciais || '');
        const tipoSistema        = obs.v === 2 ? obs.tipoSistema        : null;
        const qtdUnidades        = obs.v === 2 ? obs.quantidadeUnidades  : null;
        const prazoExecucao      = obs.v === 2 ? obs.prazoExecucao      : null;
        const garantia           = obs.v === 2 ? obs.garantia           : null;
        const condicao1          = obs.v === 2 ? obs.condicao1          : null;
        const condicao2          = obs.v === 2 ? obs.condicao2          : null;

        const empresa             = rodape.v === 2 ? (rodape.empresa              || emp.nome) : emp.nome;
        const responsavelComercial= rodape.v === 2 ? (rodape.responsavelComercial || '')       : '';
        const textoJuridico       = rodape.v === 2 ? (rodape.textoJuridico        || '')       : (o.rodapeInstitucional || '');

        // --- Service items (exclude VALOR_TOTAL marker) ---
        const serviceItems = (o.itens || []).filter(i => i.descricao !== 'VALOR_TOTAL');
        const totalValue   = o.total || 0;
        const desconto     = o.desconto || 0;

        const clienteEndereco = [o.clienteEndereco, o.clienteCidade, o.clienteEstado].filter(Boolean).join(', ');

        const servicosHtml = serviceItems.length > 0
            ? serviceItems.map(i => `<li>${this.esc(i.descricao)}</li>`).join('')
            : '<li style="color:#999">Nenhum serviço listado</li>';

        this.$printArea.innerHTML = `
            <!-- CABEÇALHO — imagem -->
            <div class="orc-doc-header-img">
                <img src="assets/cabecalho.png" alt="Cabeçalho Versatilis">
            </div>

            <!-- NÚMERO / DATAS -->
            <div class="orc-doc-meta-bar">
                <div class="orc-doc-meta-left">
                    <h2>ORÇAMENTO</h2>
                    <div class="orc-numero">${this.esc(o.numero || '—')}</div>
                    ${o.oportunidadeTitulo ? `<div style="margin-top:4px;font-size:11px;color:#666">Ref.: ${this.esc(o.oportunidadeTitulo)}</div>` : ''}
                </div>
                <div class="orc-doc-meta-right">
                    <span>Emissão: <strong>${this.formatDate(o.dataEmissao)}</strong></span><br>
                    <span>Válido até: <strong>${this.formatDate(o.dataValidade)}</strong></span>
                </div>
            </div>

            <!-- DADOS DO CLIENTE -->
            <div class="orc-doc-client">
                <h3>Dados do Cliente</h3>
                <p><strong>${this.esc(o.clienteNome || '—')}</strong></p>
                ${o.clienteCnpj ? `<p>CNPJ: ${this.esc(o.clienteCnpj)}</p>` : ''}
                ${clienteEndereco ? `<p>${this.esc(clienteEndereco)}</p>` : ''}
                ${o.clienteTelefone ? `<p>Tel.: ${this.esc(o.clienteTelefone)}</p>` : ''}
                ${o.clienteEmail   ? `<p>Email: ${this.esc(o.clienteEmail)}</p>`   : ''}
            </div>

            ${descricaoDetalhada ? `
            <!-- DESCRIÇÃO DETALHADA -->
            <div class="orc-doc-section">
                <h3>1. Descrição Detalhada dos Serviços</h3>
                <p>${this.esc(descricaoDetalhada).replace(/\n/g, '<br>')}</p>
            </div>` : ''}

            ${tipoSistema || qtdUnidades || prazoExecucao || garantia ? `
            <!-- ESPECIFICAÇÕES TÉCNICAS -->
            <div class="orc-doc-section">
                <h3>2. Especificações Técnicas</h3>
                <table class="orc-doc-specs">
                    ${tipoSistema  ? `<tr><td><strong>Tipo de Sistema:</strong></td><td>${this.esc(tipoSistema)}</td></tr>` : ''}
                    ${qtdUnidades  ? `<tr><td><strong>Quantidade de Unidades/Bandeiras:</strong></td><td>${qtdUnidades} unidades</td></tr>` : ''}
                    ${prazoExecucao? `<tr><td><strong>Prazo de Execução:</strong></td><td>${this.esc(prazoExecucao)}</td></tr>` : ''}
                    ${garantia     ? `<tr><td><strong>Garantia:</strong></td><td>${this.esc(garantia)}</td></tr>` : ''}
                </table>
            </div>` : ''}

            ${serviceItems.length > 0 ? `
            <!-- SERVIÇOS INCLUÍDOS -->
            <div class="orc-doc-section">
                <h3>3. Serviços Incluídos</h3>
                <ul class="orc-doc-services">${servicosHtml}</ul>
            </div>` : ''}

            <!-- VALORES E PAGAMENTO -->
            <div class="orc-doc-section">
                <h3>${serviceItems.length > 0 ? '4.' : '2.'} Valores e Condições de Pagamento</h3>
                <div class="orc-doc-totals">
                    <table>
                        <tr><td class="label">Valor Total do Orçamento:</td>
                            <td class="value"><strong>${this.formatCurrency(totalValue)}</strong></td></tr>
                        ${desconto > 0 ? `<tr><td class="label">Desconto:</td>
                            <td class="value" style="color:var(--color-danger)">− ${this.formatCurrency(desconto)}</td></tr>
                            <tr class="total-row"><td class="label">Total com Desconto:</td>
                            <td class="value">${this.formatCurrency(Math.max(0, totalValue - desconto))}</td></tr>` : ''}
                    </table>
                </div>
                ${condicao1 || condicao2 ? `
                <div style="margin-top:12px">
                    <p><strong>Condições de Pagamento:</strong></p>
                    ${condicao1 ? `<p>• 1ª opção: ${this.esc(condicao1)}</p>` : ''}
                    ${condicao2 ? `<p>• 2ª opção: ${this.esc(condicao2)}</p>` : ''}
                </div>` : ''}
            </div>

            <!-- ASSINATURAS -->
            <div class="orc-doc-signature-block">
                <div class="orc-sig-col">
                    <div class="sig-line-top"></div>
                    <p><strong>${this.esc(empresa)}</strong></p>
                    ${responsavelComercial ? `<p>${this.esc(responsavelComercial)}</p>` : ''}
                </div>
                <div class="orc-sig-col">
                    <div class="sig-line-top"></div>
                    <p><strong>Cliente:</strong></p>
                    <p>${this.esc(o.clienteNome || '—')}</p>
                </div>
            </div>

            ${textoJuridico ? `
            <!-- TEXTO JURÍDICO -->
            <div class="orc-doc-footer">
                <p>${this.esc(textoJuridico).replace(/\n/g, '<br>')}</p>
            </div>` : ''}

            <!-- RODAPÉ — imagem -->
            <div class="orc-doc-footer-img">
                <img src="assets/rodape.png" alt="Rodapé Versatilis">
            </div>`;
    }

    // ══ TOAST ═══════════════════════════════════════════════════════════

    toast(type, icon, message, duration = 4000) {
        const container = document.getElementById('toastContainer');
        const el = document.createElement('div');
        el.className = `toast toast-${type}`;
        el.innerHTML = `
            <i class="${icon}"></i>
            <span class="toast-msg">${message}</span>
            <button class="toast-close" aria-label="Fechar"><i class="fas fa-times"></i></button>`;
        const close = () => { el.classList.add('removing'); el.addEventListener('animationend', () => el.remove(), { once: true }); };
        el.querySelector('.toast-close').addEventListener('click', close);
        container.appendChild(el);
        setTimeout(close, duration);
    }

    // ══ HELPERS ═════════════════════════════════════════════════════════

    statusBadge(status) {
        const map = {
            'RASCUNHO': { cls: 'badge-neutral',  icon: 'fa-pen-to-square' },
            'ENVIADO':  { cls: 'badge-info',     icon: 'fa-paper-plane'   },
            'APROVADO': { cls: 'badge-success',  icon: 'fa-circle-check'  },
            'RECUSADO': { cls: 'badge-danger',   icon: 'fa-circle-xmark'  },
        };
        const s = map[status] || { cls: 'badge-neutral', icon: 'fa-circle' };
        const label = OrcamentosModule.STATUS_LABELS[status] || status;
        return `<span class="badge ${s.cls}"><i class="fas ${s.icon}"></i> ${label}</span>`;
    }

    formatCurrency(value) {
        if (value == null) return '—';
        return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
    }

    formatCompact(value) {
        if (value == null || value === 0) return 'R$ 0';
        if (value >= 1_000_000) return `R$ ${(value / 1_000_000).toFixed(1)}M`;
        if (value >= 1_000) return `R$ ${(value / 1_000).toFixed(1)}K`;
        return this.formatCurrency(value);
    }

    formatDate(d) {
        if (!d) return '—';
        const [y, m, day] = d.split('-');
        return `${day}/${m}/${y}`;
    }

    esc(str) {
        return String(str)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;')
            .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }
}

document.addEventListener('DOMContentLoaded', () => {
    console.log('[Orcamentos] DOMContentLoaded - creating OrcamentosModule');
    try {
        window.orcamentosModule = new OrcamentosModule();
        console.log('[Orcamentos] Module created successfully');
    } catch(e) {
        console.error('[Orcamentos] CONSTRUCTOR FAILED:', e);
    }
});
