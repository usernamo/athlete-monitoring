# Показывает URL API для телефонов в той же Wi‑Fi сети
$ip = (
    Get-NetIPAddress -AddressFamily IPv4 |
    Where-Object {
        $_.IPAddress -notlike "127.*" -and
        $_.PrefixOrigin -ne "WellKnown"
    } |
    Sort-Object InterfaceMetric |
    Select-Object -First 1
).IPAddress

if (-not $ip) {
    Write-Host "Не найден IPv4. Укажите IP вручную в приложении."
    exit 1
}

$url = "http://${ip}:3000/"
Write-Host ""
Write-Host "URL для Android (поле «Адрес сервера»):"
Write-Host "  $url"
Write-Host ""
Write-Host "Проверка с ПК:"
Write-Host "  curl $url`health"
Write-Host ""
