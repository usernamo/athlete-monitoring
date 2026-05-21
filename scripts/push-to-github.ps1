# Подготовка репозитория для Render (запуск из корня athlete-monitoring)
param(
    [Parameter(Mandatory = $true)]
    [string]$GitHubRepoUrl
)

$ErrorActionPreference = "Stop"
Set-Location (Split-Path $PSScriptRoot -Parent)

if (-not (Test-Path ".git")) {
    git init
    Write-Host "Git initialized."
}

git add .
git status --short | Select-Object -First 20
$msg = "Deploy to Render - athlete monitoring"
git commit -m $msg 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "Nothing to commit or commit failed (maybe already committed)."
}

$remote = git remote get-url origin 2>$null
if (-not $remote) {
    git remote add origin $GitHubRepoUrl
    Write-Host "Remote added: $GitHubRepoUrl"
} else {
    Write-Host "Remote already set: $remote"
}

git branch -M main
git push -u origin main
Write-Host ""
Write-Host "Done. Next: Render Dashboard -> New -> Blueprint -> select repo."
