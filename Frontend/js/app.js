// 
// APP.JS - Inicialização da aplicação
// 

const API_BASE_URL = window.location.hostname === 'localhost'
    ? 'http://localhost:8081/api'
    : 'https://crm-versatilis-production.up.railway.app/api';

// ── Interceptor global: redireciona para login ao receber 401 ──────────
(function() {
    const _fetch = window.fetch;
    let redirecting = false;
    let consecutiveFailures = 0;
    const MAX_FAILURES_BEFORE_REDIRECT = 3;

    window.fetch = function(...args) {
        return _fetch.apply(this, args).then(function(response) {
            if (response.ok) {
                consecutiveFailures = 0;
            }

            if (response.status === 401 && !redirecting) {
                const input = args[0];
                const url = typeof input === 'string' ? input : (input && input.url ? input.url : '');
                // Não redirecionar em chamadas de login/registro/validação
                if (url.indexOf('/auth/') === -1) {
                    consecutiveFailures++;
                    // Só redireciona depois de múltiplas falhas consecutivas
                    // para evitar redirect em erros transitórios
                    if (consecutiveFailures >= MAX_FAILURES_BEFORE_REDIRECT) {
                        redirecting = true;
                        localStorage.removeItem('crm_token');
                        window.location.replace('login.html');
                    }
                }
            }

            // Não redirecionar em erros 500/503 - são transitórios
            // O usuário deve poder tentar novamente
            return response;
        }).catch(function(error) {
            // Erros de rede não devem causar redirect
            return Promise.reject(error);
        });
    };
})();

class App {
    constructor() {
        this.init();
    }

    init() {
        console.log('🚀 CRM Versatilis iniciando...');
        this.setupEventListeners();
        this.setupUserMenu();
        this.setupResponsive();
        this.loadCachedLogo();
        this.loadUserProfile();
    }

    loadCachedLogo() {
        const logoUrl = localStorage.getItem('crm_logo_url');
        if (logoUrl) {
            const sidebarLogo = document.getElementById('sidebarLogo');
            const sidebarIcon = document.getElementById('sidebarLogoIcon');
            if (sidebarLogo) {
                sidebarLogo.src = logoUrl;
                sidebarLogo.style.display = 'block';
                if (sidebarIcon) sidebarIcon.style.display = 'none';
            }
        }
    }

    async loadUserProfile() {
        const token = localStorage.getItem('crm_token') || localStorage.getItem('token');
        if (!token) return;

        try {
            const resp = await fetch(`${API_BASE_URL}/usuarios/me`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (!resp.ok) return;
            const json = await resp.json();
            const d = json.dados;
            if (!d) return;

            // Update header and sidebar names
            const sidebarName = document.querySelector('.sidebar-footer .user-name');
            if (sidebarName) sidebarName.textContent = d.nome || 'Usuário';
            const sidebarRole = document.querySelector('.sidebar-footer .user-role');
            if (sidebarRole) sidebarRole.textContent = d.cargo || d.papel || 'Colaborador';
            const headerSpan = document.querySelector('#btnUserMenu span');
            if (headerSpan) headerSpan.textContent = (d.nome || 'Usuário').split(' ')[0];

            // Update avatar if available
            if (d.avatarUrl) {
                const sidebarIcon = document.querySelector('.sidebar-avatar-icon');
                const sidebarImg = document.querySelector('.sidebar-avatar-img');
                if (sidebarIcon && sidebarImg) {
                    sidebarIcon.style.display = 'none';
                    sidebarImg.src = d.avatarUrl;
                    sidebarImg.style.display = 'block';
                }
                const headerIcon = document.querySelector('.header-avatar-icon');
                const headerImg = document.querySelector('.header-avatar-img');
                if (headerIcon && headerImg) {
                    headerIcon.style.display = 'none';
                    headerImg.src = d.avatarUrl;
                    headerImg.style.display = 'block';
                }
            }
        } catch (e) {
            console.warn('Erro ao carregar perfil do usuário:', e);
        }
    }

    setupEventListeners() {
        // Sidebar toggle
        const sidebarToggle = document.getElementById('sidebarToggle');
        if (sidebarToggle) {
            sidebarToggle.addEventListener('click', () => {
                this.toggleSidebar();
            });
        }

        // Menu mobile
        const menuMobile = document.getElementById('menuMobile');
        if (menuMobile) {
            menuMobile.addEventListener('click', () => {
                this.toggleMobileSidebar();
            });
        }

        // Logout
        const btnLogout = document.querySelector('.btn-logout');
        if (btnLogout) {
            btnLogout.addEventListener('click', () => {
                this.logout();
            });
        }

        // Fechar sidebar ao clicar em um link (mobile)
        document.querySelectorAll('.nav-link').forEach(link => {
            link.addEventListener('click', () => {
                if (window.innerWidth <= 768) {
                    this.closeMobileSidebar();
                }
            });
        });
    }

    toggleSidebar() {
        const sidebar = document.querySelector('.sidebar');
        sidebar.classList.toggle('collapsed');
    }

    toggleMobileSidebar() {
        const sidebar = document.querySelector('.sidebar');
        sidebar.classList.toggle('active');
    }

    closeMobileSidebar() {
        const sidebar = document.querySelector('.sidebar');
        sidebar.classList.remove('active');
    }

    logout() {
        if (confirm('Tem certeza que deseja sair?')) {
            console.log('👋 Saindo do sistema...');
            localStorage.removeItem('crm_token');
            localStorage.removeItem('token');
            window.location.href = 'login.html';
        }
    }

    setupUserMenu() {
        const userMenu = document.getElementById('userMenu');
        const btnUser  = document.getElementById('btnUserMenu');
        if (!userMenu || !btnUser) return;

        // Abrir / fechar ao clicar no botão
        btnUser.addEventListener('click', (e) => {
            e.stopPropagation();
            const isOpen = userMenu.classList.toggle('open');
            btnUser.setAttribute('aria-expanded', isOpen);
        });

        // Fechar ao clicar fora
        document.addEventListener('click', (e) => {
            if (!userMenu.contains(e.target)) {
                userMenu.classList.remove('open');
                btnUser.setAttribute('aria-expanded', 'false');
            }
        });

        // Fechar com Escape
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                userMenu.classList.remove('open');
                btnUser.setAttribute('aria-expanded', 'false');
                btnUser.focus();
            }
        });

        // Ações dos itens do menu
        userMenu.querySelectorAll('[data-user-action]').forEach(item => {
            item.addEventListener('click', (e) => {
                e.preventDefault();
                const action = item.dataset.userAction;
                userMenu.classList.remove('open');
                btnUser.setAttribute('aria-expanded', 'false');

                if (action === 'sair') {
                    this.logout();
                } else if (action === 'perfil') {
                    if (window.navigationManager) {
                        window.navigationManager.navigateTo('perfil');
                    }
                } else if (action === 'configuracoes') {
                    if (window.navigationManager) {
                        window.navigationManager.navigateTo('configuracoes');
                    }
                }
            });
        });
    }

    setupResponsive() {
        window.addEventListener('resize', () => {
            if (window.innerWidth > 768) {
                this.closeMobileSidebar();
            }
        });
    }
}

// Inicializar app quando DOM estiver pronto
document.addEventListener('DOMContentLoaded', () => {
    window.app = new App();
});
