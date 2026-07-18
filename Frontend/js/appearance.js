//
// APPEARANCE.JS — aplica as preferências visuais (Configurações › Aparência).
// Carregado no <head> para aplicar o tema ANTES da primeira pintura (sem flash).
// Lê a mesma chave que configuracoes.js persiste: localStorage 'crm_config'.
//
(function () {
    'use strict';

    const STORAGE_KEY = 'crm_config';

    function readConfig() {
        try { return JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}'); }
        catch { return {}; }
    }

    // 'auto' segue a preferência do sistema operacional.
    function resolveTheme(tema) {
        if (tema === 'dark') return 'dark';
        if (tema === 'auto') {
            return (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches)
                ? 'dark' : 'light';
        }
        return 'light';
    }

    function apply(cfg) {
        cfg = cfg || readConfig();
        const root = document.documentElement;

        if (resolveTheme(cfg.tema || 'light') === 'dark') {
            root.setAttribute('data-theme', 'dark');
        } else {
            root.removeAttribute('data-theme');
        }

        root.classList.toggle('sidebar-compact', !!cfg.sidebarCompacta);
        root.classList.toggle('tables-compact', cfg.densidadeTabelas === 'compact');
    }

    // Aplica imediatamente (roda no <head>, antes do body renderizar).
    apply();

    // Se o tema estiver em "Automático", reage à troca de tema do sistema.
    if (window.matchMedia) {
        window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
            const cfg = readConfig();
            if ((cfg.tema || 'light') === 'auto') apply(cfg);
        });
    }

    // Exposto para configuracoes.js reaplicar ao vivo quando o usuário muda uma opção.
    window.Appearance = { apply, readConfig, resolveTheme };
})();
