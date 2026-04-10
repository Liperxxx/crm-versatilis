// 
// PERFIL.JS — Gerenciador do Módulo Meu Perfil
// 

class PerfilManager {
    constructor() {
        this.editing = false;
        this.originalData = {};
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

        // Observar navegação para carregar dados
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
        const avatar = document.getElementById('perfilAvatar');
        if (avatar && this.avatarInput) {
            avatar.addEventListener('click', () => this.avatarInput.click());
            this.avatarInput.addEventListener('change', () => this.uploadAvatar());
        }
    }

    async uploadAvatar() {
        const file = this.avatarInput?.files?.[0];
        if (!file) return;

        if (!['image/png', 'image/jpeg'].includes(file.type)) {
            this.toast('danger', 'fas fa-exclamation-circle', 'Apenas imagens PNG e JPEG são aceitas.');
            return;
        }
        if (file.size > 5 * 1024 * 1024) {
            this.toast('danger', 'fas fa-exclamation-circle', 'A imagem deve ter no máximo 5MB.');
            return;
        }

        const token = this.getToken();
        if (!token) return;

        // Show preview immediately
        const reader = new FileReader();
        reader.onload = (e) => {
            const img = document.getElementById('perfilAvatarImg');
            if (img) {
                img.src = e.target.result;
                img.style.display = 'block';
                document.getElementById('perfilAvatarInitial').style.display = 'none';
            }
        };
        reader.readAsDataURL(file);

        this.toast('info', 'fas fa-spinner fa-spin', 'Enviando foto...', 10000);

        try {
            const formData = new FormData();
            formData.append('file', file);

            const resp = await fetch(`${API_BASE_URL}/usuarios/me/avatar`, {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${token}` },
                body: formData
            });

            const json = await resp.json().catch(() => ({}));
            if (!resp.ok) throw new Error(json.mensagem || 'Erro no upload');

            const d = json.dados;
            this.originalData = { ...d };
            this.fillProfile(d);
            this.updateUIWithUserData(d);
            this.toast('success', 'fas fa-check-circle', 'Foto de perfil atualizada!');
        } catch (e) {
            this.toast('danger', 'fas fa-exclamation-circle', e.message);
            // Revert preview
            const img = document.getElementById('perfilAvatarImg');
            if (img && !this.originalData.avatarUrl) {
                img.style.display = 'none';
                document.getElementById('perfilAvatarInitial').style.display = '';
            }
        } finally {
            this.avatarInput.value = '';
        }
    }

    async loadPerfil() {
        const token = this.getToken();
        if (!token) return;

        this.loading.style.display = 'flex';
        this.content.style.display = 'none';

        try {
            const resp = await fetch(`${API_BASE_URL}/usuarios/me`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (!resp.ok) throw new Error('Falha ao carregar perfil');
            const json = await resp.json();
            const d = json.dados;
            if (!d) throw new Error('Dados inválidos');

            this.originalData = { ...d };
            this.fillProfile(d);

            this.loading.style.display = 'none';
            this.content.style.display = 'block';
        } catch (e) {
            console.error('Erro ao carregar perfil:', e);
            this.loading.style.display = 'none';
            this.content.style.display = 'block';
            this.toast('danger', 'fas fa-exclamation-circle', 'Erro ao carregar perfil: ' + e.message);
        }
    }

    fillProfile(d) {
        // Sidebar card
        const initial = (d.nome || '?').charAt(0).toUpperCase();
        const el = (id) => document.getElementById(id);

        el('perfilAvatarInitial').textContent = initial;
        el('perfilNomeDisplay').textContent   = d.nome || '—';
        el('perfilPapelBadge').textContent     = this.papelLabel(d.papel);
        el('perfilEmailDisplay').textContent   = d.email || '—';
        el('perfilTelDisplay').textContent      = d.telefone || 'Não informado';
        el('perfilCargoDisplay').textContent    = d.cargo || 'Não informado';
        el('perfilDesdeDisplay').textContent    = d.dataCriacao ? this.formatDate(d.dataCriacao) : '—';
        el('perfilUltimoAcessoDisplay').textContent = d.ultimoAcesso ? this.formatDate(d.ultimoAcesso) : '—';

        // Avatar image
        const avatarImg = el('perfilAvatarImg');
        const avatarInitial = el('perfilAvatarInitial');
        if (d.avatarUrl) {
            avatarImg.src = d.avatarUrl;
            avatarImg.style.display = 'block';
            avatarInitial.style.display = 'none';
        } else {
            avatarImg.style.display = 'none';
            avatarInitial.style.display = '';
        }

        // Avatar image
        const avatarImg = el('perfilAvatarImg');
        const avatarInitial = el('perfilAvatarInitial');
        if (d.avatarUrl) {
            avatarImg.src = d.avatarUrl;
            avatarImg.style.display = 'block';
            avatarInitial.style.display = 'none';
        } else {
            avatarImg.style.display = 'none';
            avatarInitial.style.display = '';
        }

        // Form fields
        el('perfilNome').value     = d.nome || '';
        el('perfilEmail').value    = d.email || '';
        el('perfilCargo').value    = d.cargo || '';
        el('perfilTelefone').value = d.telefone || '';

        // Update sidebar and header with user name
        this.updateUIWithUserData(d);
    }

    updateUIWithUserData(d) {
        // Update sidebar user-profile
        const sidebarName = document.querySelector('.sidebar-footer .user-name');
        if (sidebarName) sidebarName.textContent = d.nome || 'Usuário';

        const sidebarRole = document.querySelector('.sidebar-footer .user-role');
        if (sidebarRole) sidebarRole.textContent = d.cargo || this.papelLabel(d.papel);

        // Update header user button
        const headerUserSpan = document.querySelector('#btnUserMenu span');
        if (headerUserSpan) headerUserSpan.textContent = (d.nome || 'Usuário').split(' ')[0];

        // Update avatar images in sidebar and header
        if (d.avatarUrl) {
            // Sidebar avatar
            const sidebarIcon = document.querySelector('.sidebar-avatar-icon');
            const sidebarImg = document.querySelector('.sidebar-avatar-img');
            if (sidebarIcon && sidebarImg) {
                sidebarIcon.style.display = 'none';
                sidebarImg.src = d.avatarUrl;
                sidebarImg.style.display = 'block';
            }
            // Header avatar
            const headerIcon = document.querySelector('.header-avatar-icon');
            const headerImg = document.querySelector('.header-avatar-img');
            if (headerIcon && headerImg) {
                headerIcon.style.display = 'none';
                headerImg.src = d.avatarUrl;
                headerImg.style.display = 'block';
            }
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

        // Restore original values
        document.getElementById('perfilNome').value     = this.originalData.nome || '';
        document.getElementById('perfilCargo').value    = this.originalData.cargo || '';
        document.getElementById('perfilTelefone').value = this.originalData.telefone || '';
    }

    async savePerfil() {
        const token = this.getToken();
        if (!token) return;

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
            const resp = await fetch(`${API_BASE_URL}/usuarios/me`, {
                method: 'PUT',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
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
            this.toast('danger', 'fas fa-exclamation-circle', e.message);
        } finally {
            this.saveBtn.disabled = false;
            this.saveBtn.innerHTML = '<i class="fas fa-save"></i> Salvar';
        }
    }

    async alterarSenha() {
        const token = this.getToken();
        if (!token) return;

        const senhaAtual    = document.getElementById('perfilSenhaAtual').value;
        const novaSenha     = document.getElementById('perfilNovaSenha').value;
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
            const resp = await fetch(`${API_BASE_URL}/usuarios/me/senha`, {
                method: 'PUT',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ senhaAtual, novaSenha })
            });

            const json = await resp.json().catch(() => ({}));

            if (!resp.ok) {
                throw new Error(json.mensagem || 'Erro ao alterar senha');
            }

            this.senhaForm.reset();
            this.toast('success', 'fas fa-check-circle', 'Senha alterada com sucesso!');
        } catch (e) {
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

    getToken() {
        return localStorage.getItem('crm_token')
            || localStorage.getItem('token')
            || localStorage.getItem('jwtToken')
            || null;
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
