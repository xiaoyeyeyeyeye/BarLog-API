# 停止本机 BarLog 联调用的后端(8090)与前端 Expo(8081)
$ErrorActionPreference = "SilentlyContinue"

function Stop-PortListener {
    param([int]$Port)
    $lines = netstat -ano | Select-String ":$Port\s" | Select-String "LISTENING"
    foreach ($line in $lines) {
        $parts = ($line -split '\s+') | Where-Object { $_ -ne '' }
        $pid = [int]$parts[-1]
        if ($pid -gt 0) {
            Write-Host "Stopping PID $pid (port $Port)..."
            taskkill /PID $pid /F | Out-Null
        }
    }
}

Stop-PortListener -Port 8090
Stop-PortListener -Port 8081
Write-Host "Done. Check: netstat -ano | findstr `":8090 :8081`""
