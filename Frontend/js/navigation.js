// 
// NAVIGATION.JS - Gerenciador de navegação entre módulos
// 

class NavigationManager {
    constructor() {
        this.currentModule = 'dashboard';
        this.init();
    }

    init() {
        console.log('📍 Gerenciador de Navegação inicializando...');
        this.setupEventListeners();
    }

    setupEventListeners() {
        // Cliques nos links de navegação
        document.querySelectorAll('.nav-link').forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                const module = link.dataset.module;
                this.navigateTo(module);
            });
        });

        // Navegação por hash (URL)
        window.addEventListener('hashchange', () => {
            const hash = window.location.hash.substring(1);
            if (hash) {
                this.navigateTo(hash);
            }
        });
    }

    navigateTo(module) {
        // Validar módulo
        const validModules = [
            'dashboard',
            'clientes',
            'cliente-form',
            'leads',
            'produtos',
            'funil',
            'orcamentos',
            'orcamento-form',
            'tarefas',
            'relatorios',
            'perfil',
            'configuracoes'
        ];

        if (!validModules.includes(module)) {
            console.warn(`⚠️ Módulo inválido: ${module}`);
            return;
        }

        // Atualizar URL
        window.location.hash = module;

        // Remover classe active de todos os módulos
        document.querySelectorAll('.module').forEach(mod => {
            mod.classList.remove('active');
        });

        // Remover classe active de todos os links
        document.querySelectorAll('.nav-link').forEach(link => {
            link.classList.remove('active');
        });

        // Ativar módulo selecionado
        const moduleElement = document.getElementById(`${module}-module`);
        if (moduleElement) {
            moduleElement.classList.add('active');
        }

        // Ativar link do sidebar — sub-rotas herdam o link do pai
        const navModule = module === 'orcamento-form' ? 'orcamentos'
                        : module === 'cliente-form'   ? 'clientes'
                        : module;
        const activeLink = document.querySelector(`[data-module="${navModule}"]`);
        if (activeLink) {
            activeLink.classList.add('active');
        }

        // Atualizar breadcrumb
        this.updateBreadcrumb(module);

        // Atualizar módulo atual
        this.currentModule = module;

        console.log(`✅ Navegando para: ${module}`);
    }

    updateBreadcrumb(module) {
        const breadcrumbModule = document.getElementById('breadcrumbModule');
        if (breadcrumbModule) {
            const labels = {
                'dashboard':     'Dashboard',
                'clientes':      'Clientes',
                'leads':         'Leads',
                'produtos':      'Produtos',
                'funil':         'Oportunidades',
                'orcamentos':    'Orçamentos',
                'tarefas':       'Tarefas',
                'relatorios':    'Relatórios',
                'perfil':        'Meu Perfil',
                'configuracoes': 'Configurações'
            };
            if (module === 'orcamento-form') {
                breadcrumbModule.innerHTML =
                    `<span class="breadcrumb-link" style="cursor:pointer" onclick="window.navigationManager.navigateTo('orcamentos')">Orçamentos</span>` +
                    `<i class="fas fa-chevron-right" style="font-size:10px;margin:0 6px;opacity:.5"></i>` +
                    `<span id="orcFormBreadcrumbSub">Orçamento</span>`;
            } else if (module === 'cliente-form') {
                breadcrumbModule.innerHTML =
                    `<span class="breadcrumb-link" style="cursor:pointer" onclick="window.navigationManager.navigateTo('clientes')">Clientes</span>` +
                    `<i class="fas fa-chevron-right" style="font-size:10px;margin:0 6px;opacity:.5"></i>` +
                    `<span id="cliFormBreadcrumbSub">Cliente</span>`;
            } else {
                breadcrumbModule.textContent = labels[module] || module;
            }
        }
    }

    getCurrentModule() {
        return this.currentModule;
    }
}

// Inicializar gerenciador de navegação
document.addEventListener('DOMContentLoaded', () => {
    window.navigationManager = new NavigationManager();
    
    // Navegar para módulo da URL se houver
    const hash = window.location.hash.substring(1);
    if (hash) {
        window.navigationManager.navigateTo(hash);
    }
});