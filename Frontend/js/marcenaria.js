//
// MARCENARIA.JS — Calculadora de Custos (Marcenaria) dentro do módulo Orçamentos
//
// Recursos:
//   • CRUD de materiais e acessórios (REST /api/marcenaria/*)
//   • Configuração de mão de obra (singleton)
//   • Cálculo de custo de produto em dois modos:
//      - APROVEITAMENTO: empacotamento 2D (Guillotine BSSF + rotação) com SVG
//      - METRO_QUADRADO: ceil(área / áreaChapa) × precoChapa
//

const API_MARCENARIA            = `${API_BASE_URL}/marcenaria`;
const API_MARC_MATERIAIS        = `${API_MARCENARIA}/materiais`;
const API_MARC_ACESSORIOS       = `${API_MARCENARIA}/acessorios`;
const API_MARC_CONFIG_MAO_OBRA  = `${API_MARCENARIA}/config-mao-obra`;
const API_MARC_CALCULOS         = `${API_MARCENARIA}/calculos`;

class MarcenariaModule {

    static CATEGORIA_MATERIAL_LABELS = {
        MDF:        'MDF',
        MDP:        'MDP',
        COMPENSADO: 'Compensado',
        OUTRO:      'Outro',
    };

    static CATEGORIA_ACESSORIO_LABELS = {
        DOBRADICA:  'Dobradiça',
        CORREDICA:  'Corrediça',
        PUXADOR:    'Puxador',
        PARAFUSO:   'Parafuso',
        FITA_BORDA: 'Fita de borda',
        OUTRO:      'Outro',
    };

    constructor() {
        this.materiais       = [];
        this.acessorios      = [];
        this.configMaoObra   = { custoDiario: 300, custoDiarioAjudante: null, margemLucroPadraoPct: 30 };
        this.editingMaterialId  = null;
        this.editingAcessorioId = null;

        this.calcPecas       = [];     // { uid, materialId, larguraMm, alturaMm, quantidade, descricao }
        this.calcAreas       = [];     // { uid, materialId, areaM2 }
        this.calcAcessorios  = [];     // { uid, acessorioId, quantidade }
        this.uidSeq          = 0;
        this.lastResult      = null;

        // Persistência v2
        this.calculosSalvos  = [];
        this.editingCalculoId = null;  // se !=null, "Salvar" vira PUT (atualizar)
        this.salvosSearchTerm = '';

        this.initialized     = false;
        this.init();
    }

    // ══ INIT ═══════════════════════════════════════════════════════════

    init() {
        try {
            // Aguarda o DOM ficar disponível: alguns elementos só existem dentro do
            // módulo orcamentos, que pode estar oculto na primeira renderização.
            if (!document.getElementById('orcTabBtnCalculadora')) {
                document.addEventListener('DOMContentLoaded', () => this._setup());
            } else {
                this._setup();
            }
        } catch (e) {
            console.error('[Marcenaria] init falhou:', e);
        }
    }

    _setup() {
        if (this.initialized) return;
        this.initialized = true;
        this.bindOrcamentoTabs();
        this.bindCalculadoraTabs();
        this.bindCadastrosUI();
        this.bindCalculadoraUI();
        // Carregamentos só são disparados quando o usuário abre a aba Calculadora.
    }

    // ══ AUTH ════════════════════════════════════════════════════════════

    authHeaders() { return window.CRMAuth.authHeaders(); }

    async fetchJson(url, options = {}) {
        const res = await fetch(url, { ...options, headers: { ...this.authHeaders(), ...(options.headers || {}) } });
        if (res.status === 401 || res.status === 403) {
            throw new Error('Sessão expirada. Faça login novamente.');
        }
        const json = await res.json().catch(() => ({}));
        if (!res.ok) {
            const msg = json?.mensagem || `HTTP ${res.status}`;
            throw new Error(msg);
        }
        return json?.dados;
    }

    // ══ TABS DO MÓDULO ORÇAMENTOS (Lista | Calculadora) ═════════════════

    bindOrcamentoTabs() {
        const buttons = document.querySelectorAll('#orcamentos-module .module-tab[data-orc-tab]');
        const panels  = document.querySelectorAll('#orcamentos-module > .module-tab-panel');
        buttons.forEach(btn => {
            btn.addEventListener('click', () => {
                const target = btn.dataset.orcTab;
                buttons.forEach(b => {
                    const active = b === btn;
                    b.classList.toggle('active', active);
                    b.setAttribute('aria-selected', active ? 'true' : 'false');
                });
                panels.forEach(p => {
                    const active = p.id === (target === 'lista' ? 'orcTabLista' : 'orcTabCalculadora');
                    p.classList.toggle('active', active);
                    if (active) p.removeAttribute('hidden'); else p.setAttribute('hidden', '');
                });
                if (target === 'calculadora') {
                    this.loadAllOnce();
                }
            });
        });

        // Acessibilidade — setas para navegar entre tabs
        const tablist = document.querySelector('#orcamentos-module .module-tabs');
        if (tablist) {
            tablist.addEventListener('keydown', (e) => {
                if (e.key !== 'ArrowLeft' && e.key !== 'ArrowRight') return;
                const tabs = Array.from(buttons);
                const idx = tabs.indexOf(document.activeElement);
                if (idx < 0) return;
                e.preventDefault();
                const next = e.key === 'ArrowRight' ? (idx + 1) % tabs.length : (idx - 1 + tabs.length) % tabs.length;
                tabs[next].focus();
                tabs[next].click();
            });
        }
    }

    bindCalculadoraTabs() {
        // Sub-sub-abas internas de Marcenaria (Cadastros | Calcular | Resultado)
        const buttons = document.querySelectorAll('#calcTabMarcenaria .marc-section-tab[data-marc-section]');
        const panels  = document.querySelectorAll('#calcTabMarcenaria .marc-section-panel');
        buttons.forEach(btn => {
            btn.addEventListener('click', () => {
                const target = btn.dataset.marcSection;
                buttons.forEach(b => {
                    const active = b === btn;
                    b.classList.toggle('active', active);
                    b.setAttribute('aria-selected', active ? 'true' : 'false');
                });
                panels.forEach(p => {
                    const map = {
                        cadastros: 'marcSecCadastros',
                        calcular: 'marcSecCalcular',
                        resultado: 'marcSecResultado',
                        salvos: 'marcSecSalvos',
                    };
                    const active = p.id === map[target];
                    p.classList.toggle('active', active);
                    if (active) p.removeAttribute('hidden'); else p.setAttribute('hidden', '');
                });
                if (target === 'salvos') this.loadCalculosSalvos();
            });
        });
    }

    // ══ CARGA INICIAL ══════════════════════════════════════════════════

    async loadAllOnce() {
        if (this._loaded) {
            this.refreshSelectsCalculadora();
            return;
        }
        this._loaded = true;
        try {
            await Promise.all([
                this.loadMateriais(),
                this.loadAcessorios(),
                this.loadConfigMaoObra(),
                this.loadCalculosSalvos(),
            ]);
            this.refreshSelectsCalculadora();
            this.aplicarMargemPadraoNoForm();
        } catch (e) {
            console.error('[Marcenaria] loadAllOnce falhou:', e);
            this.toast('danger', 'fas fa-exclamation-circle', `Erro ao carregar dados: ${this.esc(e.message)}`);
        }
    }

    aplicarMargemPadraoNoForm() {
        const input = document.getElementById('marcCalcMargemLucro');
        if (!input) return;
        const padrao = this.configMaoObra?.margemLucroPadraoPct;
        if (padrao != null) input.placeholder = String(padrao);
    }

    async loadMateriais() {
        try {
            const data = await this.fetchJson(API_MARC_MATERIAIS);
            this.materiais = Array.isArray(data) ? data : [];
            this.renderMateriaisTable();
        } catch (e) {
            console.error('[Marcenaria] loadMateriais:', e);
        }
    }

    async loadAcessorios() {
        try {
            const data = await this.fetchJson(API_MARC_ACESSORIOS);
            this.acessorios = Array.isArray(data) ? data : [];
            this.renderAcessoriosTable();
        } catch (e) {
            console.error('[Marcenaria] loadAcessorios:', e);
        }
    }

    async loadConfigMaoObra() {
        try {
            const data = await this.fetchJson(API_MARC_CONFIG_MAO_OBRA);
            this.configMaoObra = data || { custoDiario: 300 };
            this.renderConfigMaoObra();
        } catch (e) {
            console.error('[Marcenaria] loadConfigMaoObra:', e);
        }
    }

    // ══ CADASTROS - UI ══════════════════════════════════════════════════

