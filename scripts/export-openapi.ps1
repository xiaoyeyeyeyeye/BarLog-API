# 导出 OpenAPI 文档到 docs/ 目录（无需 PostgreSQL）
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$docs = Join-Path (Split-Path -Parent $root) "docs"
if (-not (Test-Path $docs)) { $docs = Join-Path $root "..\docs" }

Write-Host "Starting server with profile=swagger ..."
$job = Start-Job {
    Set-Location $using:root
    mvn -q spring-boot:run "-Dspring-boot.run.profiles=swagger" 2>&1
}

$deadline = (Get-Date).AddSeconds(90)
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 2
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:8080/v3/api-docs" -UseBasicParsing -TimeoutSec 3
        if ($r.StatusCode -eq 200) { break }
    } catch { }
}
if (-not $?) {
    Stop-Job $job; Remove-Job $job
    throw "Server did not start within 90s"
}

Invoke-WebRequest -Uri "http://localhost:8080/v3/api-docs" -OutFile (Join-Path $docs "openapi.json")
Invoke-WebRequest -Uri "http://localhost:8080/v3/api-docs.yaml" -OutFile (Join-Path $docs "openapi.yaml")
Write-Host "Exported to $docs\openapi.json and openapi.yaml"

Stop-Job $job -ErrorAction SilentlyContinue
Remove-Job $job -ErrorAction SilentlyContinue
Get-Process -Name java -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle -eq '' } | Stop-Process -Force -ErrorAction SilentlyContinue
