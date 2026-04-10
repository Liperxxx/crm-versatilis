//
// RELATORIOS.JS — Módulo de Relatórios Gerenciais
//

const API_RELATORIOS = `${API_BASE_URL}/relatorios`;

class RelatoriosModule {

    constructor() {
        this.dados = null;
        this.charts = {};
        this.bindElements();
        this.bindEvents();
        this.carregarRelatorio();
    }

    // ── DOM bindings ─────────────────────────────────────────────────────
    bindElements() {
        this.$dataInicio = document.getElementById('relDataInicio');
        this.$dataFim    = document.getElementById('relDataFim');
        this.$btnFiltrar = document.getElementById('btnRelFiltrar');
        this.$btnLimpar  = document.getElementById('btnRelLimpar');
        this.$btnPDF     = document.getElementById('btnRelExportarPDF');
        this.$loading    = document.getElementById('relLoading');
        this.$conteudo   = document.getElementById('relConteudo');
    }

    bindEvents() {
        this.$btnFiltrar?.addEventListener('click', () => this.carregarRelatorio());
        this.$btnLimpar?.addEventListener('click', () => this.limparFiltros());
        this.$btnPDF?.addEventListener('click', () => this.exportarPDF());
    }

    // ── API ──────────────────────────────────────────────────────────────
    async carregarRelatorio() {
        this.showLoading(true);
        try {
            const params = new URLSearchParams();
            if (this.$dataInicio?.value) params.append('dataInicio', this.$dataInicio.value);
            if (this.$dataFim?.value)    params.append('dataFim', this.$dataFim.value);

            const qs  = params.toString();
            const url = qs ? `${API_RELATORIOS}?${qs}` : API_RELATORIOS;

            const token = localStorage.getItem('crm_token') || localStorage.getItem('token') || null;
            const headers = token ? { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' }
                                  : { 'Content-Type': 'application/json' };
            const resp  = await fetch(url, { headers });

            if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
            const json = await resp.json();

            if (json.sucesso && json.dados) {
                this.dados = json.dados;
                this.renderAll();
            } else {
                this.showToast('Erro ao carregar relatório', 'danger');
            }
        } catch (err) {
            console.error('Erro relatório:', err);
            this.showToast('Erro de conexão com o servidor', 'danger');
        } finally {
            this.showLoading(false);
        }
    }

    limparFiltros() {
        if (this.$dataInicio) this.$dataInicio.value = '';
        if (this.$dataFim)    this.$dataFim.value = '';
        this.carregarRelatorio();
    }

    // ── Render ───────────────────────────────────────────────────────────
    renderAll() {
        if (!this.dados) return;
        this.renderCards();
        this.renderCharts();
        this.renderTabelas();
    }

    renderCards() {
        const d = this.dados;
        this.setCardValue('relTotalClientes',     d.totalClientes);
        this.setCardValue('relTotalLeads',         d.totalLeads);
        this.setCardValue('relLeadsConvertidos',   d.leadsConvertidos);
        this.setCardValue('relOportAbertas',       d.oportunidadesAbertas);
        this.setCardValue('relOportGanhas',        d.oportunidadesGanhas);
        this.setCardValue('relOportPerdidas',      d.oportunidadesPerdidas);
        this.setCardValue('relTarefasPendentes',   d.tarefasPendentes);
        this.setCardValue('relTarefasConcluidas',  d.tarefasConcluidas);
        this.setCardValue('relTotalOrcamentos',    this.formatCurrency(d.totalOrcamentos || 0));
    }

    setCardValue(id, value) {
        const el = document.getElementById(id);
        if (el) el.textContent = value;
    }

    // ── Charts ───────────────────────────────────────────────────────────
    renderCharts() {
        this.renderLeadsChart();
        this.renderOportChart();
        this.renderOrcChart();
        this.renderTarefasChart();
    }

    destroyChart(key) {
        if (this.charts[key]) {
            this.charts[key].destroy();
            this.charts[key] = null;
        }
    }

    renderLeadsChart() {
        this.destroyChart('leads');
        const ctx = document.getElementById('chartLeadsStatus');
        if (!ctx) return;

        const data = this.dados.leadsPorStatus || {};
        const statusLabels = {
            'NOVO': 'Novo', 'QUALIFICADO': 'Qualificado', 'EM_CONTATO': 'Em Contato',
            'PROPOSTA_ENVIADA': 'Proposta Enviada', 'PERDIDO': 'Perdido', 'CONVERTIDO': 'Convertido'
        };
        const statusColors = {
            'NOVO': '#306EB4', 'QUALIFICADO': '#FF9300', 'EM_CONTATO': '#FAA532',
            'PROPOSTA_ENVIADA': '#CD5A26', 'PERDIDO': '#ef4444', 'CONVERTIDO': '#10b981'
        };

        const labels = Object.keys(data).map(k => statusLabels[k] || k);
        const values = Object.values(data);
        const colors = Object.keys(data).map(k => statusColors[k] || '#94a3b8');

        this.charts.leads = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels,
                datasets: [{ data: values, backgroundColor: colors, borderWidth: 2, borderColor: '#fff' }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { position: 'bottom', labels: { padding: 16, usePointStyle: true, font: { size: 12 } } }
                }
            }
        });
    }

    renderOportChart() {
        this.destroyChart('oport');
        const ctx = document.getElementById('chartOportEtapa');
        if (!ctx) return;

        const data = this.dados.oportunidadesPorEtapa || {};
        const etapaLabels = {
            'QUALIFICACAO': 'Qualificação', 'ANALISE_NECESSIDADES': 'Análise',
            'PROPOSTA': 'Proposta', 'NEGOCIACAO': 'Negociação', 'FECHAMENTO': 'Fechamento'
        };
        const etapaColors = ['#306EB4', '#FF9300', '#CD5A26', '#FAA532', '#10b981'];

        const orderedKeys = ['QUALIFICACAO', 'ANALISE_NECESSIDADES', 'PROPOSTA', 'NEGOCIACAO', 'FECHAMENTO'];
        const labels = orderedKeys.filter(k => data[k] !== undefined).map(k => etapaLabels[k] || k);
        const values = orderedKeys.filter(k => data[k] !== undefined).map(k => data[k]);
        const colors = orderedKeys.filter(k => data[k] !== undefined).map((_, i) => etapaColors[i % etapaColors.length]);

        this.charts.oport = new Chart(ctx, {
            type: 'bar',
            data: {
                labels,
                datasets: [{
                    label: 'Oportunidades',
                    data: values,
                    backgroundColor: colors,
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

    renderOrcChart() {
        this.destroyChart('orc');
        const ctx = document.getElementById('chartOrcStatus');
        if (!ctx) return;

        const data = this.dados.orcamentosPorStatus || {};
        const statusLabels = {
            'RASCUNHO': 'Rascunho', 'ENVIADO': 'Enviado', 'APROVADO': 'Aprovado', 'RECUSADO': 'Recusado'
        };
        const statusColors = { 'RASCUNHO': '#94a3b8', 'ENVIADO': '#306EB4', 'APROVADO': '#10b981', 'RECUSADO': '#ef4444' };

        const orderedKeys = ['RASCUNHO', 'ENVIADO', 'APROVADO', 'RECUSADO'];
        const labels = orderedKeys.filter(k => data[k] !== undefined).map(k => statusLabels[k] || k);
        const values = orderedKeys.filter(k => data[k] !== undefined).map(k => data[k]);
        const colors = orderedKeys.filter(k => data[k] !== undefined).map(k => statusColors[k] || '#94a3b8');

        this.charts.orc = new Chart(ctx, {
            type: 'bar',
            data: {
                labels,
                datasets: [{
                    label: 'Orçamentos',
                    data: values,
                    backgroundColor: colors,
                    borderRadius: 6,
                    maxBarThickness: 50
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                indexAxis: 'y',
                plugins: { legend: { display: false } },
                scales: {
                    x: { beginAtZero: true, ticks: { stepSize: 1 } }
                }
            }
        });
    }

    renderTarefasChart() {
        this.destroyChart('tarefas');
        const ctx = document.getElementById('chartTarefasPrioridade');
        if (!ctx) return;

        const data = this.dados.tarefasPorPrioridade || {};
        const prioLabels = { 'BAIXA': 'Baixa', 'MEDIA': 'Média', 'ALTA': 'Alta', 'URGENTE': 'Urgente' };
        const prioColors = { 'BAIXA': '#10b981', 'MEDIA': '#306EB4', 'ALTA': '#FF9300', 'URGENTE': '#ef4444' };

        const orderedKeys = ['BAIXA', 'MEDIA', 'ALTA', 'URGENTE'];
        const labels = orderedKeys.filter(k => data[k] !== undefined).map(k => prioLabels[k] || k);
        const values = orderedKeys.filter(k => data[k] !== undefined).map(k => data[k]);
        const colors = orderedKeys.filter(k => data[k] !== undefined).map(k => prioColors[k] || '#94a3b8');

        this.charts.tarefas = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels,
                datasets: [{ data: values, backgroundColor: colors, borderWidth: 2, borderColor: '#fff' }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { position: 'bottom', labels: { padding: 16, usePointStyle: true, font: { size: 12 } } }
                }
            }
        });
    }

    // ── Tabelas ──────────────────────────────────────────────────────────
    renderTabelas() {
        this.renderTopOport();
        this.renderTarefasVencidas();
    }

    renderTopOport() {
        const tbody = document.getElementById('relTblTopOport');
        const empty = document.getElementById('relTopOportEmpty');
        if (!tbody) return;

        const list = this.dados.topOportunidades || [];
        if (list.length === 0) {
            tbody.innerHTML = '';
            if (empty) empty.style.display = 'block';
            return;
        }
        if (empty) empty.style.display = 'none';

        tbody.innerHTML = list.map(o => `
            <tr>
                <td><strong>${this.esc(o.titulo)}</strong></td>
                <td>${this.esc(o.clienteNome)}</td>
                <td><span class="badge badge-info">${this.formatEtapa(o.etapa)}</span></td>
                <td style="text-align:right; font-weight:600;">${this.formatCurrency(o.valorEstimado)}</td>
            </tr>
        `).join('');
    }

    renderTarefasVencidas() {
        const tbody = document.getElementById('relTblTarefasVenc');
        const empty = document.getElementById('relTarefasVencEmpty');
        if (!tbody) return;

        const list = this.dados.tarefasVencidas || [];
        if (list.length === 0) {
            tbody.innerHTML = '';
            if (empty) empty.style.display = 'block';
            return;
        }
        if (empty) empty.style.display = 'none';

        tbody.innerHTML = list.map(t => `
            <tr>
                <td><strong>${this.esc(t.titulo)}</strong></td>
                <td>${this.esc(t.responsavelNome)}</td>
                <td><span class="badge badge-danger">${this.formatDate(t.dataVencimento)}</span></td>
                <td><span class="badge ${this.prioridadeBadge(t.prioridade)}">${this.formatPrioridade(t.prioridade)}</span></td>
            </tr>
        `).join('');
    }

    // ── Exportar PDF ─────────────────────────────────────────────────────
    exportarPDF() {
        window.print();
    }

    // ── Helpers ──────────────────────────────────────────────────────────
    showLoading(show) {
        if (this.$loading)  this.$loading.style.display  = show ? 'block' : 'none';
        if (this.$conteudo) this.$conteudo.style.display = show ? 'none'  : 'block';
    }

    formatCurrency(value) {
        const num = typeof value === 'number' ? value : parseFloat(value) || 0;
        return num.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
    }

    formatDate(dateStr) {
        if (!dateStr) return '—';
        const [y, m, d] = dateStr.split('-');
        return `${d}/${m}/${y}`;
    }

    formatEtapa(e) {
        const map = {
            'QUALIFICACAO': 'Qualificação', 'ANALISE_NECESSIDADES': 'Análise',
            'PROPOSTA': 'Proposta', 'NEGOCIACAO': 'Negociação', 'FECHAMENTO': 'Fechamento'
        };
        return map[e] || e;
    }

    formatPrioridade(p) {
        const map = { 'BAIXA': 'Baixa', 'MEDIA': 'Média', 'ALTA': 'Alta', 'URGENTE': 'Urgente' };
        return map[p] || p;
    }

    prioridadeBadge(p) {
        const map = { 'BAIXA': 'badge-success', 'MEDIA': 'badge-info', 'ALTA': 'badge-warning', 'URGENTE': 'badge-danger' };
        return map[p] || 'badge-info';
    }

    esc(str) {
        if (!str) return '—';
        const d = document.createElement('div');
        d.textContent = str;
        return d.innerHTML;
    }

    showToast(msg, type = 'success') {
        const container = document.getElementById('toastContainer');
        if (!container) return;
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        toast.innerHTML = `<i class="fas fa-${type === 'success' ? 'check-circle' : 'exclamation-circle'}"></i><span>${msg}</span>`;
        container.appendChild(toast);
        requestAnimationFrame(() => toast.classList.add('show'));
        setTimeout(() => { toast.classList.remove('show'); setTimeout(() => toast.remove(), 300); }, 3500);
    }
}

// Inicializar quando o DOM estiver pronto
document.addEventListener('DOMContentLoaded', () => {
    window.relatoriosModule = new RelatoriosModule();
});
