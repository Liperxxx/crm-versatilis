// 
// NOTIFICATIONS.JS — Gerenciador de Notificações do CRM
// Estrutura pronta para integração futura com backend de notificações.
// Hoje gera notificações locais baseadas nos dados do dashboard.
// 

class NotificationsManager {
    constructor() {
        this.notifications = [];
        this.isOpen = false;
        this.init();
    }

    init() {
        this.wrapper  = document.getElementById('notifWrapper');
        this.btn      = document.getElementById('btnNotif');
        this.dropdown = document.getElementById('notifDropdown');
        this.badge    = document.getElementById('notifBadge');
        this.list     = document.getElementById('notifList');
        this.empty    = document.getElementById('notifEmpty');
        this.markAll  = document.getElementById('notifMarkAll');
        if (!this.wrapper) return;

        this.setupEvents();
        this.loadNotifications();
    }

    setupEvents() {
        // Toggle dropdown
        this.btn.addEventListener('click', (e) => {
            e.stopPropagation();
            this.toggle();
        });

        // Fechar ao clicar fora
        document.addEventListener('click', (e) => {
            if (this.isOpen && !this.wrapper.contains(e.target)) {
                this.close();
            }
        });

        // Fechar com Escape
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && this.isOpen) {
                this.close();
                this.btn.focus();
            }
        });

        // Marcar todas como lidas
        this.markAll.addEventListener('click', (e) => {
            e.stopPropagation();
            this.markAllRead();
        });
    }

    toggle() {
        this.isOpen ? this.close() : this.open();
    }

    open() {
        this.isOpen = true;
        this.wrapper.classList.add('open');
        this.btn.setAttribute('aria-expanded', 'true');
    }

    close() {
        this.isOpen = false;
        this.wrapper.classList.remove('open');
        this.btn.setAttribute('aria-expanded', 'false');
    }

    // ── Geração de notificações baseadas nos dados do sistema ──
    async loadNotifications() {
        const token = this.getToken();
        if (!token) return;

        try {
            const resp = await fetch(`${API_BASE_URL}/dashboard/resumo`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (!resp.ok) return;
            const json = await resp.json();
            const d = json.dados;
            if (!d) return;

            this.notifications = [];
            const now = new Date();

            // Tarefas pendentes
            if (d.totalTarefasPendentes > 0) {
                this.notifications.push({
                    id: 'tarefas-pendentes',
                    icon: 'fas fa-tasks',
                    iconClass: 'notif-icon-warning',
                    title: `${d.totalTarefasPendentes} tarefa(s) pendente(s)`,
                    desc: 'Há tarefas aguardando sua ação.',
                    time: now,
                    read: false
                });
            }

            // Oportunidades abertas
            if (d.totalOportunidadesAbertas > 0) {
                this.notifications.push({
                    id: 'oportunidades-abertas',
                    icon: 'fas fa-handshake',
                    iconClass: 'notif-icon-info',
                    title: `${d.totalOportunidadesAbertas} oportunidade(s) aberta(s)`,
                    desc: `Valor total: R$ ${Number(d.valorOportunidadesAbertas || 0).toLocaleString('pt-BR', { minimumFractionDigits: 2 })}`,
                    time: now,
                    read: false
                });
            }

            // Leads recentes
            if (d.leadsRecentes && d.leadsRecentes.length > 0) {
                const lead = d.leadsRecentes[0];
                this.notifications.push({
                    id: 'lead-recente',
                    icon: 'fas fa-star',
                    iconClass: 'notif-icon-success',
                    title: 'Novo lead cadastrado',
                    desc: lead.nome || 'Lead sem nome',
                    time: lead.dataCriacao ? new Date(lead.dataCriacao) : now,
                    read: false
                });
            }

            // Clientes recentes
            if (d.clientesRecentes && d.clientesRecentes.length > 0) {
                const c = d.clientesRecentes[0];
                this.notifications.push({
                    id: 'cliente-recente',
                    icon: 'fas fa-user-plus',
                    iconClass: 'notif-icon-info',
                    title: 'Cliente cadastrado recentemente',
                    desc: c.nome || 'Cliente',
                    time: c.dataCriacao ? new Date(c.dataCriacao) : now,
                    read: false
                });
            }

            // Orçamentos
            if (d.totalOrcamentos > 0 && d.valorOrcamentos > 0) {
                this.notifications.push({
                    id: 'orcamentos',
                    icon: 'fas fa-file-invoice-dollar',
                    iconClass: 'notif-icon-success',
                    title: `${d.totalOrcamentos} orçamento(s) registrado(s)`,
                    desc: `Valor total: R$ ${Number(d.valorOrcamentos).toLocaleString('pt-BR', { minimumFractionDigits: 2 })}`,
                    time: now,
                    read: true
                });
            }

            // Restaurar leituras do localStorage
            const readIds = JSON.parse(localStorage.getItem('crm_notif_read') || '[]');
            this.notifications.forEach(n => {
                if (readIds.includes(n.id)) n.read = true;
            });

            this.render();
        } catch (e) {
            console.warn('Notificações: falha ao carregar', e);
        }
    }

    render() {
        if (this.notifications.length === 0) {
            this.list.classList.add('hidden');
            this.empty.classList.remove('hidden');
            this.badge.classList.add('hidden');
            return;
        }

        this.empty.classList.add('hidden');
        this.list.classList.remove('hidden');

        const unreadCount = this.notifications.filter(n => !n.read).length;
        if (unreadCount > 0) {
            this.badge.textContent = unreadCount > 9 ? '9+' : unreadCount;
            this.badge.classList.remove('hidden');
        } else {
            this.badge.classList.add('hidden');
        }

        this.list.innerHTML = this.notifications.map(n => `
            <div class="notif-item ${n.read ? '' : 'unread'}" data-notif-id="${this.esc(n.id)}">
                <div class="notif-item-icon ${n.iconClass}">
                    <i class="${n.icon}"></i>
                </div>
                <div class="notif-item-content">
                    <p class="notif-item-title">${this.esc(n.title)}</p>
                    <p class="notif-item-desc">${this.esc(n.desc)}</p>
                    <span class="notif-item-time">${this.timeAgo(n.time)}</span>
                </div>
                <span class="notif-item-dot"></span>
            </div>
        `).join('');

        // Click para marcar como lida
        this.list.querySelectorAll('.notif-item').forEach(el => {
            el.addEventListener('click', () => {
                const id = el.dataset.notifId;
                this.markRead(id);
            });
        });
    }

    markRead(id) {
        const n = this.notifications.find(n => n.id === id);
        if (n && !n.read) {
            n.read = true;
            this.persistRead();
            this.render();
        }
    }

    markAllRead() {
        this.notifications.forEach(n => n.read = true);
        this.persistRead();
        this.render();
    }

    persistRead() {
        const readIds = this.notifications.filter(n => n.read).map(n => n.id);
        localStorage.setItem('crm_notif_read', JSON.stringify(readIds));
    }

    timeAgo(date) {
        if (!date) return '';
        const now = new Date();
        const diff = Math.floor((now - new Date(date)) / 1000);
        if (diff < 60)    return 'agora mesmo';
        if (diff < 3600)  return `há ${Math.floor(diff / 60)} min`;
        if (diff < 86400) return `há ${Math.floor(diff / 3600)}h`;
        return `há ${Math.floor(diff / 86400)} dia(s)`;
    }

    getToken() {
        return localStorage.getItem('crm_token')
            || localStorage.getItem('token')
            || localStorage.getItem('jwtToken')
            || null;
    }

    esc(str) {
        if (!str) return '';
        const d = document.createElement('div');
        d.textContent = str;
        return d.innerHTML;
    }
}

// Inicializar
document.addEventListener('DOMContentLoaded', () => {
    window.notificationsManager = new NotificationsManager();
});