    bindCadastrosUI() {
        // Mão de obra
        document.getElementById('marcMaoObraEdit')?.addEventListener('click', () => this.openMaoObraModal());
        document.getElementById('marcMaoObraModalClose')?.addEventListener('click', () => this.closeMaoObraModal());
        document.getElementById('marcMaoObraModalCancel')?.addEventListener('click', () => this.closeMaoObraModal());
        document.getElementById('marcMaoObraModalSave')?.addEventListener('click', () => this.saveMaoObra());

        // Material
        document.getElementById('marcNovoMaterial')?.addEventListener('click', () => this.openMaterialModal());
        document.getElementById('marcMaterialModalClose')?.addEventListener('click', () => this.closeMaterialModal());
        document.getElementById('marcMaterialModalCancel')?.addEventListener('click', () => this.closeMaterialModal());
        document.getElementById('marcMaterialModalSave')?.addEventListener('click', () => this.saveMaterial());

        // Acessório
        document.getElementById('marcNovoAcessorio')?.addEventListener('click', () => this.openAcessorioModal());
        document.getElementById('marcAcessorioModalClose')?.addEventListener('click', () => this.closeAcessorioModal());
        document.getElementById('marcAcessorioModalCancel')?.addEventListener('click', () => this.closeAcessorioModal());
        document.getElementById('marcAcessorioModalSave')?.addEventListener('click', () => this.saveAcessorio());

        // Esc para fechar modais
        document.addEventListener('keydown', (e) => {
            if (e.key !== 'Escape') return;
            ['marcMaterialModalBackdrop', 'marcAcessorioModalBackdrop', 'marcMaoObraModalBackdrop'].forEach(id => {
                const m = document.getElementById(id);
                if (m && !m.classList.contains('hidden')) m.classList.add('hidden');
            });
        });

        // Click fora fecha
        ['marcMaterialModalBackdrop', 'marcAcessorioModalBackdrop', 'marcMaoObraModalBackdrop'].forEach(id => {
            const m = document.getElementById(id);
            if (m) m.addEventListener('click', (e) => { if (e.target === m) m.classList.add('hidden'); });
        });
    }

    renderConfigMaoObra() {
        const c = this.configMaoObra || {};
        const elFunc = document.getElementById('marcMaoObraValor');
        if (elFunc) elFunc.textContent = `${this.formatCurrency(c.custoDiario ?? 300)} / dia`;

        const elAjud = document.getElementById('marcMaoObraAjudanteValor');
        if (elAjud) {
            const tem = c.custoDiarioAjudante != null && Number(c.custoDiarioAjudante) > 0;
            elAjud.textContent = tem ? `${this.formatCurrency(c.custoDiarioAjudante)} / dia` : 'Não configurado';
            elAjud.classList.toggle('marc-mao-obra-empty', !tem);
        }

        const elMargem = document.getElementById('marcMargemPadraoValor');
        if (elMargem) {
            const m = c.margemLucroPadraoPct != null ? Number(c.margemLucroPadraoPct) : 30;
            elMargem.textContent = `${m.toLocaleString('pt-BR', { maximumFractionDigits: 2 })}%`;
        }

        this.aplicarMargemPadraoNoForm();
    }

    renderMateriaisTable() {
        const tbody = document.getElementById('marcMateriaisBody');
        if (!tbody) return;
        if (this.materiais.length === 0) {
            tbody.innerHTML = `<tr class="marc-empty-row"><td colspan="7">Nenhum material cadastrado. Clique em <strong>Novo Material</strong>.</td></tr>`;
            return;
        }
        tbody.innerHTML = this.materiais.map(m => `
            <tr>
                <td>${this.esc(m.nome)}</td>
                <td>${this.esc(MarcenariaModule.CATEGORIA_MATERIAL_LABELS[m.categoria] || m.categoria)}</td>
                <td class="marc-num">${m.espessuraMm ?? '—'}</td>
                <td class="marc-num">${m.larguraChapaMm} × ${m.alturaChapaMm}</td>
                <td class="marc-num">${this.formatCurrency(m.precoChapa)}</td>
                <td>${this.esc(m.fornecedor || '—')}</td>
                <td class="marc-actions-cell">
                    <button class="marc-action-btn" data-action="edit-material" data-id="${m.id}" title="Editar">
                        <i class="fas fa-pen"></i>
                    </button>
                    <button class="marc-action-btn danger" data-action="delete-material" data-id="${m.id}" title="Excluir">
                        <i class="fas fa-trash"></i>
                    </button>
                </td>
            </tr>
        `).join('');
        tbody.querySelectorAll('button[data-action]').forEach(btn => {
            const id = parseInt(btn.dataset.id);
            const action = btn.dataset.action;
            btn.addEventListener('click', () => {
                if (action === 'edit-material') this.openMaterialModal(id);
                if (action === 'delete-material') this.confirmDeleteMaterial(id);
            });
        });
    }

    renderAcessoriosTable() {
        const tbody = document.getElementById('marcAcessoriosBody');
        if (!tbody) return;
        if (this.acessorios.length === 0) {
            tbody.innerHTML = `<tr class="marc-empty-row"><td colspan="6">Nenhum acessório cadastrado. Clique em <strong>Novo Acessório</strong>.</td></tr>`;
            return;
        }
        tbody.innerHTML = this.acessorios.map(a => `
            <tr>
                <td>${this.esc(a.nome)}</td>
                <td>${this.esc(MarcenariaModule.CATEGORIA_ACESSORIO_LABELS[a.categoria] || a.categoria)}</td>
                <td>${this.esc(a.unidadeMedida)}</td>
                <td class="marc-num">${this.formatCurrency(a.precoUnitario)}</td>
                <td>${this.esc(a.fornecedor || '—')}</td>
                <td class="marc-actions-cell">
                    <button class="marc-action-btn" data-action="edit-acessorio" data-id="${a.id}" title="Editar">
                        <i class="fas fa-pen"></i>
                    </button>
                    <button class="marc-action-btn danger" data-action="delete-acessorio" data-id="${a.id}" title="Excluir">
                        <i class="fas fa-trash"></i>
                    </button>
                </td>
            </tr>
        `).join('');
        tbody.querySelectorAll('button[data-action]').forEach(btn => {
            const id = parseInt(btn.dataset.id);
            const action = btn.dataset.action;
            btn.addEventListener('click', () => {
                if (action === 'edit-acessorio') this.openAcessorioModal(id);
                if (action === 'delete-acessorio') this.confirmDeleteAcessorio(id);
            });
        });
    }

    // ── MATERIAL MODAL ─────────────────────────────────────────────────

    openMaterialModal(id = null) {
        this.editingMaterialId = id;
        const m = id ? this.materiais.find(x => x.id === id) : null;
        document.getElementById('marcMaterialModalTitle').textContent = m ? 'Editar Material' : 'Novo Material';
        document.getElementById('marcMaterialId').value          = m?.id ?? '';
        document.getElementById('marcMaterialNome').value        = m?.nome ?? '';
        document.getElementById('marcMaterialCategoria').value   = m?.categoria ?? 'MDF';
        document.getElementById('marcMaterialEspessura').value   = m?.espessuraMm ?? '';
        document.getElementById('marcMaterialLargura').value     = m?.larguraChapaMm ?? 2750;
        document.getElementById('marcMaterialAltura').value      = m?.alturaChapaMm ?? 1850;
        document.getElementById('marcMaterialPreco').value       = m?.precoChapa ?? '';
        document.getElementById('marcMaterialFornecedor').value  = m?.fornecedor ?? '';
        document.getElementById('marcMaterialModalBackdrop').classList.remove('hidden');
    }

    closeMaterialModal() {
        document.getElementById('marcMaterialModalBackdrop').classList.add('hidden');
        this.editingMaterialId = null;
    }

    async saveMaterial() {
        const dto = {
            nome:           document.getElementById('marcMaterialNome').value.trim(),
            categoria:      document.getElementById('marcMaterialCategoria').value,
            espessuraMm:    this.parseIntOrNull(document.getElementById('marcMaterialEspessura').value),
            larguraChapaMm: parseInt(document.getElementById('marcMaterialLargura').value) || 2750,
            alturaChapaMm:  parseInt(document.getElementById('marcMaterialAltura').value)  || 1850,
            precoChapa:     parseFloat(document.getElementById('marcMaterialPreco').value),
            fornecedor:     document.getElementById('marcMaterialFornecedor').value.trim() || null,
        };
        if (!dto.nome) { this.toast('warning', 'fas fa-exclamation-triangle', 'Informe o nome do material.'); return; }
        if (!dto.precoChapa || dto.precoChapa <= 0) {
            this.toast('warning', 'fas fa-exclamation-triangle', 'Preço da chapa deve ser maior que zero.');
            return;
        }
        try {
            if (this.editingMaterialId) {
                await this.fetchJson(`${API_MARC_MATERIAIS}/${this.editingMaterialId}`, {
                    method: 'PUT', body: JSON.stringify(dto),
                });
                this.toast('success', 'fas fa-check-circle', `Material <strong>${this.esc(dto.nome)}</strong> atualizado.`);
            } else {
                await this.fetchJson(API_MARC_MATERIAIS, {
                    method: 'POST', body: JSON.stringify(dto),
                });
                this.toast('success', 'fas fa-check-circle', `Material <strong>${this.esc(dto.nome)}</strong> criado.`);
            }
            this.closeMaterialModal();
            await this.loadMateriais();
            this.refreshSelectsCalculadora();
        } catch (e) {
            this.toast('danger', 'fas fa-exclamation-circle', `Erro ao salvar: ${this.esc(e.message)}`);
        }
    }

