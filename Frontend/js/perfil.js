// 
// PERFIL.JS — Gerenciador do Módulo Meu Perfil
// 

class PerfilManager {
    constructor() {
        this.editing = false;
        this.originalData = {};
        this._pendingFile = null;          // arquivo aguardando confirmação no tab avatar
        this._pendingPreviewUrl = null;    // data-URL do preview antes de enviar
        this.init();
    }

    init() {
        this.loading     = document.getElementById('perfilLoading');
        this.content     = document.getElementById('perfilContent');
        this.form        = document.getElementById('perfilForm');
        this.senhaForm   = document.getElementById('perfilSenhaForm');
        this.editBtn     = document.getElementById('perfilEditBtn');
        this.cancelBtn   = document.getElementById('perfilCancelBtn');
        this.saveBtn     = document.getElementById('perfilSaveBtn');
        this.formActions = document.getElementById('perfilFormActions');
        this.avatarInput = document.getElementById('perfilAvatarInput');
        if (!this.form) return;

        this.setupEvents();
        this.setupPasswordToggles();
        this.setupAvatarUpload();
    }

    setupEvents() {
        this.editBtn.addEventListener('click', () => this.enableEdit());
        this.cancelBtn.addEventListener('click', () => this.cancelEdit());

        this.form.addEventListener('submit', (e) => {
            e.preventDefault();
            this.savePerfil();
        });

        this.senhaForm.addEventListener('submit', (e) => {
            e.preventDefault();
            this.alterarSenha();
        });

        // ── Tab switching ──────────────────────────────────────────
        document.querySelectorAll('#perfil-module .perfil-tab-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                const tab = btn.dataset.tab;
                // activate button
                document.querySelectorAll('#perfil-module .perfil-tab-btn').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                // activate panel
                document.querySelectorAll('#perfil-module .perfil-tab-panel').forEach(p => p.classList.remove('active'));
                const panel = document.getElementById(`perfilTab${tab.charAt(0).toUpperCase() + tab.slice(1)}`);
                if (panel) panel.classList.add('active');
                // sync avatar preview when opening avatar tab
                if (tab === 'avatar') this.syncAvatarTabPreview();
            });
        });

        // ── Observar navegação (MutationObserver) ─────────────────
        const observer = new MutationObserver(() => {
            const mod = document.getElementById('perfil-module');
            if (mod && mod.classList.contains('active')) {
                this.loadPerfil();
            }
        });
        const perfilMod = document.getElementById('perfil-module');
        if (perfilMod) {
            observer.observe(perfilMod, { attributes: true, attributeFilter: ['class'] });
        }

        // ── Backup: carregar se já estiver ativo ao ficar app:ready ─
        window.addEventListener('app:ready', () => {
            const mod = document.getElementById('perfil-module');
            if (mod && mod.classList.contains('active')) {
                this.loadPerfil();
            }
        });
    }

    setupPasswordToggles() {
        document.querySelectorAll('#perfilSenhaForm .input-password-toggle').forEach(btn => {
            btn.addEventListener('click', () => {
                const input = btn.previousElementSibling;
                const icon = btn.querySelector('i');
                if (input.type === 'password') {
                    input.type = 'text';
                    icon.classList.replace('fa-eye', 'fa-eye-slash');
                } else {
                    input.type = 'password';
                    icon.classList.replace('fa-eye-slash', 'fa-eye');
                }
            });
        });
    }

    setupAvatarUpload() {
        // ── Overlay rápido no card lateral ─────────────────────────
        const avatar = document.getElementById('perfilAvatar');
        if (avatar && this.avatarInput) {
            avatar.addEventListener('click', () => this.avatarInput.click());
            this.avatarInput.addEventListener('change', () => this.uploadAvatar(this.avatarInput));
        }

        // ── Aba Foto de Perfil ──────────────────────────────────────
        const dropzone   = document.getElementById('avatarDropzone');
        const tabInput   = document.getElementById('avatarTabInput');
        const selectBtn  = document.getElementById('avatarSelectBtn');
        const confirmBtn = document.getElementById('avatarConfirmBtn');
        const cancelBtn  = document.getElementById('avatarCancelUploadBtn');

        if (!dropzone || !tabInput) return;

        // Abrir file picker
        selectBtn?.addEventListener('click', () => tabInput.click());
        dropzone.addEventListener('click', (e) => {
            if (e.target !== selectBtn && !selectBtn?.contains(e.target)) tabInput.click();
        });

        // Drag & drop
        dropzone.addEventListener('dragover', (e) => {
            e.preventDefault();
            dropzone.classList.add('drag-over');
        });
        dropzone.addEventListener('dragleave', () => dropzone.classList.remove('drag-over'));
        dropzone.addEventListener('drop', (e) => {
            e.preventDefault();
            dropzone.classList.remove('drag-over');
            const file = e.dataTransfer?.files?.[0];
            if (file) this.previewAvatarTabFile(file);
        });

        // File picker selecionou arquivo
        tabInput.addEventListener('change', () => {
            const file = tabInput.files?.[0];
            if (file) this.previewAvatarTabFile(file);
            tabInput.value = '';
        });

        // Confirmar upload
        confirmBtn?.addEventListener('click', () => {
            if (this._pendingFile) this.uploadAvatar(null, this._pendingFile);
        });

        // Cancelar preview
        cancelBtn?.addEventListener('click', () => this.cancelAvatarPreview());
    }

    // Valida e faz preview do arquivo na aba
    previewAvatarTabFile(file) {
        if (!['image/png', 'image/jpeg'].includes(file.type)) {
            this.toast('danger', 'fas fa-exclamation-circle', 'Apenas imagens PNG e JPEG são aceitas.');
            return;
        }
        if (file.size > 5 * 1024 * 1024) {
            this.toast('danger', 'fas fa-exclamation-circle', 'A imagem deve ter no máximo 5MB.');
            return;
        }

        this._pendingFile = file;

        const reader = new FileReader();
        reader.onload = (e) => {
            this._pendingPreviewUrl = e.target.result;

            // Atualiza preview na aba
            const img     = document.getElementById('avatarTabImg');
            const initial = document.getElementById('avatarTabInitial');
            if (img) { img.src = e.target.result; img.style.display = 'block'; }
            if (initial) initial.style.display = 'none';

            // Mostra barra de ações
            const actions = document.getElementById('avatarUploadActions');
            if (actions) actions.style.display = 'flex';
        };
        reader.readAsDataURL(file);
    }

    cancelAvatarPreview() {
        this._pendingFile       = null;
        this._pendingPreviewUrl = null;
        // Restaura preview com dado original
        this.syncAvatarTabPreview();
        const actions = document.getElementById('avatarUploadActions');
        if (actions) actions.style.display = 'none';
    }

    // Sincroniza a aba de avatar com os dados já carregados (originalData)
    syncAvatarTabPreview() {
        const img     = document.getElementById('avatarTabImg');
        const initial = document.getElementById('avatarTabInitial');
        if (!img || !initial) return;

        const d = this.originalData;
        const letter = ((d.nome || '') + '?').charAt(0).toUpperCase();
        initial.textContent = letter;

        if (d.avatarUrl) {
            img.src = d.avatarUrl;
            img.style.display = 'block';
            initial.style.display = 'none';
        } else {
            img.style.display = 'none';
            initial.style.display = '';
        }
    }

    // Upload de avatar — aceita um input element (overlay) ou um File direto (aba)
    async uploadAvatar(inputEl, fileArg) {
        const file = fileArg || inputEl?.files?.[0];
        if (!file) return;

        if (!['image/png', 'image/jpeg'].includes(file.type)) {
            this.toast('danger', 'fas fa-exclamation-circle', 'Apenas imagens PNG e JPEG são aceitas.');
            return;
        }
        if (file.size > 5 * 1024 * 1024) {
            this.toast('danger', 'fas fa-exclamation-circle', 'A imagem deve ter no máximo 5MB.');
            return;
        }

        // Preview imediato no card lateral (overlay rápido)
        if (inputEl) {
            const reader = new FileReader();
            reader.onload = (e) => {
                const img = document.getElementById('perfilAvatarImg');
                if (img) { img.src = e.target.result; img.style.display = 'block'; }
                const ini = document.getElementById('perfilAvatarInitial');
                if (ini) ini.style.display = 'none';
            };
            reader.readAsDataURL(file);
        }

        this.toast('info', 'fas fa-spinner fa-spin', 'Enviando foto...', 10000);

        try {
            const formData = new FormData();
            formData.append('file', file);

            const resp = await apiFetch(`${API_BASE_URL}/usuarios/me/avatar`, {
                method: 'POST',
                body: formData
            });

            const json = await resp.json().catch(() => ({}));
            if (!resp.ok) throw new Error(json.mensagem || 'Erro no upload');

            const d = json.dados;
            this.originalData = { ...d };
            this.fillProfile(d);

            // Limpa estado pendente da aba
            this._pendingFile       = null;
            this._pendingPreviewUrl = null;
            const actions = document.getElementById('avatarUploadActions');
            if (actions) actions.style.display = 'none';

            this.toast('success', 'fas fa-check-circle', 'Foto de perfil atualizada!');
        } catch (e) {
            if (e.isAuthError) return;
            this.toast('danger', 'fas fa-exclamation-circle', e.message);
            // Reverte previews
            this.syncAvatarTabPreview();
            const img = document.getElementById('perfilAvatarImg');
            if (img && !this.originalData.avatarUrl) {
                img.style.display = 'none';
                const ini = document.getElementById('perfilAvatarInitial');
                if (ini) ini.style.display = '';
            }
        } finally {
            if (inputEl) inputEl.value = '';
            if (this.avatarInput) this.avatarInput.value = '';
        }
    }

    async loadPerfil() {
        this.loading.style.display = 'flex';
        this.content.style.display = 'none';

        try {
            const resp = await apiFetch(`${API_BASE_URL}/usuarios/me`);

            if (!resp.ok) throw new Error('Falha ao carregar perfil');
            const json = await resp.json();
            const d = json.dados;
            if (!d) throw new Error('Dados inválidos');

            this.originalData = { ...d };
            this.fillProfile(d);

            this.loading.style.display = 'none';
            this.content.style.display = 'block';
        } catch (e) {
            this.loading.style.display = 'none';
            this.content.style.display = 'block';
            if (e.isAuthError) return;
            console.error('Erro ao carregar perfil:', e);
            this.toast('danger', 'fas fa-exclamation-circle', 'Erro ao carregar perfil: ' + e.message);
        }
    }

    fillProfile(d) {
        const initial = (d.nome || '?').charAt(0).toUpperCase();
        const el = (id) => document.getElementById(id);

        // Card lateral
        const ini = el('perfilAvatarInitial');
        if (ini) ini.textContent = initial;
        const nomeEl = el('perfilNomeDisplay');
        if (nomeEl) nomeEl.textContent = d.nome || '—';
        const papelEl = el('perfilPapelBadge');
        if (papelEl) papelEl.textContent = this.papelLabel(d.papel);
        const emailEl = el('perfilEmailDisplay');
        if (emailEl) emailEl.textContent = d.email || '—';
        const telEl = el('perfilTelDisplay');
        if (telEl) telEl.textContent = d.telefone || 'Não informado';
        const cargoEl = el('perfilCargoDisplay');
        if (cargoEl) cargoEl.textContent = d.cargo || 'Não informado';
        const desdeEl = el('perfilDesdeDisplay');
        if (desdeEl) desdeEl.textContent = d.dataCriacao ? this.formatDate(d.dataCriacao) : '—';
        const acessoEl = el('perfilUltimoAcessoDisplay');
        if (acessoEl) acessoEl.textContent = d.ultimoAcesso ? this.formatDate(d.ultimoAcesso) : '—';

        // Avatar no card lateral
        const avatarImg = el('perfilAvatarImg');
        const avatarIni = el('perfilAvatarInitial');
        if (d.avatarUrl) {
            if (avatarImg) { avatarImg.src = d.avatarUrl; avatarImg.style.display = 'block'; }
            if (avatarIni) avatarIni.style.display = 'none';
        } else {
            if (avatarImg) avatarImg.style.display = 'none';
            if (avatarIni) avatarIni.style.display = '';
        }

        // Campos do formulário
        const fnome = el('perfilNome');
        if (fnome) fnome.value = d.nome || '';
        const femail = el('perfilEmail');
        if (femail) femail.value = d.email || '';
        const fcargo = el('perfilCargo');
        if (fcargo) fcargo.value = d.cargo || '';
        const ftel = el('perfilTelefone');
        if (ftel) ftel.value = d.telefone || '';

        // Sincroniza preview da aba de avatar
        this.syncAvatarTabPreview();

        // Atualiza sidebar e header
        this.updateUIWithUserData(d);
    }

    updateUIWithUserData(d) {
        const sidebarName = document.querySelector('.sidebar-footer .user-name');
        if (sidebarName) sidebarName.textContent = d.nome || 'Usuário';

        const sidebarRole = document.querySelector('.sidebar-footer .user-role');
        if (sidebarRole) sidebarRole.textContent = d.cargo || this.papelLabel(d.papel);

        const headerUserSpan = document.querySelector('#btnUserMenu span');
        if (headerUserSpan) headerUserSpan.textContent = (d.nome || 'Usuário').split(' ')[0];

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

    enableEdit() {
        this.editing = true;
        this.form.classList.remove('perfil-form-disabled');
        document.getElementById('perfilNome').disabled = false;
        document.getElementById('perfilCargo').disabled = false;
        document.getElementById('perfilTelefone').disabled = false;
        this.formActions.classList.remove('hidden');
        this.editBtn.classList.add('hidden');
        document.getElementById('perfilNome').focus();
    }

    cancelEdit() {
        this.editing = false;
        this.form.classList.add('perfil-form-disabled');
        document.getElementById('perfilNome').disabled = true;
        document.getElementById('perfilCargo').disabled = true;
        document.getElementById('perfilTelefone').disabled = true;
        this.formActions.classList.add('hidden');
        this.editBtn.classList.remove('hidden');

        document.getElementById('perfilNome').value     = this.originalData.nome || '';
        document.getElementById('perfilCargo').value    = this.originalData.cargo || '';
        document.getElementById('perfilTelefone').value = this.originalData.telefone || '';
    }

    async savePerfil() {
        const nome     = document.getElementById('perfilNome').value.trim();
        const cargo    = document.getElementById('perfilCargo').value.trim();
        const telefone = document.getElementById('perfilTelefone').value.trim();

        if (!nome) {
            this.toast('danger', 'fas fa-exclamation-circle', 'Nome é obrigatório.');
            return;
        }

        this.saveBtn.disabled = true;
        this.saveBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Salvando...';

        try {
            const resp = await apiFetch(`${API_BASE_URL}/usuarios/me`, {
                method: 'PUT',
                body: JSON.stringify({ nome, cargo, telefone })
            });

            if (!resp.ok) {
                const err = await resp.json().catch(() => ({}));
                throw new Error(err.mensagem || 'Erro ao salvar perfil');
            }

            const json = await resp.json();
            const d = json.dados;
            this.originalData = { ...d };
            this.fillProfile(d);
            this.cancelEdit();
            this.toast('success', 'fas fa-check-circle', 'Perfil atualizado com sucesso!');
        } catch (e) {
            if (e.isAuthError) return;
            this.toast('danger', 'fas fa-exclamation-circle', e.message);
        } finally {
            this.saveBtn.disabled = false;
            this.saveBtn.innerHTML = '<i class="fas fa-save"></i> Salvar';
        }
    }

    async alterarSenha() {
        const senhaAtual     = document.getElementById('perfilSenhaAtual').value;
        const novaSenha      = document.getElementById('perfilNovaSenha').value;
        const confirmarSenha = document.getElementById('perfilConfirmarSenha').value;

        if (!senhaAtual || !novaSenha) {
            this.toast('danger', 'fas fa-exclamation-circle', 'Preencha todos os campos de senha.');
            return;
        }
        if (novaSenha.length < 6) {
            this.toast('danger', 'fas fa-exclamation-circle', 'Nova senha deve ter no mínimo 6 caracteres.');
            return;
        }
        if (novaSenha !== confirmarSenha) {
            this.toast('danger', 'fas fa-exclamation-circle', 'As senhas não conferem.');
            return;
        }

        const btn = this.senhaForm.querySelector('button[type="submit"]');
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Alterando...';

        try {
            const resp = await apiFetch(`${API_BASE_URL}/usuarios/me/senha`, {
                method: 'PUT',
                body: JSON.stringify({ senhaAtual, novaSenha })
            });

            const json = await resp.json().catch(() => ({}));
            if (!resp.ok) throw new Error(json.mensagem || 'Erro ao alterar senha');

            this.senhaForm.reset();
            this.toast('success', 'fas fa-check-circle', 'Senha alterada com sucesso!');
        } catch (e) {
            if (e.isAuthError) return;
            this.toast('danger', 'fas fa-exclamation-circle', e.message);
        } finally {
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-key"></i> Alterar Senha';
        }
    }

    // ── Helpers ──

    papelLabel(papel) {
        const map = { ADMIN: 'Administrador', GERENTE: 'Gerente', OPERADOR: 'Operador' };
        return map[papel] || papel || '—';
    }

    formatDate(dateStr) {
        try {
            const d = new Date(dateStr);
            return d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
        } catch {
            return dateStr;
        }
    }

    toast(type, icon, message, duration = 4000) {
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
    window.perfilManager = new PerfilManager();
});
