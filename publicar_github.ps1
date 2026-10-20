param(
    [int]$VersaoCodigo,
    [string]$VersaoNome,
    [string]$Notas = "Nova atualizacao publicada pelo GitHub."
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

# 1. Detectar ou atualizar versões no build.gradle.kts
$gradleFile = Join-Path $PSScriptRoot "app\build.gradle.kts"
$gradleContent = Get-Content $gradleFile -Raw

$codigoAtual = 59
if ($gradleContent -match 'versionCode\s*=\s*(\d+)') {
    $codigoAtual = [int]$matches[1]
}

$nomeAtual = "2.0.19"
if ($gradleContent -match 'versionName\s*=\s*"([^"]+)"') {
    $nomeAtual = $matches[1]
}

if (-not $PSBoundParameters.ContainsKey('VersaoCodigo') -or $VersaoCodigo -le 0) {
    $VersaoCodigo = $codigoAtual
}

if (-not $PSBoundParameters.ContainsKey('VersaoNome') -or [string]::IsNullOrWhiteSpace($VersaoNome)) {
    $VersaoNome = $nomeAtual
}

Write-Host "=== Publicando atualizacao CineList ===" -ForegroundColor Yellow
Write-Host "Versao: v$VersaoNome (build $VersaoCodigo)" -ForegroundColor Yellow
Write-Host "Notas: $Notas" -ForegroundColor Yellow

# Atualiza no app/build.gradle.kts se necessário
$gradleContent = $gradleContent -replace 'versionCode\s*=\s*\d+', "versionCode = $VersaoCodigo"
$gradleContent = $gradleContent -replace 'versionName\s*=\s*"[^"]+"', "versionName = `"$VersaoNome`""
Set-Content -Path $gradleFile -Value $gradleContent -NoNewline

Write-Host "`n1. Compilando APK assinado com a chave oficial..." -ForegroundColor Cyan
.\gradlew.bat assembleRelease --console=plain

$apkOrigem = Join-Path $PSScriptRoot "app\build\outputs\apk\release\app-release.apk"
if (-not (Test-Path $apkOrigem)) {
    # Fallback para debug se release falhar
    Write-Host "Compilando debug como fallback..." -ForegroundColor Yellow
    .\gradlew.bat assembleDebug --console=plain
    $apkOrigem = Join-Path $PSScriptRoot "app\build\outputs\apk\debug\app-debug.apk"
}

if (-not (Test-Path $apkOrigem)) {
    throw "APK nao encontrado em $apkOrigem"
}

$pastaAtu = Join-Path $PSScriptRoot "atualizacao"
New-Item -ItemType Directory -Force -Path $pastaAtu | Out-Null
$apkDestino = Join-Path $pastaAtu "cinelist.apk"

Copy-Item $apkOrigem $apkDestino -Force

$apkUrl = "https://raw.githubusercontent.com/AlissonJr98/Cinelist-Atualiza-es/main/atualizacao/cinelist.apk"

# Cria atualizacao/versao.json (padrão LivennIA)
$jsonNovo = [ordered]@{
    versaoCodigo = $VersaoCodigo
    versaoNome   = $VersaoNome
    notas        = $Notas
    apkUrl       = $apkUrl
} | ConvertTo-Json -Depth 4

Set-Content -Path (Join-Path $pastaAtu "versao.json") -Value $jsonNovo -Encoding UTF8

# Atualiza também version.json na raiz para compatibilidade com versões antigas instaladas
$jsonRaiz = [ordered]@{
    versionCode = $VersaoCodigo
    versionName = $VersaoNome
    apkUrl      = $apkUrl
    changelog   = $Notas
} | ConvertTo-Json -Depth 4

Set-Content -Path (Join-Path $PSScriptRoot "version.json") -Value $jsonRaiz -Encoding UTF8

Write-Host "`n2. Enviando para o GitHub..." -ForegroundColor Cyan
git add .
git commit -m "Release v$VersaoNome (build $VersaoCodigo) - $Notas"
git push origin main

Write-Host "`nPronto! O CineList assinado ja pode atualizar pelo GitHub para a v$VersaoNome." -ForegroundColor Green