    async confirmDeleteMaterial(id) {
        const m = this.materiais.find(x => x.id === id);
        if (!m) return;
        if (!confirm(`Excluir material "${m.nome}"?`)) return;
        try {
            await this.fetchJson(`${API_MARC_MATERIAIS}/${id}`, { method: 'DELETE' });
            this.toast('success', 'fas fa-check-circle', `Material <strong>${this.esc(m.nome)}</strong> excluído.`);
            await this.loadMateriais();
            this.refreshSelectsCalculadora();
        } catch (e) {
            this.toast('danger', 'fas fa-exclamation-circle', `Erro ao excluir: ${this.esc(e.message)}`);
        }
    }

    // ── ACESSÓRIO MODAL ────────────────────────────────────────────────

    openAcessorioModal(id = null) {
        this.editingAcessorioId = id;
        const a = id ? this.acessorios.find(x => x.id === id) : null;
        document.getElementById('marcAcessorioModalTitle').textContent = a ? 'Editar Acessório' : 'Novo Acessório';
        document.getElementById('marcAcessorioId').value          = a?.id ?? '';
        document.getElementById('marcAcessorioNome').value        = a?.nome ?? '';
        document.getElementById('marcAcessorioCategoria').value   = a?.categoria ?? 'DOBRADICA';
        document.getElementById('marcAcessorioUnidade').value     = a?.unidadeMedida ?? 'UN';
        document.getElementById('marcAcessorioPreco').value       = a?.precoUnitario ?? '';
        document.getElementById('marcAcessorioFornecedor').value  = a?.fornecedor ?? '';
        document.getElementById('marcAcessorioModalBackdrop').classList.remove('hidden');
    }

    closeAcessorioModal() {
        document.getElementById('marcAcessorioModalBackdrop').classList.add('hidden');
        this.editingAcessorioId = null;
    }

    async saveAcessorio() {
        const dto = {
            nome:           document.getElementById('marcAcessorioNome').value.trim(),
            categoria:      document.getElementById('marcAcessorioCategoria').value,
            unidadeMedida:  document.getElementById('marcAcessorioUnidade').value,
            precoUnitario:  parseFloat(document.getElementById('marcAcessorioPreco').value),
            fornecedor:     document.getElementById('marcAcessorioFornecedor').value.trim() || null,
        };
        if (!dto.nome) { this.toast('warning', 'fas fa-exclamation-triangle', 'Informe o nome do acessório.'); return; }
        if (!dto.precoUnitario || dto.precoUnitario <= 0) {
            this.toast('warning', 'fas fa-exclamation-triangle', 'Preço unitário deve ser maior que zero.');
            return;
        }
        try {
            if (this.editingAcessorioId) {
                await this.fetchJson(`${API_MARC_ACESSORIOS}/${this.editingAcessorioId}`, {
                    method: 'PUT', body: JSON.stringify(dto),
                });
                this.toast('success', 'fas fa-check-circle', `Acessório <strong>${this.esc(dto.nome)}</strong> atualizado.`);
            } else {
                await this.fetchJson(API_MARC_ACESSORIOS, {
                    method: 'POST', body: JSON.stringify(dto),
                });
                this.toast('success', 'fas fa-check-circle', `Acessório <strong>${this.esc(dto.nome)}</strong> criado.`);
            }
            this.closeAcessorioModal();
            await this.loadAcessorios();
            this.refreshSelectsCalculadora();
        } catch (e) {
            this.toast('danger', 'fas fa-exclamation-circle', `Erro ao salvar: ${this.esc(e.message)}`);
        }
    }

    async confirmDeleteAcessorio(id) {
        const a = this.acessorios.find(x => x.id === id);
        if (!a) return;
        if (!confirm(`Excluir acessório "${a.nome}"?`)) return;
        try {
            await this.fetchJson(`${API_MARC_ACESSORIOS}/${id}`, { method: 'DELETE' });
            this.toast('success', 'fas fa-check-circle', `Acessório <strong>${this.esc(a.nome)}</strong> excluído.`);
            await this.loadAcessorios();
            this.refreshSelectsCalculadora();
        } catch (e) {
            this.toast('danger', 'fas fa-exclamation-circle', `Erro ao excluir: ${this.esc(e.message)}`);
        }
    }

    // ── MÃO DE OBRA MODAL ──────────────────────────────────────────────

    openMaoObraModal() {
        const c = this.configMaoObra || {};
        document.getElementById('marcMaoObraCustoDiario').value = c.custoDiario ?? 300;
        document.getElementById('marcMaoObraCustoAjudante').value =
            (c.custoDiarioAjudante != null && Number(c.custoDiarioAjudante) > 0) ? c.custoDiarioAjudante : '';
        document.getElementById('marcMargemPadraoPct').value = c.margemLucroPadraoPct ?? 30;
        document.getElementById('marcMaoObraModalBackdrop').classList.remove('hidden');
    }

    closeMaoObraModal() {
        document.getElementById('marcMaoObraModalBackdrop').classList.add('hidden');
    }

    async saveMaoObra() {
        const custoDiario = parseFloat(document.getElementById('marcMaoObraCustoDiario').value);
        const custoAjudanteRaw = document.getElementById('marcMaoObraCustoAjudante').value.trim();
        const custoDiarioAjudante = custoAjudanteRaw === '' ? null : parseFloat(custoAjudanteRaw);
        const margemLucroPadraoPct = parseFloat(document.getElementById('marcMargemPadraoPct').value);

        if (isNaN(custoDiario) || custoDiario < 0) {
            this.toast('warning', 'fas fa-exclamation-triangle', 'Custo diário do funcionário inválido.');
            return;
        }
        if (custoDiarioAjudante != null && (isNaN(custoDiarioAjudante) || custoDiarioAjudante < 0)) {
            this.toast('warning', 'fas fa-exclamation-triangle', 'Custo diário do ajudante inválido.');
            return;
        }
        if (isNaN(margemLucroPadraoPct) || margemLucroPadraoPct < 0) {
            this.toast('warning', 'fas fa-exclamation-triangle', 'Margem de lucro padrão inválida.');
            return;
        }
        try {
            const data = await this.fetchJson(API_MARC_CONFIG_MAO_OBRA, {
                method: 'PUT',
                body: JSON.stringify({ custoDiario, custoDiarioAjudante, margemLucroPadraoPct }),
            });
            this.configMaoObra = data;
            this.renderConfigMaoObra();
            this.closeMaoObraModal();
            this.toast('success', 'fas fa-check-circle', 'Configuração salva.');
        } catch (e) {
            this.toast('danger', 'fas fa-exclamation-circle', `Erro ao salvar: ${this.esc(e.message)}`);
        }
    }

    // ══ CALCULADORA - UI ═══════════════════════════════════════════════

