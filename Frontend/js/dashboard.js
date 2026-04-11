// 
// DASHBOARD.JS — Integração com GET /api/dashboard/resumo
// 

const API_DASHBOARD = `${API_BASE_URL}/dashboard`;

class DashboardManager {
    constructor() {
        this.data = null;
        this.chartEtapa = null;
        this.chartStatus = null;
        this.bindElements();
        this.bindEvents();
        this.load();
    }

    // ══ ELEMENTOS ═════════════════════════════════════════════════════════

    bindElements() {
        this.$loading = document.getElementById('dashLoading');
        this.$error   = document.getElementById('dashError');
        this.$errorMsg = document.getElementById('dashErrorMsg');
        this.$content = document.getElementById('dashContent');
        this.$retry   = document.getElementById('dashRetry');
    }

    bindEvents() {
        if (this.$retry) {
            this.$retry.addEventListener('click', () => this.load());
        }
    }

    // ══ TOKEN ═════════════════════════════════════════════════════════════

    getToken() {
        return localStorage.getItem('crm_token')
            || localStorage.getItem('token')
            || localStorage.getItem('jwtToken')
            || null;
    }

    authHeaders() {
        const token = this.getToken();
        return token
            ? { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' }
            : { 'Content-Type': 'application/json' };
    }

    // ══ ESTADOS VISUAIS ══════════════════════════════════════════════════

    showLoading() {
        if (this.$loading) this.$loading.style.display = 'flex';
        if (this.$error)   this.$error.style.display = 'none';
        if (this.$content) this.$content.style.display = 'none';
    }

    showError(msg) {
        if (this.$loading) this.$loading.style.display = 'none';
        if (this.$error)   this.$error.style.display = 'flex';
        if (this.$content) this.$content.style.display = 'none';
        if (this.$errorMsg) this.$errorMsg.textContent = msg || 'Erro ao carregar dados.';
    }

    showContent() {
        if (this.$loading) this.$loading.style.display = 'none';
        if (this.$error)   this.$error.style.display = 'none';
        if (this.$content) this.$content.style.display = 'block';
    }

    // ══ CARREGAMENTO ═════════════════════════════════════════════════════

    async load() {
        this.showLoading();
        try {
            const res = await fetch(`${API_DASHBOARD}/resumo`, {
                headers: this.authHeaders()
            });
            if (res.status === 401 || res.status === 403) {
                this.showError('Sessão expirada. Faça login novamente.');
                return;
            }
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const json = await res.json();
            this.data = json.dados || json;
            this.render();
            this.showContent();
        } catch (e) {
            console.error('[Dashboard] Erro ao carregar:', e);
            this.showError(`Não foi possível conectar ao backend: ${e.message}`);
        }
    }

    // ══ RENDER ═══════════════════════════════════════════════════════════

    render() {
        const d = this.data;
        if (!d) return;
        this.renderKpis(d);
        this.renderChartEtapa(d.oportunidadesPorEtapa);
        this.renderChartStatus(d.orcamentosPorStatus);
        this.renderClientesRecentes(d.clientesRecentes);
        this.renderLeadsRecentes(d.leadsRecentes);
        this.renderTarefasPendentes(d.tarefasPendentes);
    }

    // ── KPIs ─────────────────────────────────────────────────────────────

    renderKpis(d) {
        this.setText('kpiValorOportunidades', this.currency(d.valorOportunidadesAbertas));
        this.setText('kpiTotalOportunidades',
            `${d.totalOportunidadesAbertas || 0} oportunidade${d.totalOportunidadesAbertas !== 1 ? 's' : ''} aberta${d.totalOportunidadesAbertas !== 1 ? 's' : ''}`);

        this.setText('kpiTotalClientes', d.totalClientes || 0);
        this.setText('kpiTotalProdutos', `${d.totalProdutos || 0} produto${d.totalProdutos !== 1 ? 's' : ''} cadastrado${d.totalProdutos !== 1 ? 's' : ''}`);

        this.setText('kpiTotalLeads', d.totalLeads || 0);
        this.setText('kpiTarefasPendentes',
            `${d.totalTarefasPendentes || 0} tarefa${d.totalTarefasPendentes !== 1 ? 's' : ''} pendente${d.totalTarefasPendentes !== 1 ? 's' : ''}`);

        this.setText('kpiTotalOrcamentos', d.totalOrcamentos || 0);
        this.setText('kpiValorOrcamentos', `Total: ${this.currency(d.valorOrcamentos)}`);
    }

    // ── Gráfico: Oportunidades por Etapa (bar) ──────────────────────────

    renderChartEtapa(mapa) {
        const canvas = document.getElementById('chartOportunidadesEtapa');
        if (!canvas || !mapa) return;

        if (this.chartEtapa) this.chartEtapa.destroy();

        const labels = Object.keys(mapa).map(k => this.formatLabel(k));
        const values = Object.values(mapa);
        const colors = ['#CD5A26', '#ef4444', '#f59e0b', '#10b981', '#306EB4'];

        this.chartEtapa = new Chart(canvas, {
            type: 'bar',
            data: {
                labels,
                datasets: [{
                    label: 'Oportunidades',
                    data: values,
                    backgroundColor: colors.slice(0, labels.length),
                    borderRadius: 6,
                    maxBarThickness: 50
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    y: { beginAtZero: true, ticks: { stepSize: 1 } }
                }
            }
        });
    }

    // ── Gráfico: Orçamentos por Status (doughnut) ───────────────────────

    renderChartStatus(mapa) {
        const canvas = document.getElementById('chartOrcamentosStatus');
        if (!canvas || !mapa) return;

        if (this.chartStatus) this.chartStatus.destroy();

        const keys   = Object.keys(mapa);
        const values = Object.values(mapa);
        const total  = values.reduce((a, b) => a + b, 0);

        // Labels com porcentagem: "Aprovado (35%)"
        const labels = keys.map((k, i) => {
            const pct = total > 0 ? ((values[i] / total) * 100).toFixed(1) : '0.0';
            return `${this.formatLabel(k)} (${pct}%)`;
        });

        const colors = ['#94a3b8', '#306EB4', '#10b981', '#ef4444'];

        this.chartStatus = new Chart(canvas, {
            type: 'doughnut',
            data: {
                labels,
                datasets: [{
                    data: values,
                    backgroundColor: colors.slice(0, labels.length),
                    borderWidth: 2,
                    borderColor: '#fff'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { position: 'bottom', labels: { padding: 16, font: { size: 12 } } },
                    tooltip: {
                        callbacks: {
                            label: (ctx) => {
                                const val = ctx.parsed || 0;
                                const pct = total > 0 ? ((val / total) * 100).toFixed(1) : '0.0';
                                return ` ${ctx.label.split(' (')[0]}: ${val} (${pct}%)`;
                            }
                        }
                    }
                }
            }
        });

        // ── Taxa de Conversão Comercial ──
        this.renderTaxaConversao(mapa, total);
    }

    renderTaxaConversao(mapa, total) {
        const container = document.getElementById('conversaoComercial');
        const valorEl   = document.getElementById('taxaConversaoValor');
        const detalheEl = document.getElementById('conversaoDetalhe');
        if (!container || !valorEl) return;

        // Normalizar chaves para uppercase
        const norm = {};
        Object.keys(mapa).forEach(k => { norm[k.toUpperCase()] = mapa[k] || 0; });

        const aprovados = norm['APROVADO'] || 0;
        const enviados  = norm['ENVIADO']  || 0;
        const recusados = norm['RECUSADO'] || 0;

        // Base de cálculo: orçamentos que passaram por decisão (enviados + aprovados + recusados)
        const base = aprovados + enviados + recusados;
        const taxa = base > 0 ? ((aprovados / base) * 100).toFixed(1) : '0.0';

        valorEl.textContent = `${taxa}%`;

        // Cor condicional
        const num = parseFloat(taxa);
        if (num >= 50)      valorEl.style.color = '#10b981'; // verde
        else if (num >= 25) valorEl.style.color = '#f59e0b'; // amarelo
        else                valorEl.style.color = '#ef4444'; // vermelho

        if (detalheEl) {
            detalheEl.textContent = `${aprovados} aprovado(s) de ${base} proposta(s) em negociação — Rascunhos não contabilizados`;
        }

        container.style.display = 'block';
    }

    // ── Clientes Recentes ───────────────────────────────────────────────

    renderClientesRecentes(lista) {
        const container = document.getElementById('dashClientesRecentes');
        if (!container) return;

        if (!lista || lista.length === 0) {
            container.innerHTML = '<p class="text-muted text-sm">Nenhum cliente encontrado.</p>';
            return;
        }

        container.innerHTML = lista.map(c => `
            <div class="activity-item">
                <div class="activity-icon" style="background: var(--color-primary);">
                    <i class="fas fa-building"></i>
                </div>
                <div class="activity-content">
                    <p class="activity-title">${this.esc(c.nome)}</p>
                    <p class="activity-description"><span class="badge badge-${this.statusBadge(c.status)}">${this.esc(c.status || '—')}</span></p>
                    <p class="activity-time">${this.timeAgo(c.dataCriacao)}</p>
                </div>
            </div>
        `).join('');
    }

    // ── Leads Recentes ──────────────────────────────────────────────────

    renderLeadsRecentes(lista) {
        const container = document.getElementById('dashLeadsRecentes');
        if (!container) return;

        if (!lista || lista.length === 0) {
            container.innerHTML = '<p class="text-muted text-sm">Nenhum lead encontrado.</p>';
            return;
        }

        container.innerHTML = lista.map(l => `
            <div class="activity-item">
                <div class="activity-icon" style="background: var(--color-info);">
                    <i class="fas fa-user"></i>
                </div>
                <div class="activity-content">
                    <p class="activity-title">${this.esc(l.nome)}</p>
                    <p class="activity-description"><span class="badge badge-${this.statusBadge(l.status)}">${this.esc(l.status || '—')}</span></p>
                    <p class="activity-time">${this.timeAgo(l.dataCriacao)}</p>
                </div>
            </div>
        `).join('');
    }

    // ── Tarefas Pendentes ───────────────────────────────────────────────

    renderTarefasPendentes(lista) {
        const container = document.getElementById('dashTarefasPendentes');
        if (!container) return;

        if (!lista || lista.length === 0) {
            container.innerHTML = '<p class="text-muted text-sm">Nenhuma tarefa pendente.</p>';
            return;
        }

        container.innerHTML = lista.map(t => {
            const venc = t.dataVencimento ? this.formatDate(t.dataVencimento) : '—';
            const prioClass = this.prioridadeClass(t.prioridade);
            return `
                <div class="activity-item">
                    <div class="activity-icon" style="background: var(${prioClass});">
                        <i class="fas fa-clipboard-check"></i>
                    </div>
                    <div class="activity-content">
                        <p class="activity-title">${this.esc(t.titulo)}</p>
                        <p class="activity-description">${this.esc(t.vinculo || '—')} · Vence: ${venc}</p>
                        <p class="activity-time"><span class="badge badge-${this.prioridadeBadge(t.prioridade)}">${this.esc(t.prioridade || '—')}</span></p>
                    </div>
                </div>
            `;
        }).join('');
    }

    // ══ HELPERS ══════════════════════════════════════════════════════════

    setText(id, value) {
        const el = document.getElementById(id);
        if (el) el.textContent = value;
    }

    esc(str) {
        if (!str) return '';
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }

    currency(value) {
        const num = parseFloat(value) || 0;
        return num.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
    }

    formatLabel(key) {
        return (key || '')
            .replace(/_/g, ' ')
            .toLowerCase()
            .replace(/^\w/, c => c.toUpperCase());
    }

    formatDate(str) {
        if (!str) return '—';
        const parts = str.split('-');
        if (parts.length === 3) return `${parts[2]}/${parts[1]}/${parts[0]}`;
        return str;
    }

    timeAgo(dateStr) {
        if (!dateStr) return '';
        const date = new Date(dateStr);
        const now = new Date();
        const diffMs = now - date;
        const diffMin = Math.floor(diffMs / 60000);
        if (diffMin < 1) return 'agora';
        if (diffMin < 60) return `há ${diffMin} min`;
        const diffH = Math.floor(diffMin / 60);
        if (diffH < 24) return `há ${diffH}h`;
        const diffD = Math.floor(diffH / 24);
        if (diffD === 1) return 'há 1 dia';
        if (diffD < 30) return `há ${diffD} dias`;
        return this.formatDate(dateStr.substring(0, 10));
    }

    statusBadge(status) {
        const map = {
            'Ativo': 'success', 'ABERTA': 'success', 'NOVO': 'info', 'QUALIFICADO': 'primary',
            'EM_CONTATO': 'warning', 'PROPOSTA_ENVIADA': 'warning', 'CONVERTIDO': 'success',
            'Inativo': 'secondary', 'PERDIDO': 'danger', 'Prospecto': 'warning'
        };
        return map[status] || 'secondary';
    }

    prioridadeClass(prio) {
        const map = {
            'URGENTE': '--color-danger', 'ALTA': '--color-warning',
            'MEDIA': '--color-info', 'BAIXA': '--color-success'
        };
        return map[prio] || '--color-info';
    }

    prioridadeBadge(prio) {
        const map = { 'URGENTE': 'danger', 'ALTA': 'warning', 'MEDIA': 'info', 'BAIXA': 'success' };
        return map[prio] || 'secondary';
    }
}

// ══ INICIALIZAÇÃO ════════════════════════════════════════════════════════

document.addEventListener('DOMContentLoaded', () => {
    window.dashboardManager = new DashboardManager();
});
