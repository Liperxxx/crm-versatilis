// 
// CONFIGURACOES.JS — Gerenciador do Módulo de Configurações
// Salva preferências no localStorage. Pronto para integração futura com backend.
// 

class ConfiguracoesManager {
    constructor() {
        this.STORAGE_KEY = 'crm_config';
        this.defaults = {
            empresa: { nome: 'Versatilis', cnpj: '', email: '', telefone: '', endereco: '' },
            tema: 'light',
            sidebarCompacta: false,
            densidadeTabelas: 'comfortable',
            notifSistema: true,
            notifTarefas: true,
            notifLeads: true,
            notifOrcamentos: true,
            sessaoTempo: '480',
            senhaForte: true,
            formatoData: 'dd/MM/yyyy',
            moeda: 'BRL',
            itensPagina: '25'
        };
        this.config = {};
        this.init();
    }

    init() {
        this.configNav = document.getElementById('configNav');
        if (!this.configNav) return;

        this.loadConfig();
        this.fillForm();
        this.setupNavigation();
        this.setupEvents();
        this.setupHeaderConfigBtn();
        this.setupLogoUpload();
        this.loadLogo();
    }

    // ── Carregar / Salvar ──

    loadConfig() {
        try {
            const saved = JSON.parse(localStorage.getItem(this.STORAGE_KEY) || '{}');
            this.config = { ...this.defaults, ...saved };
            // Merge nested empresa
            this.config.empresa = { ...this.defaults.empresa, ...(saved.empresa || {}) };
        } catch {
            this.config = { ...this.defaults };
        }
    }

    saveConfig() {
        localStorage.setItem(this.STORAGE_KEY, JSON.stringify(this.config));
    }

    // ── Navegação de seções ──

    setupNavigation() {
        this.configNav.querySelectorAll('.config-nav-item').forEach(item => {
            item.addEventListener('click', (e) => {
                e.preventDefault();
                const section = item.dataset.configSection;
                this.showSection(section);
            });
        });
    }

    showSection(section) {
        // Nav items
        this.configNav.querySelectorAll('.config-nav-item').forEach(i => i.classList.remove('active'));
        const navItem = this.configNav.querySelector(`[data-config-section="${section}"]`);
        if (navItem) navItem.classList.add('active');

        // Sections
        document.querySelectorAll('#configuracoes-module .config-section').forEach(s => s.classList.remove('active'));
        const sectionMap = {
            empresa: 'configEmpresa',
            aparencia: 'configAparencia',
            notificacoes: 'configNotificacoes',
            seguranca: 'configSeguranca',
            integracoes: 'configIntegracoes',
            preferencias: 'configPreferencias'
        };
        const target = document.getElementById(sectionMap[section]);
        if (target) target.classList.add('active');
    }

    // ── Preenchimento do form ──

    fillForm() {
        const el = (id) => document.getElementById(id);
        const c = this.config;

        // Empresa
        if (el('cfgEmpresaNome'))    el('cfgEmpresaNome').value    = c.empresa.nome;
        if (el('cfgEmpresaCnpj'))    el('cfgEmpresaCnpj').value    = c.empresa.cnpj;
        if (el('cfgEmpresaEmail'))   el('cfgEmpresaEmail').value   = c.empresa.email;
        if (el('cfgEmpresaTel'))     el('cfgEmpresaTel').value     = c.empresa.telefone;
        if (el('cfgEmpresaEndereco')) el('cfgEmpresaEndereco').value = c.empresa.endereco;

        // Aparência
        if (el('cfgTema'))             el('cfgTema').value             = c.tema;
        if (el('cfgSidebarCompacta'))  el('cfgSidebarCompacta').checked = c.sidebarCompacta;
        if (el('cfgDensidade'))        el('cfgDensidade').value        = c.densidadeTabelas;

        // Notificações
        if (el('cfgNotifSistema'))    el('cfgNotifSistema').checked    = c.notifSistema;
        if (el('cfgNotifTarefas'))    el('cfgNotifTarefas').checked    = c.notifTarefas;
        if (el('cfgNotifLeads'))      el('cfgNotifLeads').checked      = c.notifLeads;
        if (el('cfgNotifOrcamentos')) el('cfgNotifOrcamentos').checked = c.notifOrcamentos;

        // Segurança
        if (el('cfgSessaoTempo')) el('cfgSessaoTempo').value = c.sessaoTempo;
        if (el('cfgSenhaForte'))  el('cfgSenhaForte').checked  = c.senhaForte;

        // Preferências
        if (el('cfgFormatoData'))  el('cfgFormatoData').value  = c.formatoData;
        if (el('cfgMoeda'))        el('cfgMoeda').value        = c.moeda;
        if (el('cfgItensPagina'))  el('cfgItensPagina').value  = c.itensPagina;
    }

    // ── Eventos ──