    bindCalculadoraUI() {
        document.getElementById('marcCalcModo')?.addEventListener('change', (e) => {
            const modo = e.target.value;
            const isAprov = (modo === 'APROVEITAMENTO');
            document.getElementById('marcPecasWrap').classList.toggle('hidden', !isAprov);
            document.getElementById('marcAreasWrap').classList.toggle('hidden', isAprov);
            document.getElementById('marcCalcMargemWrap').classList.toggle('hidden', !isAprov);
            document.getElementById('marcCalcRotacaoWrap').classList.toggle('hidden', !isAprov);
        });

        document.getElementById('marcAddPeca')?.addEventListener('click', () => this.addPecaRow());
        document.getElementById('marcAddArea')?.addEventListener('click', () => this.addAreaRow());
        document.getElementById('marcAddAcessorioCalc')?.addEventListener('click', () => this.addAcessorioCalcRow());

        document.getElementById('marcCalcular')?.addEventListener('click', () => this.calcular());
        document.getElementById('marcLimparCalc')?.addEventListener('click', () => this.limparCalculadora());

        // ── v2: Salvar cálculo + voltar pra editar ──
        document.getElementById('marcResultRecalcular')?.addEventListener('click', () => {
            document.querySelector('#calcTabMarcenaria .marc-section-tab[data-marc-section="calcular"]').click();
        });
        document.getElementById('marcResultSalvar')?.addEventListener('click', () => this.openSaveCalcModal());

        // Modal de salvar
        document.getElementById('marcSaveCalcModalClose')?.addEventListener('click', () => this.closeSaveCalcModal());
        document.getElementById('marcSaveCalcModalCancel')?.addEventListener('click', () => this.closeSaveCalcModal());
        document.getElementById('marcSaveCalcModalSave')?.addEventListener('click', () => this.salvarCalculo());
        const saveBackdrop = document.getElementById('marcSaveCalcModalBackdrop');
        if (saveBackdrop) saveBackdrop.addEventListener('click', (e) => {
            if (e.target === saveBackdrop) this.closeSaveCalcModal();
        });
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && saveBackdrop && !saveBackdrop.classList.contains('hidden')) {
                this.closeSaveCalcModal();
            }
        });

        // Tabela de Salvos: busca local
        document.getElementById('marcSalvosSearch')?.addEventListener('input', (e) => {
            this.salvosSearchTerm = e.target.value.trim().toLowerCase();
            this.renderSalvosTable();
        });
    }

    refreshSelectsCalculadora() {
        // Atualiza os <select> dos rows de peças, áreas e acessórios na calculadora.
        document.querySelectorAll('select.marc-row-material').forEach(sel => this.populateMaterialSelect(sel, sel.value));
        document.querySelectorAll('select.marc-row-acessorio').forEach(sel => this.populateAcessorioSelect(sel, sel.value));
    }

    populateMaterialSelect(sel, currentValue = '') {
        sel.innerHTML = `<option value="">— selecione —</option>` +
            this.materiais.map(m => `<option value="${m.id}">${this.esc(m.nome)}</option>`).join('');
        if (currentValue) sel.value = currentValue;
    }

    populateAcessorioSelect(sel, currentValue = '') {
        sel.innerHTML = `<option value="">— selecione —</option>` +
            this.acessorios.map(a => `<option value="${a.id}">${this.esc(a.nome)} (${this.esc(a.unidadeMedida)})</option>`).join('');
        if (currentValue) sel.value = currentValue;
    }

    addPecaRow(p = {}) {
        const uid = `pc-${++this.uidSeq}`;
        const tbody = document.getElementById('marcPecasBody');
        if (!tbody) return;
        const tr = document.createElement('tr');
        tr.dataset.uid = uid;
        tr.innerHTML = `
            <td><input type="text" class="marc-row-input marc-peca-desc" placeholder="ex: lateral" value="${this.esc(p.descricao || '')}"></td>
            <td><select class="marc-row-input marc-row-material marc-peca-material"></select></td>
            <td><input type="number" min="1" class="marc-row-input narrow marc-peca-largura" value="${p.larguraMm ?? ''}"></td>
            <td><input type="number" min="1" class="marc-row-input narrow marc-peca-altura" value="${p.alturaMm ?? ''}"></td>
            <td><input type="number" min="1" class="marc-row-input narrow marc-peca-qtd" value="${p.quantidade ?? 1}"></td>
            <td class="marc-actions-cell"><button class="marc-action-btn danger" data-action="remove" title="Remover"><i class="fas fa-trash"></i></button></td>
        `;
        tbody.appendChild(tr);
        this.populateMaterialSelect(tr.querySelector('.marc-peca-material'), p.materialId ?? '');
        tr.querySelector('button[data-action="remove"]').addEventListener('click', () => tr.remove());
    }

    addAreaRow(a = {}) {
        const uid = `ar-${++this.uidSeq}`;
        const tbody = document.getElementById('marcAreasBody');
        if (!tbody) return;
        const tr = document.createElement('tr');
        tr.dataset.uid = uid;
        tr.innerHTML = `
            <td><select class="marc-row-input marc-row-material marc-area-material"></select></td>
            <td><input type="number" min="0.01" step="0.01" class="marc-row-input narrow marc-area-m2" value="${a.areaM2 ?? ''}"></td>
            <td class="marc-actions-cell"><button class="marc-action-btn danger" data-action="remove" title="Remover"><i class="fas fa-trash"></i></button></td>
        `;
        tbody.appendChild(tr);
        this.populateMaterialSelect(tr.querySelector('.marc-area-material'), a.materialId ?? '');
        tr.querySelector('button[data-action="remove"]').addEventListener('click', () => tr.remove());
    }

    addAcessorioCalcRow(a = {}) {
        const uid = `ac-${++this.uidSeq}`;
        const tbody = document.getElementById('marcAcessoriosCalcBody');
        if (!tbody) return;
        const tr = document.createElement('tr');
        tr.dataset.uid = uid;
        tr.innerHTML = `
            <td><select class="marc-row-input marc-row-acessorio marc-ac-id"></select></td>
            <td><input type="number" min="0.001" step="0.001" class="marc-row-input narrow marc-ac-qtd" value="${a.quantidade ?? 1}"></td>
            <td class="marc-actions-cell"><button class="marc-action-btn danger" data-action="remove" title="Remover"><i class="fas fa-trash"></i></button></td>
        `;
        tbody.appendChild(tr);
        this.populateAcessorioSelect(tr.querySelector('.marc-ac-id'), a.acessorioId ?? '');
        tr.querySelector('button[data-action="remove"]').addEventListener('click', () => tr.remove());
    }

    coletarPecas() {
        const trs = document.querySelectorAll('#marcPecasBody tr');
        const out = [];
        trs.forEach(tr => {
            const materialId = parseInt(tr.querySelector('.marc-peca-material').value);
            const larguraMm  = parseInt(tr.querySelector('.marc-peca-largura').value);
            const alturaMm   = parseInt(tr.querySelector('.marc-peca-altura').value);
            const quantidade = parseInt(tr.querySelector('.marc-peca-qtd').value);
            const descricao  = tr.querySelector('.marc-peca-desc').value.trim();
            if (materialId && larguraMm > 0 && alturaMm > 0 && quantidade > 0) {
                out.push({ materialId, larguraMm, alturaMm, quantidade, descricao });
            }
        });
        return out;
    }

    coletarAreas() {
        const trs = document.querySelectorAll('#marcAreasBody tr');
        const out = [];
        trs.forEach(tr => {
            const materialId = parseInt(tr.querySelector('.marc-area-material').value);
            const areaM2     = parseFloat(tr.querySelector('.marc-area-m2').value);
            if (materialId && areaM2 > 0) out.push({ materialId, areaM2 });
        });
        return out;
    }

    coletarAcessoriosCalc() {
        const trs = document.querySelectorAll('#marcAcessoriosCalcBody tr');
        const out = [];
        trs.forEach(tr => {
            const acessorioId = parseInt(tr.querySelector('.marc-ac-id').value);
            const quantidade  = parseFloat(tr.querySelector('.marc-ac-qtd').value);
            if (acessorioId && quantidade > 0) out.push({ acessorioId, quantidade });
        });
        return out;
    }

    limparCalculadora() {
        document.getElementById('marcCalcNome').value = '';
        document.getElementById('marcCalcDias').value = '1';
        document.getElementById('marcCalcDiasAjudante').value = '';
        document.getElementById('marcCalcMargemLucro').value = '';
        document.getElementById('marcPecasBody').innerHTML = '';
        document.getElementById('marcAreasBody').innerHTML = '';
        document.getElementById('marcAcessoriosCalcBody').innerHTML = '';
        document.getElementById('marcResultEmpty').classList.remove('hidden');
        document.getElementById('marcResultContent').classList.add('hidden');
        document.getElementById('marcResultActions')?.classList.add('hidden');
        this.lastResult = null;
        this.editingCalculoId = null;
    }

    // ══ CÁLCULO ════════════════════════════════════════════════════════

    calcular() {
        const nomeProduto    = document.getElementById('marcCalcNome').value.trim();
        const modo           = document.getElementById('marcCalcModo').value;
        const diasFuncionario = parseFloat(document.getElementById('marcCalcDias').value) || 0;
        const diasAjudanteRaw = document.getElementById('marcCalcDiasAjudante').value.trim();
        const diasAjudante   = diasAjudanteRaw === '' ? 0 : (parseFloat(diasAjudanteRaw) || 0);
        const margemLucroRaw = document.getElementById('marcCalcMargemLucro').value.trim();
        const margemLucroPct = margemLucroRaw === ''
            ? Number(this.configMaoObra?.margemLucroPadraoPct ?? 30)
            : (parseFloat(margemLucroRaw) || 0);
        const margemCorteMm  = parseInt(document.getElementById('marcCalcMargem').value) || 0;
        const permitirRotacao = document.getElementById('marcCalcRotacao').value === 'true';

        const acessoriosUsados = this.coletarAcessoriosCalc();

        let detalhamentoMateriais = [];
        let layouts = [];
        let totalMateriais = 0;

        if (modo === 'APROVEITAMENTO') {
            const pecas = this.coletarPecas();
            if (pecas.length === 0) {
                this.toast('warning', 'fas fa-exclamation-triangle', 'Adicione ao menos uma peça para calcular.');
                return;
            }
            const result = this.calcularAproveitamento(pecas, margemCorteMm, permitirRotacao);
            detalhamentoMateriais = result.detalhamento;
            layouts = result.layouts;
            totalMateriais = result.total;
        } else {
            const areas = this.coletarAreas();
            if (areas.length === 0) {
                this.toast('warning', 'fas fa-exclamation-triangle', 'Adicione ao menos uma área de material.');
                return;
            }
            const result = this.calcularPorArea(areas);
            detalhamentoMateriais = result.detalhamento;
            totalMateriais = result.total;
        }

        // Acessórios
        const detalhamentoAcessorios = acessoriosUsados.map(item => {
            const a = this.acessorios.find(x => x.id === item.acessorioId);
            const preco = parseFloat(a?.precoUnitario ?? 0);
            const subtotal = preco * item.quantidade;
            return {
                acessorioId: item.acessorioId,
                nome: a?.nome ?? `#${item.acessorioId}`,
                quantidade: item.quantidade,
                precoUnitario: preco,
                subtotal,
            };
        });
        const totalAcessorios = detalhamentoAcessorios.reduce((s, d) => s + d.subtotal, 0);

        // Mão de obra: funcionário (sempre) + ajudante (opcional, só se config tiver custoDiarioAjudante > 0)
        const custoDiarioFuncionario = parseFloat(this.configMaoObra?.custoDiario ?? 300);
        const custoDiarioAjudanteCfg = this.configMaoObra?.custoDiarioAjudante;
        const temAjudanteConfig = custoDiarioAjudanteCfg != null && Number(custoDiarioAjudanteCfg) > 0;
        const custoDiarioAjudante = temAjudanteConfig ? Number(custoDiarioAjudanteCfg) : 0;

        const custoFuncionario = custoDiarioFuncionario * diasFuncionario;
        const custoAjudante    = (diasAjudante > 0 && temAjudanteConfig) ? custoDiarioAjudante * diasAjudante : 0;
        const custoMaoObra     = custoFuncionario + custoAjudante;

        const custoProducao = totalMateriais + totalAcessorios + custoMaoObra;
        const valorLucro    = custoProducao * (margemLucroPct / 100);
        const precoVenda    = custoProducao + valorLucro;

        if (diasAjudante > 0 && !temAjudanteConfig) {
            this.toast('warning', 'fas fa-exclamation-triangle',
                'Você informou dias do ajudante, mas o custo diário do ajudante não está configurado. <a href="#" onclick="document.getElementById(\'marcSecBtnCadastros\').click(); return false;" style="color:inherit;text-decoration:underline">Configure aqui</a>.', 8000);
        }

        this.lastResult = {
            nomeProduto, modo,
            custoMateriais:  { total: totalMateriais,   detalhamento: detalhamentoMateriais },
            custoAcessorios: { total: totalAcessorios,  detalhamento: detalhamentoAcessorios },
            custoMaoObra: {
                total: custoMaoObra,
                diasFuncionario, custoDiarioFuncionario, custoFuncionario,
                diasAjudante, custoDiarioAjudante, custoAjudante,
                temAjudante: custoAjudante > 0,
            },
            custoProducao,
            margemLucroPct, valorLucro, precoVenda,
            // compat v1
            custoTotal: precoVenda,
            layouts,
            // estado bruto para "Salvar" depois
            _input: {
                modo,
                pecas: modo === 'APROVEITAMENTO' ? this.coletarPecas() : [],
                areasPorMaterial: modo === 'METRO_QUADRADO' ? this.coletarAreas() : [],
                acessorios: acessoriosUsados,
                diasFuncionario, diasAjudante,
                margemCorteMm, permitirRotacao, margemLucroPct,
            },
        };

        this.renderResultado();
        // Navega automaticamente para a aba Resultado
        document.querySelector('#calcTabMarcenaria .marc-section-tab[data-marc-section="resultado"]').click();
    }

    /**
     * Modo "Por metro quadrado".
     * chapasNecessarias = ceil(area / areaChapa)
     * subtotal          = chapasNecessarias × precoChapa
     */
    calcularPorArea(areas) {
        const detalhamento = [];
        let total = 0;
        // Agrega áreas por material (caso a usuária liste o mesmo mat. duas vezes)
        const agg = new Map();
        areas.forEach(a => {
            agg.set(a.materialId, (agg.get(a.materialId) ?? 0) + a.areaM2);
        });
        agg.forEach((areaM2, materialId) => {
            const m = this.materiais.find(x => x.id === materialId);
            if (!m) return;
            const areaChapaM2 = (m.larguraChapaMm * m.alturaChapaMm) / 1_000_000;
            const chapasNecessarias = Math.max(1, Math.ceil(areaM2 / areaChapaM2));
            const precoChapa = parseFloat(m.precoChapa);
            const subtotal = chapasNecessarias * precoChapa;
            const aproveitamentoPct = areaChapaM2 > 0
                ? Math.min(100, (areaM2 / (chapasNecessarias * areaChapaM2)) * 100)
                : 0;
            detalhamento.push({
                materialId, nome: m.nome,
                chapasNecessarias, precoChapa, subtotal,
                areaUtilizadaM2: areaM2, areaChapaM2, aproveitamentoPct,
            });
            total += subtotal;
        });
        return { detalhamento, total };
    }

    /**
     * Modo "Por aproveitamento de chapa".
     * Usa o packer 2D Guillotine BSSF + rotação 90° (vide MarcenariaPacker).
     * Retorna: chapas necessárias por material, layouts e custo total.
     */
    calcularAproveitamento(pecas, margemCorteMm, permitirRotacao) {
        // Agrupa peças por material — cada material é empacotado independentemente.
        const groupByMat = new Map();
        pecas.forEach(p => {
            if (!groupByMat.has(p.materialId)) groupByMat.set(p.materialId, []);
            groupByMat.get(p.materialId).push(p);
        });

        const detalhamento = [];
        const layouts = [];
        let total = 0;

        groupByMat.forEach((pecasDoMat, materialId) => {
            const m = this.materiais.find(x => x.id === materialId);
            if (!m) return;

            // Expande quantidades em peças individuais
            const items = [];
            let pieceUid = 0;
            pecasDoMat.forEach(p => {
                for (let i = 0; i < p.quantidade; i++) {
                    items.push({
                        id: ++pieceUid,
                        w: p.larguraMm + margemCorteMm,
                        h: p.alturaMm + margemCorteMm,
                        baseW: p.larguraMm,
                        baseH: p.alturaMm,
                        descricao: p.descricao || `${p.larguraMm}×${p.alturaMm}`,
                    });
                }
            });

            const sheet = { w: m.larguraChapaMm, h: m.alturaChapaMm };
            const packed = MarcenariaPacker.pack(items, sheet, { allowRotation: !!permitirRotacao });

            // Gera layouts (descontando a margem na visualização)
            packed.forEach((sheetPack, idx) => {
                layouts.push({
                    materialId,
                    materialNome: m.nome,
                    indiceChapa: idx + 1,
                    larguraChapaMm: sheet.w,
                    alturaChapaMm: sheet.h,
                    pecasPosicionadas: sheetPack.pieces.map(pp => ({
                        descricao: pp.descricao,
                        x: pp.x,
                        y: pp.y,
                        larguraMm: pp.w - margemCorteMm,
                        alturaMm:  pp.h - margemCorteMm,
                        rotacionada: pp.rotated,
                    })),
                });
            });

            const chapasNecessarias = packed.length;
            const areaChapaM2 = (sheet.w * sheet.h) / 1_000_000;
            const areaUtilizadaM2 = items.reduce((s, it) => s + (it.baseW * it.baseH) / 1_000_000, 0);
            const aproveitamentoPct = (chapasNecessarias > 0 && areaChapaM2 > 0)
                ? Math.min(100, (areaUtilizadaM2 / (chapasNecessarias * areaChapaM2)) * 100)
                : 0;
            const precoChapa = parseFloat(m.precoChapa);
            const subtotal = chapasNecessarias * precoChapa;

            detalhamento.push({
                materialId, nome: m.nome,
                chapasNecessarias, precoChapa, subtotal,
                areaUtilizadaM2, areaChapaM2, aproveitamentoPct,
            });
            total += subtotal;
        });

        return { detalhamento, layouts, total };
    }

    // ══ RENDER RESULTADO ═══════════════════════════════════════════════

    renderResultado() {
        const empty   = document.getElementById('marcResultEmpty');
        const content = document.getElementById('marcResultContent');
        const actions = document.getElementById('marcResultActions');
        if (!this.lastResult) {
            empty.classList.remove('hidden');
            content.classList.add('hidden');
            actions?.classList.add('hidden');
            return;
        }
        empty.classList.add('hidden');
        content.classList.remove('hidden');
        actions?.classList.remove('hidden');

        const r = this.lastResult;
        const fmt = (v) => this.formatCurrency(v);
        const fmtNum = (v, d = 2) => Number(v ?? 0).toLocaleString('pt-BR', { maximumFractionDigits: d, minimumFractionDigits: d });

        const editandoBadge = this.editingCalculoId
            ? `<div class="marc-editing-badge"><i class="fas fa-pen"></i> Editando cálculo salvo #${this.editingCalculoId} — ao salvar, será atualizado.</div>`
            : '';
        const headerHtml = r.nomeProduto
            ? `<h2 style="margin-bottom:var(--spacing-sm);font-size:var(--font-size-xl);font-weight:var(--font-weight-semibold)">${this.esc(r.nomeProduto)}</h2>`
            : '';

        const mo = r.custoMaoObra;
        const moSubtitle = mo.temAjudante
            ? `<div class="text-muted text-sm" style="margin-top:4px">
                  Func.: ${fmtNum(mo.diasFuncionario)}d × ${fmt(mo.custoDiarioFuncionario)} = ${fmt(mo.custoFuncionario)}<br>
                  Ajud.: ${fmtNum(mo.diasAjudante)}d × ${fmt(mo.custoDiarioAjudante)} = ${fmt(mo.custoAjudante)}
               </div>`
            : `<div class="text-muted text-sm" style="margin-top:4px">${fmtNum(mo.diasFuncionario)} dias × ${fmt(mo.custoDiarioFuncionario)}</div>`;

        const summaryHtml = `
            ${editandoBadge}
            ${headerHtml}
            <div class="marc-result-summary">
                <div class="marc-result-card">
                    <span class="marc-result-card-label"><i class="fas fa-layer-group"></i> Materiais</span>
                    <div class="marc-result-card-value">${fmt(r.custoMateriais.total)}</div>
                </div>
                <div class="marc-result-card">
                    <span class="marc-result-card-label"><i class="fas fa-screwdriver-wrench"></i> Acessórios</span>
                    <div class="marc-result-card-value">${fmt(r.custoAcessorios.total)}</div>
                </div>
                <div class="marc-result-card">
                    <span class="marc-result-card-label"><i class="fas fa-hard-hat"></i> Mão de obra</span>
                    <div class="marc-result-card-value">${fmt(mo.total)}</div>
                    ${moSubtitle}
                </div>
                <div class="marc-result-card is-producao">
                    <span class="marc-result-card-label"><i class="fas fa-industry"></i> Custo de produção</span>
                    <div class="marc-result-card-value">${fmt(r.custoProducao)}</div>
                </div>
                <div class="marc-result-card is-margem">
                    <span class="marc-result-card-label"><i class="fas fa-percentage"></i> Margem (${fmtNum(r.margemLucroPct)}%)</span>
                    <div class="marc-result-card-value">${fmt(r.valorLucro)}</div>
                </div>
                <div class="marc-result-card is-venda">
                    <span class="marc-result-card-label"><i class="fas fa-tags"></i> Preço de venda</span>
                    <div class="marc-result-card-value">${fmt(r.precoVenda)}</div>
                </div>
            </div>
        `;

        // Detalhamento materiais
        const detMatRows = r.custoMateriais.detalhamento.map(d => `
            <tr>
                <td>${this.esc(d.nome)}</td>
                <td class="marc-num">${d.chapasNecessarias}</td>
                <td class="marc-num">${fmt(d.precoChapa)}</td>
                <td class="marc-num">${fmtNum(d.areaUtilizadaM2, 2)} m²</td>
                <td class="marc-num">${fmtNum(d.aproveitamentoPct, 1)}%</td>
                <td class="marc-num"><strong>${fmt(d.subtotal)}</strong></td>
            </tr>
        `).join('');
        const matTable = r.custoMateriais.detalhamento.length === 0 ? '' : `
            <div class="marc-card">
                <h3 class="marc-card-title"><i class="fas fa-layer-group"></i> Detalhamento de materiais</h3>
                <table class="marc-detalhe-table">
                    <thead>
                        <tr>
                            <th>Material</th>
                            <th class="marc-num">Chapas</th>
                            <th class="marc-num">Preço/Chapa</th>
                            <th class="marc-num">Área usada</th>
                            <th class="marc-num">Aproveit.</th>
                            <th class="marc-num">Subtotal</th>
                        </tr>
                    </thead>
                    <tbody>${detMatRows}</tbody>
                    <tfoot>
                        <tr><td colspan="5" class="marc-num">Total materiais</td><td class="marc-num">${fmt(r.custoMateriais.total)}</td></tr>
                    </tfoot>
                </table>
            </div>
        `;

        // Detalhamento acessórios
        const detAcRows = r.custoAcessorios.detalhamento.map(d => `
            <tr>
                <td>${this.esc(d.nome)}</td>
                <td class="marc-num">${fmtNum(d.quantidade, 3)}</td>
                <td class="marc-num">${fmt(d.precoUnitario)}</td>
                <td class="marc-num"><strong>${fmt(d.subtotal)}</strong></td>
            </tr>
        `).join('');
        const acTable = r.custoAcessorios.detalhamento.length === 0 ? '' : `
            <div class="marc-card">
                <h3 class="marc-card-title"><i class="fas fa-screwdriver-wrench"></i> Detalhamento de acessórios</h3>
                <table class="marc-detalhe-table">
                    <thead>
                        <tr>
                            <th>Acessório</th>
                            <th class="marc-num">Qtd.</th>
                            <th class="marc-num">Preço unit.</th>
                            <th class="marc-num">Subtotal</th>
                        </tr>
                    </thead>
                    <tbody>${detAcRows}</tbody>
                    <tfoot>
                        <tr><td colspan="3" class="marc-num">Total acessórios</td><td class="marc-num">${fmt(r.custoAcessorios.total)}</td></tr>
                    </tfoot>
                </table>
            </div>
        `;

        // Detalhamento de mão de obra (com ou sem ajudante)
        const moRows = mo.temAjudante
            ? `<tr>
                   <td>Funcionário principal</td>
                   <td class="marc-num">${fmtNum(mo.diasFuncionario)} dias</td>
                   <td class="marc-num">${fmt(mo.custoDiarioFuncionario)}</td>
                   <td class="marc-num"><strong>${fmt(mo.custoFuncionario)}</strong></td>
               </tr>
               <tr>
                   <td>Ajudante</td>
                   <td class="marc-num">${fmtNum(mo.diasAjudante)} dias</td>
                   <td class="marc-num">${fmt(mo.custoDiarioAjudante)}</td>
                   <td class="marc-num"><strong>${fmt(mo.custoAjudante)}</strong></td>
               </tr>`
            : `<tr>
                   <td>Funcionário</td>
                   <td class="marc-num">${fmtNum(mo.diasFuncionario)} dias</td>
                   <td class="marc-num">${fmt(mo.custoDiarioFuncionario)}</td>
                   <td class="marc-num"><strong>${fmt(mo.custoFuncionario)}</strong></td>
               </tr>`;

        const moTable = `
            <div class="marc-card">
                <h3 class="marc-card-title"><i class="fas fa-hard-hat"></i> Detalhamento de mão de obra</h3>
                <table class="marc-detalhe-table">
                    <thead>
                        <tr>
                            <th>Quem</th>
                            <th class="marc-num">Dias</th>
                            <th class="marc-num">Custo/dia</th>
                            <th class="marc-num">Subtotal</th>
                        </tr>
                    </thead>
                    <tbody>${moRows}</tbody>
                    <tfoot>
                        <tr><td colspan="3" class="marc-num">Total mão de obra</td><td class="marc-num">${fmt(mo.total)}</td></tr>
                    </tfoot>
                </table>
            </div>
        `;

        const totaisTable = `
            <div class="marc-card marc-totais-card">
                <h3 class="marc-card-title"><i class="fas fa-coins"></i> Totais</h3>
                <table class="marc-detalhe-table">
                    <tbody>
                        <tr><td>Custo de produção (materiais + acessórios + mão de obra)</td>
                            <td class="marc-num"><strong>${fmt(r.custoProducao)}</strong></td></tr>
                        <tr><td>Margem de lucro (${fmtNum(r.margemLucroPct)}% sobre o custo de produção)</td>
                            <td class="marc-num"><strong>${fmt(r.valorLucro)}</strong></td></tr>
                    </tbody>
                    <tfoot>
                        <tr class="marc-totais-venda">
                            <td>Preço de venda</td>
                            <td class="marc-num">${fmt(r.precoVenda)}</td>
                        </tr>
                    </tfoot>
                </table>
            </div>
        `;

        // Layouts (apenas no modo APROVEITAMENTO)
        let layoutsHtml = '';
        if (r.layouts && r.layouts.length > 0) {
            // Agrupa por material
            const byMat = new Map();
            r.layouts.forEach(L => {
                if (!byMat.has(L.materialId)) byMat.set(L.materialId, []);
                byMat.get(L.materialId).push(L);
            });
            const blocks = [];
            byMat.forEach((list) => {
                const matName = list[0].materialNome
                    || (this.materiais.find(m => m.id === list[0].materialId)?.nome ?? '');
                const cards = list.map((L) => this.renderLayoutSvg(L, list.length, matName)).join('');
                blocks.push(`
                    <div class="marc-card">
                        <h3 class="marc-card-title"><i class="fas fa-th"></i> Layout — ${this.esc(matName)}</h3>
                        <p class="marc-card-sub">${list.length} chapa(s) necessária(s).</p>
                        <div class="marc-layouts-wrap">${cards}</div>
                    </div>
                `);
            });
            layoutsHtml = blocks.join('');
        }

        content.innerHTML = summaryHtml + matTable + acTable + moTable + totaisTable + layoutsHtml;
    }

    renderLayoutSvg(layout, totalChapas, matName) {
        const W = layout.larguraChapaMm;
        const H = layout.alturaChapaMm;
        const areaTotal = W * H;
        const areaPecas = layout.pecasPosicionadas.reduce((s, p) => s + (p.larguraMm * p.alturaMm), 0);
        const aprov = areaTotal > 0 ? (areaPecas / areaTotal) * 100 : 0;

        const rects = layout.pecasPosicionadas.map(p => {
            const cx = p.x + p.larguraMm / 2;
            const cy = p.y + p.alturaMm / 2;
            const cls = p.rotacionada ? 'marc-piece-rect rotated' : 'marc-piece-rect';
            return `
                <g>
                    <rect class="${cls}" x="${p.x}" y="${p.y}" width="${p.larguraMm}" height="${p.alturaMm}"></rect>
                    ${p.descricao ? `<text class="marc-piece-label" x="${cx}" y="${cy - 18}">${this.esc(p.descricao)}</text>` : ''}
                    <text class="marc-piece-dim" x="${cx}" y="${cy + (p.descricao ? 20 : 0)}">${p.larguraMm}×${p.alturaMm}${p.rotacionada ? ' ↻' : ''}</text>
                </g>`;
        }).join('');

        return `
            <div class="marc-layout-card">
                <div class="marc-layout-head">
                    <h4 class="marc-layout-title">Chapa ${layout.indiceChapa} de ${totalChapas} — ${W}×${H}mm</h4>
                    <span class="marc-layout-aprov">${aprov.toFixed(1)}% aproveit.</span>
                </div>
                <div class="marc-layout-svg-wrap">
                    <svg class="marc-layout-svg" viewBox="0 0 ${W} ${H}" preserveAspectRatio="xMidYMid meet"
                         role="img" aria-label="Layout de corte da chapa ${layout.indiceChapa} de ${matName}">
                        <rect x="0" y="0" width="${W}" height="${H}" fill="#fff" stroke="#e2e8f0" stroke-width="3"></rect>
                        ${rects}
                    </svg>
                </div>
            </div>
        `;
    }

    // ══ PERSISTÊNCIA DE CÁLCULOS (v2) ══════════════════════════════════

    openSaveCalcModal() {
        if (!this.lastResult) {
            this.toast('warning', 'fas fa-exclamation-triangle', 'Calcule antes de salvar.');
            return;
        }
        const isEdit = !!this.editingCalculoId;
        const titleEl = document.getElementById('marcSaveCalcModalTitle');
        if (titleEl) titleEl.textContent = isEdit ? 'Atualizar cálculo' : 'Salvar cálculo';
        const nomeInput = document.getElementById('marcSaveCalcNome');
        if (nomeInput) {
            nomeInput.value = this.lastResult.nomeProduto || '';
            setTimeout(() => nomeInput.focus(), 50);
        }
        document.getElementById('marcSaveCalcObs').value = this.lastResult._observacoes || '';
        document.getElementById('marcSaveCalcModalBackdrop').classList.remove('hidden');
    }

    closeSaveCalcModal() {
        document.getElementById('marcSaveCalcModalBackdrop').classList.add('hidden');
    }

    async salvarCalculo() {
        if (!this.lastResult) return;
        const nome = document.getElementById('marcSaveCalcNome').value.trim();
        const observacoes = document.getElementById('marcSaveCalcObs').value.trim() || null;
        if (!nome) {
            this.toast('warning', 'fas fa-exclamation-triangle', 'Informe um nome para o cálculo.');
            return;
        }
        const r = this.lastResult;
        const i = r._input;
        const dto = {
            nome,
            modo: i.modo,
            diasFuncionario: i.diasFuncionario,
            diasAjudante: i.diasAjudante > 0 ? i.diasAjudante : null,
            margemLucroPct: i.margemLucroPct,
            margemCorteMm: i.margemCorteMm,
            permitirRotacao: i.permitirRotacao,
            custoMateriais: r.custoMateriais.total,
            custoAcessorios: r.custoAcessorios.total,
            observacoes,
            pecas: i.pecas.map(p => ({
                materialId: p.materialId,
                larguraMm: p.larguraMm,
                alturaMm: p.alturaMm,
                quantidade: p.quantidade,
                descricao: p.descricao || null,
            })),
            areasPorMaterial: i.areasPorMaterial.map(a => ({
                materialId: a.materialId,
                areaM2: a.areaM2,
            })),
            acessorios: i.acessorios.map(a => ({
                acessorioId: a.acessorioId,
                quantidade: a.quantidade,
            })),
        };
        try {
            const url = this.editingCalculoId ? `${API_MARC_CALCULOS}/${this.editingCalculoId}` : API_MARC_CALCULOS;
            const method = this.editingCalculoId ? 'PUT' : 'POST';
            const data = await this.fetchJson(url, { method, body: JSON.stringify(dto) });
            this.toast('success', 'fas fa-check-circle',
                `Cálculo <strong>${this.esc(data.nome)}</strong> ${this.editingCalculoId ? 'atualizado' : 'salvo'}.`);
            this.closeSaveCalcModal();
            this.editingCalculoId = data.id;
            this.lastResult.nomeProduto = data.nome;
            this.lastResult._observacoes = data.observacoes;
            this.renderResultado();
            await this.loadCalculosSalvos();
        } catch (e) {
            this.toast('danger', 'fas fa-exclamation-circle', `Erro ao salvar: ${this.esc(e.message)}`);
        }
    }

    async loadCalculosSalvos() {
        try {
            const data = await this.fetchJson(`${API_MARC_CALCULOS}?size=500&sort=dataCriacao,desc`);
            this.calculosSalvos = (data?.content ?? data ?? []);
            this.renderSalvosTable();
        } catch (e) {
            console.error('[Marcenaria] loadCalculosSalvos:', e);
        }
    }

    renderSalvosTable() {
        const tbody = document.getElementById('marcSalvosBody');
        if (!tbody) return;
        const term = this.salvosSearchTerm;
        const list = !term
            ? this.calculosSalvos
            : this.calculosSalvos.filter(c => (c.nome || '').toLowerCase().includes(term));

        if (list.length === 0) {
            tbody.innerHTML = `<tr class="marc-empty-row"><td colspan="6">${
                this.calculosSalvos.length === 0
                    ? 'Nenhum cálculo salvo ainda. Faça um cálculo e clique em <strong>Salvar cálculo</strong>.'
                    : 'Nenhum cálculo encontrado para o filtro atual.'
            }</td></tr>`;
            return;
        }

        const modoLabel = (m) => m === 'APROVEITAMENTO' ? 'Aproveitamento' : 'Por m²';
        tbody.innerHTML = list.map(c => `
            <tr>
                <td>${this.esc(c.nome)}</td>
                <td>${modoLabel(c.modo)}</td>
                <td>${this.formatDateTime(c.dataAtualizacao || c.dataCriacao)}</td>
                <td class="marc-num">${this.formatCurrency(c.custoProducao)}</td>
                <td class="marc-num"><strong>${this.formatCurrency(c.precoVenda)}</strong></td>
                <td class="marc-actions-cell">
                    <button class="marc-action-btn" data-action="reopen" data-id="${c.id}" title="Reabrir">
                        <i class="fas fa-folder-open"></i>
                    </button>
                    <button class="marc-action-btn danger" data-action="delete" data-id="${c.id}" title="Excluir">
                        <i class="fas fa-trash"></i>
                    </button>
                </td>
            </tr>
        `).join('');
        tbody.querySelectorAll('button[data-action]').forEach(btn => {
            const id = parseInt(btn.dataset.id);
            const action = btn.dataset.action;
            btn.addEventListener('click', () => {
                if (action === 'reopen') this.reabrirCalculo(id);
                if (action === 'delete') this.confirmExcluirCalculo(id);
            });
        });
    }

    async reabrirCalculo(id) {
        try {
            const c = await this.fetchJson(`${API_MARC_CALCULOS}/${id}`);
            // Popula formulário
            document.getElementById('marcCalcNome').value = c.nome || '';
            document.getElementById('marcCalcModo').value = c.modo;
            // Dispara o handler de troca de modo (mostra/esconde lista de peças/áreas)
            document.getElementById('marcCalcModo').dispatchEvent(new Event('change'));

            document.getElementById('marcCalcDias').value = c.diasFuncionario ?? '1';
            document.getElementById('marcCalcDiasAjudante').value =
                (c.diasAjudante != null && Number(c.diasAjudante) > 0) ? c.diasAjudante : '';
            document.getElementById('marcCalcMargemLucro').value = c.margemLucroPctSnapshot ?? '';
            document.getElementById('marcCalcMargem').value = c.margemCorteMm ?? 4;
            document.getElementById('marcCalcRotacao').value = c.permitirRotacao === false ? 'false' : 'true';

            // Limpa e re-popula tabelas
            document.getElementById('marcPecasBody').innerHTML = '';
            document.getElementById('marcAreasBody').innerHTML = '';
            document.getElementById('marcAcessoriosCalcBody').innerHTML = '';

            (c.pecas || []).forEach(p => this.addPecaRow({
                materialId: p.materialId,
                larguraMm: p.larguraMm,
                alturaMm: p.alturaMm,
                quantidade: p.quantidade,
                descricao: p.descricao,
            }));
            (c.areas || []).forEach(a => this.addAreaRow({
                materialId: a.materialId,
                areaM2: a.areaM2,
            }));
            (c.acessorios || []).forEach(a => this.addAcessorioCalcRow({
                acessorioId: a.acessorioId,
                quantidade: a.quantidade,
            }));

            this.editingCalculoId = c.id;
            this.lastResult = null;
            document.getElementById('marcResultContent').classList.add('hidden');
            document.getElementById('marcResultEmpty').classList.remove('hidden');
            document.getElementById('marcResultActions')?.classList.add('hidden');

            this.toast('info', 'fas fa-folder-open',
                `Cálculo <strong>${this.esc(c.nome)}</strong> reaberto. Edite e clique em <strong>Calcular</strong>.`);
            // Vai para a aba Calcular
            document.querySelector('#calcTabMarcenaria .marc-section-tab[data-marc-section="calcular"]').click();
        } catch (e) {
            this.toast('danger', 'fas fa-exclamation-circle', `Erro ao reabrir: ${this.esc(e.message)}`);
        }
    }

    async confirmExcluirCalculo(id) {
        const c = this.calculosSalvos.find(x => x.id === id);
        if (!c) return;
        if (!confirm(`Excluir o cálculo "${c.nome}"? Esta ação faz soft delete (pode ser restaurada no banco).`)) return;
        try {
            await this.fetchJson(`${API_MARC_CALCULOS}/${id}`, { method: 'DELETE' });
            this.toast('success', 'fas fa-check-circle', `Cálculo <strong>${this.esc(c.nome)}</strong> excluído.`);
            if (this.editingCalculoId === id) this.editingCalculoId = null;
            await this.loadCalculosSalvos();
        } catch (e) {
            this.toast('danger', 'fas fa-exclamation-circle', `Erro ao excluir: ${this.esc(e.message)}`);
        }
    }

    formatDateTime(iso) {
        if (!iso) return '—';
        try {
            const d = new Date(iso);
            return d.toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
        } catch { return '—'; }
    }

    // ══ HELPERS ════════════════════════════════════════════════════════

    parseIntOrNull(v) {
        const n = parseInt(v);
        return Number.isFinite(n) ? n : null;
    }

    formatCurrency(value) {
        if (value == null || isNaN(Number(value))) return '—';
        return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
    }

    esc(str) {
        return String(str ?? '')
            .replace(/&/g, '&amp;').replace(/</g, '&lt;')
            .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    toast(type, icon, message, duration = 4000) {
        const container = document.getElementById('toastContainer');
        if (!container) { console.log(`[toast ${type}]`, message); return; }
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
}

// ══════════════════════════════════════════════════════════════════════
// MarcenariaPacker — Empacotamento 2D estilo Guillotine BSSF + rotação
// ══════════════════════════════════════════════════════════════════════
//
// Algoritmo:
//   1. Ordena peças por área decrescente (maior primeiro).
//   2. Para cada peça, escolhe o "free rectangle" da chapa atual que
//      melhor encaixa (best short-side fit). Considera rotação 90°.
//   3. Após posicionar, faz "guillotine split": o retângulo livre é
//      dividido em 2 sub-retângulos (corte horizontal-first).
//   4. Se nenhuma chapa atual cabe → abre nova chapa.
//
// Referência: Jukka Jylänki, "A Thousand Ways to Pack the Bin" (2010).
//
class MarcenariaPacker {
    static pack(items, sheet, options = {}) {
        const allowRotation = options.allowRotation !== false;

        // Ordena: peças maiores primeiro (área desc, depois maior lado desc)
        const sorted = items.slice().sort((a, b) => {
            const aArea = a.w * a.h, bArea = b.w * b.h;
            if (bArea !== aArea) return bArea - aArea;
            return Math.max(b.w, b.h) - Math.max(a.w, a.h);
        });

        const sheets = []; // [{ pieces: [...], freeRects: [...] }]

        for (const it of sorted) {
            // Peça nem cabe na chapa? Pulamos com aviso (mas continuamos).
            const fitsBase   = (it.w <= sheet.w && it.h <= sheet.h);
            const fitsRotate = allowRotation && (it.h <= sheet.w && it.w <= sheet.h);
            if (!fitsBase && !fitsRotate) {
                console.warn('[Packer] peça não cabe na chapa:', it);
                continue;
            }

            let placed = false;
            for (const s of sheets) {
                if (this._tryPlace(s, it, allowRotation)) { placed = true; break; }
            }
            if (!placed) {
                // Nova chapa
                const s = {
                    pieces: [],
                    freeRects: [{ x: 0, y: 0, w: sheet.w, h: sheet.h }],
                };
                sheets.push(s);
                this._tryPlace(s, it, allowRotation);
            }
        }

        return sheets;
    }

    static _tryPlace(sheetState, item, allowRotation) {
        // Acha o melhor free rectangle (BSSF — best short-side fit)
        let best = null;
        let bestScore1 = Infinity, bestScore2 = Infinity;
        let bestRotated = false;
        let bestIdx = -1;

        for (let i = 0; i < sheetState.freeRects.length; i++) {
            const fr = sheetState.freeRects[i];
            // Tenta orientação base
            if (item.w <= fr.w && item.h <= fr.h) {
                const leftoverW = fr.w - item.w;
                const leftoverH = fr.h - item.h;
                const s1 = Math.min(leftoverW, leftoverH);
                const s2 = Math.max(leftoverW, leftoverH);
                if (s1 < bestScore1 || (s1 === bestScore1 && s2 < bestScore2)) {
                    best = fr; bestScore1 = s1; bestScore2 = s2; bestRotated = false; bestIdx = i;
                }
            }
            // Tenta orientação rotacionada
            if (allowRotation && item.h <= fr.w && item.w <= fr.h) {
                const leftoverW = fr.w - item.h;
                const leftoverH = fr.h - item.w;
                const s1 = Math.min(leftoverW, leftoverH);
                const s2 = Math.max(leftoverW, leftoverH);
                if (s1 < bestScore1 || (s1 === bestScore1 && s2 < bestScore2)) {
                    best = fr; bestScore1 = s1; bestScore2 = s2; bestRotated = true; bestIdx = i;
                }
            }
        }

        if (!best) return false;

        const placedW = bestRotated ? item.h : item.w;
        const placedH = bestRotated ? item.w : item.h;

        // Posiciona a peça
        sheetState.pieces.push({
            id: item.id,
            x: best.x, y: best.y,
            w: placedW, h: placedH,
            rotated: bestRotated,
            descricao: item.descricao,
        });

        // Guillotine split do free rectangle (horizontal-first):
        //   após colocar a peça no canto inferior-esquerdo do free rect,
        //   sobram 2 sub-retângulos: o "à direita" e o "acima".
        sheetState.freeRects.splice(bestIdx, 1);
        const rightW = best.w - placedW;
        const aboveH = best.h - placedH;
        if (rightW > 0) {
            // Faixa à direita ocupando toda a altura do free rect (split horizontal: prioriza separar primeiro horizontalmente)
            sheetState.freeRects.push({ x: best.x + placedW, y: best.y, w: rightW, h: best.h });
        }
        if (aboveH > 0) {
            // Faixa acima cobrindo apenas a largura ocupada pela peça
            sheetState.freeRects.push({ x: best.x, y: best.y + placedH, w: placedW, h: aboveH });
        }

        // Limpeza: remove free rects degenerados ou contidos em outros
        sheetState.freeRects = sheetState.freeRects.filter(r => r.w > 0 && r.h > 0);
        sheetState.freeRects = MarcenariaPacker._pruneContainedRects(sheetState.freeRects);

        return true;
    }

    static _pruneContainedRects(rects) {
        const out = [];
        for (let i = 0; i < rects.length; i++) {
            let contained = false;
            for (let j = 0; j < rects.length; j++) {
                if (i === j) continue;
                if (MarcenariaPacker._isContained(rects[i], rects[j])) { contained = true; break; }
            }
            if (!contained) out.push(rects[i]);
        }
        return out;
    }

    static _isContained(inner, outer) {
        return inner.x >= outer.x && inner.y >= outer.y &&
               inner.x + inner.w <= outer.x + outer.w &&
               inner.y + inner.h <= outer.y + outer.h;
    }
}

// ══════════════════════════════════════════════════════════════════════
document.addEventListener('DOMContentLoaded', () => {
    try {
        window.marcenariaModule = new MarcenariaModule();
        console.log('[Marcenaria] Module created successfully');
    } catch (e) {
        console.error('[Marcenaria] CONSTRUCTOR FAILED:', e);
    }
});
