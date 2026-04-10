//
// APP.JS — Inicialização da aplicação CRM Versatilis
//

// ─── URL base da API ────────────────────────────────────────────────────────
const API_BASE_URL = window.location.hostname === 'localhost'
    ? 'http://localhost:8081/api'
    : 'https://crm-versatilis-production.up.railway.app/api';

// ─── Helpers de sessão ──────────────────────────────────────────────────────

/**
 * Retorna o JWT armazenado no localStorage (qualquer chave conhecida).
 */
function getAuthToken() {
    return localStorage.getItem('crm_token')
        || localStorage.getItem('token')
        || localStorage.getItem('jwtToken')
        || null;
}

/**
 * Limpa todos os dados de sessão e redireciona para login.
 * Evita loop se já estiver na página de login.
 */
function logout(force = false) {
    const onLogin = window.location.pathname.includes('login');
    if (onLogin && !force) return;

    localStorage.removeItem('crm_token');
    localStorage.removeItem('token');
    localStorage.removeItem('jwtToken');
    localStorage.removeItem('crm_user');
    localStorage.removeItem('crm_logo');

    window.location.href = 'login.html';
}

// Expõe globalmente para os módulos
window.logout = logout;

// ─── apiFetch — wrapper centralizado de fetch ───────────────────────────────
/**
 * Wrapper de fetch que:
 *  - injeta automaticamente o header Authorization com o JWT
 *  - intercepta respostas 401/403 e redireciona para login (sem toast de erro)
 *  - permite distinguir erro de autenticação de erro real de backend
 *
 * Uso:  const res = await apiFetch(url, { method, body, headers, ... })
 * Lança AuthError (instanceof AuthError === true) em caso de 401/403.
 */
class AuthError extends Error {
    constructor(status) {
        super(`Sessão inválida (HTTP ${status})`);
        this.status = status;
        this.isAuthError = true;
    }
}

window.AuthError = AuthError;

// Flag para evitar múltiplos redirecionamentos simultâneos
let _redirectingToLogin = false;

async function apiFetch(url, options = {}) {
    const token = getAuthToken();

    // Mescla headers: Authorization + Content-Type padrão
    const headers = Object.assign(
        token ? { 'Authorization': `Bearer ${token}` } : {},
        options.headers || {}
    );

    // Se não há Content-Type explícito e não é FormData → usa JSON
    if (!headers['Content-Type'] && !(options.body instanceof FormData)) {
        headers['Content-Type'] = 'application/json';
    }

    const response = await fetch(url, { ...options, headers });

    // Intercepta sessão inválida
    if (response.status === 401 || response.status === 403) {
        if (!_redirectingToLogin && !window.location.pathname.includes('login')) {
            _redirectingToLogin = true;
            console.warn(`[apiFetch] Sessão expirada (${response.status}). Redirecionando...`);
            logout(true);
        }
        throw new AuthError(response.status);
    }

    return response;
}

window.apiFetch = apiFetch;

// ─── App principal ───────────────────────────────────────────────────────────
class App {
    constructor() {
        this.bootstrap();
    }

