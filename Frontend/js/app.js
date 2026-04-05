// 
// APP.JS - Inicialização da aplicação
// 

const API_BASE_URL = 'http://localhost:8081/api';

class App {
    constructor() {
        this.init();
    }

    init() {
        console.log('🚀 CRM Versatilis iniciando...');
        this.setupEventListeners();
        this.setupUserMenu();
        this.setupResponsive();
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