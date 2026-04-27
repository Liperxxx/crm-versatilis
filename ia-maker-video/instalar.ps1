# ================================================================
# Instalador da skill "ia-maker-video"
# ================================================================
# Instala em: C:\Users\<voce>\.claude\skills\ia-maker-video\
# Esta e a pasta PADRAO de skills pessoais do Claude no Windows.
#
# Como usar:
#   Clique com botao direito neste arquivo -> "Executar com PowerShell"
# ================================================================

$ErrorActionPreference = "Stop"

# Origem: pasta onde este script esta
$Origem = Split-Path -Parent $MyInvocation.MyCommand.Definition

# Destino padrao: ~/.claude/skills/ia-maker-video
$PastaSkillsUsuario = Join-Path $env:USERPROFILE ".claude\skills"
$Destino = Join-Path $PastaSkillsUsuario "ia-maker-video"

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host " Instalador: ia-maker-video skill" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Origem : $Origem"
Write-Host "Destino: $Destino"
Write-Host ""

# Verifica se origem existe
if (-not (Test-Path "$Origem\SKILL.md")) {
    Write-Host "[ERRO] Nao encontrei SKILL.md na origem." -ForegroundColor Red
    Write-Host "       Este script precisa estar dentro da pasta ia-maker-video." -ForegroundColor Red
    Read-Host "Pressione ENTER para fechar"
    exit 1
}

# Cria a pasta .claude\skills se nao existir
if (-not (Test-Path $PastaSkillsUsuario)) {
    Write-Host "Criando pasta de skills do usuario..." -ForegroundColor Yellow
    New-Item -ItemType Directory -Path $PastaSkillsUsuario -Force | Out-Null
    Write-Host "[OK] Pasta criada: $PastaSkillsUsuario" -ForegroundColor Green
    Write-Host ""
}

# Se ja existe destino, pergunta se quer sobrescrever
if (Test-Path $Destino) {
    Write-Host "[AVISO] Ja existe uma skill 'ia-maker-video' instalada." -ForegroundColor Yellow
    $resp = Read-Host "Deseja sobrescrever? (s/n)"
    if ($resp -ne "s" -and $resp -ne "S") {
        Write-Host "Instalacao cancelada." -ForegroundColor Yellow
        Read-Host "Pressione ENTER para fechar"
        exit 0
    }
    Remove-Item -Path $Destino -Recurse -Force
}

# Copia
Write-Host "Copiando arquivos..." -ForegroundColor Green
New-Item -ItemType Directory -Path $Destino -Force | Out-Null

Copy-Item -Path "$Origem\SKILL.md"     -Destination $Destino -Force
Copy-Item -Path "$Origem\referencias"  -Destination $Destino -Recurse -Force
Copy-Item -Path "$Origem\templates"    -Destination $Destino -Recurse -Force
Copy-Item -Path "$Origem\exemplos"     -Destination $Destino -Recurse -Force

$arquivos = Get-ChildItem -Path $Destino -Recurse -File

Write-Host ""
Write-Host "==========================================" -ForegroundColor Green
Write-Host " [OK] Skill instalada com sucesso!" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Arquivos instalados: $($arquivos.Count)" -ForegroundColor Green
Write-Host "Local: $Destino" -ForegroundColor Gray
Write-Host ""
Write-Host "Proximos passos:" -ForegroundColor Cyan
Write-Host "  1. Feche e reabra o Claude desktop" -ForegroundColor White
Write-Host "  2. Abra uma nova conversa" -ForegroundColor White
Write-Host "  3. Teste dizendo: 'quero criar um video com IA'" -ForegroundColor White
Write-Host ""
Write-Host "Se a skill nao aparecer apos reabrir o Claude," -ForegroundColor Yellow
Write-Host "ela pode precisar ser empacotada como plugin." -ForegroundColor Yellow
Write-Host "Me avise que eu te ajudo a empacotar." -ForegroundColor Yellow
Write-Host ""

Read-Host "Pressione ENTER para fechar"