    async bootstrap() {
        console.log('🚀 CRM Versatilis iniciando...');
        window.appReady = false;
        window.appSessionValid = false;

        this.setupEventListeners();
        this.setupUserMenu();
        this.setupResponsive();
        this.loadCachedLogo();

        // 1. Verificar token — se ausente, redirecionar para login
        const token = getAuthToken();
        if (!token) {
            if (!window.location.pathname.includes('login')) {
                window.location.href = 'login.html';
            }
            return;
        }

        // 2. Validar sessão no backend
        let sessionValid = false;
        try {
            const resp = await fetch(`${API_BASE_URL}/usuarios/me`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (resp.ok) {
                const json = await resp.json();
                const d = json.dados;
                if (d) {
                    sessionValid = true;
                    this.updateUserUI(d);
                }
            } else if (resp.status === 401 || resp.status === 403) {
                // Token expirado → redirecionar
                logout(true);
                return;
            }
        } catch (e) {
            // Backend offline — continua sem forçar logout
            console.warn('[App] Erro ao validar sessão (backend offline?):', e);
        }

        // 3. Sinalizar app pronto
        window.appReady = true;
        window.appSessionValid = sessionValid;
        window.dispatchEvent(new Event('app:ready'));
    }

    updateUserUI(d) {
        const sidebarName = document.querySelector('.sidebar-footer .user-name');
        if (sidebarName) sidebarName.textContent = d.nome || 'Usuário';
        const sidebarRole = document.querySelector('.sidebar-footer .user-role');
        if (sidebarRole) sidebarRole.textContent = d.cargo || d.papel || 'Colaborador';
        const headerSpan = document.querySelector('#btnUserMenu span');
        if (headerSpan) headerSpan.textContent = (d.nome || 'Usuário').split(' ')[0];

        if (d.avatarUrl) {
            ['sidebar', 'header'].forEach(prefix => {
                const icon = document.querySelector(`.${prefix}-avatar-icon`);
                const img  = document.querySelector(`.${prefix}-avatar-img`);
                if (icon && img) {
                    icon.style.display = 'none';
                    img.src = d.avatarUrl;
                    img.style.display = 'block';
                }
            });
        }
    }

    loadCachedLogo() {
        const logo = localStorage.getItem('crm_logo');
        if (!logo) return;
        const sidebarLogo = document.getElementById('sidebarLogo');
        const sidebarIcon = document.querySelector('.sidebar-header .logo i');
        if (sidebarLogo) {
            sidebarLogo.src = logo;
            sidebarLogo.style.display = 'block';
            if (sidebarIcon) sidebarIcon.style.display = 'none';
        }
    }

    setupEventListeners() {
        const sidebarToggle = document.getElementById('sidebarToggle');
        if (sidebarToggle) sidebarToggle.addEventListener('click', () => this.toggleSidebar());

        const menuMobile = document.getElementById('menuMobile');
        if (menuMobile) menuMobile.addEventListener('click', () => this.toggleMobileSidebar());

        const btnLogout = document.querySelector('.btn-logout');
        if (btnLogout) btnLogout.addEventListener('click', () => this.confirmLogout());

        document.querySelectorAll('.nav-link').forEach(link => {
            link.addEventListener('click', () => {
                if (window.innerWidth <= 768) this.closeMobileSidebar();
            });
        });
    }

    toggleSidebar() {
        document.querySelector('.sidebar')?.classList.toggle('collapsed');
    }

    toggleMobileSidebar() {
        document.querySelector('.sidebar')?.classList.toggle('active');
    }

    closeMobileSidebar() {
        document.querySelector('.sidebar')?.classList.remove('active');
    }

    confirmLogout() {
        if (confirm('Tem certeza que deseja sair?')) {
            console.log('👋 Saindo do sistema...');
            logout(true);
        }
    }

    // Alias para compatibilidade
    logout() { this.confirmLogout(); }

    setupUserMenu() {
        const userMenu = document.getElementById('userMenu');
        const btnUser  = document.getElementById('btnUserMenu');
        if (!userMenu || !btnUser) return;

        btnUser.addEventListener('click', (e) => {
            e.stopPropagation();
            const isOpen = userMenu.classList.toggle('open');
            btnUser.setAttribute('aria-expanded', isOpen);
        });

        document.addEventListener('click', (e) => {
            if (!userMenu.contains(e.target)) {
                userMenu.classList.remove('open');
                btnUser.setAttribute('aria-expanded', 'false');
            }
        });

        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                userMenu.classList.remove('open');
                btnUser.setAttribute('aria-expanded', 'false');
                btnUser.focus();
            }
        });

        userMenu.querySelectorAll('[data-user-action]').forEach(item => {
            item.addEventListener('click', (e) => {
                e.preventDefault();
                const action = item.dataset.userAction;
                userMenu.classList.remove('open');
                btnUser.setAttribute('aria-expanded', 'false');

                if (action === 'sair') {
                    this.confirmLogout();
                } else if (action === 'perfil') {
                    window.navigationManager?.navigateTo('perfil');
                } else if (action === 'configuracoes') {
                    window.navigationManager?.navigateTo('configuracoes');
                }
            });
        });
    }

    setupResponsive() {
        window.addEventListener('resize', () => {
            if (window.innerWidth > 768) this.closeMobileSidebar();
        });
    }
}

// Inicializar quando o DOM estiver pronto
document.addEventListener('DOMContentLoaded', () => {
    window.app = new App();
});
