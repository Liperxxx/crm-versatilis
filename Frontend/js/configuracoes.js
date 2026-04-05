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
