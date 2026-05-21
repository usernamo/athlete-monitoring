# Установка Docker Desktop на Windows
# Запуск: правой кнопкой PowerShell -> "Запуск от имени администратора"
#   Set-ExecutionPolicy -Scope Process Bypass -Force
#   cd C:\Users\PC\athlete-monitoring
#   .\install-docker.ps1

$ErrorActionPreference = "Continue"
$Log = Join-Path $PSScriptRoot "docker-install.log"

function Log($msg) {
    $line = "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] $msg"
    Write-Host $line
    Add-Content -Path $Log -Value $line -Encoding UTF8
}

"" | Set-Content $Log -Encoding UTF8
Log "=== Docker install script started ==="

# 1. Уже установлен?
if (Get-Command docker -ErrorAction SilentlyContinue) {
    $ver = docker --version 2>&1
    Log "Docker already installed: $ver"
    Log "If Docker Desktop is not running, start it from the Start menu."
    exit 0
}

# 2. Включить компоненты Windows (WSL2 / VM Platform) — нужны для Docker
Log "Enabling Windows features (may require reboot)..."
$features = @(
    "VirtualMachinePlatform",
    "Microsoft-Windows-Subsystem-Linux"
)
foreach ($f in $features) {
    try {
        Enable-WindowsOptionalFeature -Online -FeatureName $f -NoRestart -All 2>&1 | Out-Null
        Log "Feature enabled (or already on): $f"
    } catch {
        Log "Feature $f : $($_.Exception.Message)"
    }
}

# 3. winget
if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
    Log "ERROR: winget not found. Install App Installer from Microsoft Store, or download Docker manually:"
    Log "https://desktop.docker.com/win/main/amd64/Docker%20Desktop%20Installer.exe"
    exit 1
}

Log "Installing Docker Desktop via winget (this may take several minutes)..."
$wingetOut = winget install -e --id Docker.DockerDesktop `
    --accept-package-agreements `
    --accept-source-agreements 2>&1
$wingetOut | ForEach-Object { Log $_ }

if ($LASTEXITCODE -ne 0) {
    Log "winget exit code: $LASTEXITCODE"
    Log "Trying direct download installer..."
    $installer = Join-Path $env:TEMP "DockerDesktopInstaller.exe"
    $url = "https://desktop.docker.com/win/main/amd64/Docker%20Desktop%20Installer.exe"
    try {
        Invoke-WebRequest -Uri $url -OutFile $installer -UseBasicParsing
        Log "Running installer: $installer install --quiet"
        Start-Process -FilePath $installer -ArgumentList "install", "--quiet" -Wait
    } catch {
        Log "Download/install failed: $($_.Exception.Message)"
        exit 1
    }
}

# 4. Проверка
Start-Sleep -Seconds 3
if (Get-Command docker -ErrorAction SilentlyContinue) {
    Log "SUCCESS: $(docker --version 2>&1)"
} else {
    Log "Docker CLI not in PATH yet — normal before first reboot."
}

Log ""
Log "=== NEXT STEPS ==="
Log "1. Reboot the computer if Windows asked for it."
Log "2. Start 'Docker Desktop' from the Start menu."
Log "3. Wait until Docker says it is running (whale icon in tray)."
Log "4. Open a new terminal and run:"
Log "   cd C:\Users\PC\athlete-monitoring"
Log "   docker compose up -d"
Log "5. Test: docker run hello-world"
Log ""
Log "Log file: $Log"