    setupEvents() {
        // Empresa Save
        const empresaSave = document.getElementById('configEmpresaSave');
        if (empresaSave) {
            empresaSave.addEventListener('click', () => {
                this.config.empresa = {
                    nome:     document.getElementById('cfgEmpresaNome')?.value || '',
                    cnpj:     document.getElementById('cfgEmpresaCnpj')?.value || '',
                    email:    document.getElementById('cfgEmpresaEmail')?.value || '',
                    telefone: document.getElementById('cfgEmpresaTel')?.value || '',
                    endereco: document.getElementById('cfgEmpresaEndereco')?.value || ''
                };
                this.saveConfig();
                this.toast('success', 'fas fa-check-circle', 'Dados da empresa salvos!');
            });
        }

        // Aparência — auto-save on change
        this.autoSave('cfgTema', 'tema', 'select');
        this.autoSave('cfgSidebarCompacta', 'sidebarCompacta', 'checkbox');
        this.autoSave('cfgDensidade', 'densidadeTabelas', 'select');

        // Notificações — auto-save
        this.autoSave('cfgNotifSistema', 'notifSistema', 'checkbox');
        this.autoSave('cfgNotifTarefas', 'notifTarefas', 'checkbox');
        this.autoSave('cfgNotifLeads', 'notifLeads', 'checkbox');
        this.autoSave('cfgNotifOrcamentos', 'notifOrcamentos', 'checkbox');

        // Segurança
        this.autoSave('cfgSessaoTempo', 'sessaoTempo', 'select');
        this.autoSave('cfgSenhaForte', 'senhaForte', 'checkbox');

        // Preferências
        this.autoSave('cfgFormatoData', 'formatoData', 'select');
        this.autoSave('cfgMoeda', 'moeda', 'select');
        this.autoSave('cfgItensPagina', 'itensPagina', 'select');
    }

    autoSave(elementId, configKey, type) {
        const el = document.getElementById(elementId);
        if (!el) return;
        el.addEventListener('change', () => {
            this.config[configKey] = type === 'checkbox' ? el.checked : el.value;
            this.saveConfig();
            this.toast('success', 'fas fa-check-circle', 'Preferência salva!');
        });
    }

    setupHeaderConfigBtn() {
        const btn = document.getElementById('btnHeaderConfig');
        if (btn) {
            btn.addEventListener('click', () => {
                if (window.navigationManager) {
                    window.navigationManager.navigateTo('configuracoes');
                }
            });
        }
    }

    // ── Logo Upload ──

    setupLogoUpload() {
        const btn = document.getElementById('cfgLogoBtn');
        const input = document.getElementById('cfgLogoInput');
        if (btn && input) {
            btn.addEventListener('click', () => input.click());
            input.addEventListener('change', () => this.uploadLogo());
        }
    }

    async loadLogo() {
        // Try localStorage cache first
        const cached = localStorage.getItem('crm_logo_url');
        if (cached) this.showLogo(cached);

        try {
            const resp = await apiFetch(`${API_BASE_URL}/config/logo`);
            if (!resp.ok) return;

            const json = await resp.json();
            const url = json.dados?.logoUrl;
            if (url) {
                localStorage.setItem('crm_logo_url', url);
                this.showLogo(url);
            }
        } catch (e) {
            if (e.isAuthError) return;
            console.warn('Erro ao carregar logo:', e);
        }
    }

    showLogo(url) {
        if (!url) return;

        // Config preview
        const img = document.getElementById('cfgLogoImg');
        const placeholder = document.getElementById('cfgLogoPlaceholder');
        if (img) {
            img.src = url;
            img.style.display = 'block';
            if (placeholder) placeholder.style.display = 'none';
        }

        // Sidebar logo
        const sidebarLogo = document.getElementById('sidebarLogo');
        const sidebarIcon = document.getElementById('sidebarLogoIcon');
        if (sidebarLogo) {
            sidebarLogo.src = url;
            sidebarLogo.style.display = 'block';
            if (sidebarIcon) sidebarIcon.style.display = 'none';
        }
    }

    async uploadLogo() {
        const input = document.getElementById('cfgLogoInput');
        const file = input?.files?.[0];
        if (!file) return;

        if (file.type !== 'image/png') {
            this.toast('danger', 'fas fa-exclamation-circle', 'Apenas imagens PNG são aceitas.');
            input.value = '';
            return;
        }
        if (file.size > 5 * 1024 * 1024) {
            this.toast('danger', 'fas fa-exclamation-circle', 'O arquivo deve ter no máximo 5MB.');
            input.value = '';
            return;
        }

        // Preview
        const reader = new FileReader();
        reader.onload = (e) => this.showLogo(e.target.result);
        reader.readAsDataURL(file);

        const btn = document.getElementById('cfgLogoBtn');
        if (btn) {
            btn.disabled = true;
            btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Enviando...';
        }

        try {
            const formData = new FormData();
            formData.append('file', file);

            const resp = await apiFetch(`${API_BASE_URL}/config/logo`, {
                method: 'POST',
                body: formData
            });

            const json = await resp.json().catch(() => ({}));
            if (!resp.ok) throw new Error(json.mensagem || 'Erro no upload');

            const url = json.dados?.logoUrl;
            if (url) {
                localStorage.setItem('crm_logo_url', url);
                this.showLogo(url);
            }
            this.toast('success', 'fas fa-check-circle', 'Logo atualizado com sucesso!');
        } catch (e) {
            if (e.isAuthError) return;
            this.toast('danger', 'fas fa-exclamation-circle', e.message);
        } finally {
            if (btn) {
                btn.disabled = false;
                btn.innerHTML = '<i class="fas fa-upload"></i> Enviar Logo';
            }
            input.value = '';
        }
    }

    // ── Toast ──

    toast(type, icon, message, duration = 3000) {
        const container = document.getElementById('toastContainer');
        if (!container) return;
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
}

// Inicializar
document.addEventListener('DOMContentLoaded', () => {
    window.configuracoesManager = new ConfiguracoesManager();
});
