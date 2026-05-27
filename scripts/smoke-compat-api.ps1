# 用法: powershell -File scripts/smoke-compat-api.ps1
# 可选: $env:API_BASE_URL="https://api-test.barlog.app"

$Base = if ($env:API_BASE_URL) { $env:API_BASE_URL.TrimEnd('/') } else { "http://localhost:8090" }
$Failed = 0

function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Path,
        [string]$Body = $null,
        [int[]]$ExpectStatus = @(200)
    )
    try {
        $params = @{
            Uri = "$Base$Path"
            Method = $Method
            ContentType = "application/json"
            UseBasicParsing = $true
        }
        if ($Body) { $params.Body = $Body }
        $r = Invoke-WebRequest @params
        if ($ExpectStatus -notcontains $r.StatusCode) {
            Write-Host "[FAIL] $Name - status $($r.StatusCode)" -ForegroundColor Red
            $script:Failed++
            return $null
        }
        Write-Host "[OK] $Name ($($r.StatusCode))" -ForegroundColor Green
        return $r.Content
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($ExpectStatus -contains $code) {
            Write-Host "[OK] $Name ($code)" -ForegroundColor Green
            return $null
        }
        Write-Host "[FAIL] $Name - $($_.Exception.Message)" -ForegroundColor Red
        $script:Failed++
        return $null
    }
}

Write-Host "=== BarLog Frontend Compat Smoke Test ===" -ForegroundColor Cyan

Test-Endpoint "health" GET "/health"
Test-Endpoint "diary summary" GET "/api/diary/summary?month=2026-05"
Test-Endpoint "diary calendar" GET "/api/diary/calendar?month=2026-05"
Test-Endpoint "diary stats" GET "/api/diary/stats"
Test-Endpoint "recent checkins" GET "/api/checkins/recent"
Test-Endpoint "nearby bars" GET "/api/bars/nearby?city=Shanghai"
Test-Endpoint "bar rankings" GET "/api/bars/rankings?city=Shanghai"
Test-Endpoint "bar detail" GET "/api/bars/bar_001"
Test-Endpoint "bar checkins" GET "/api/bars/bar_001/checkins"
Test-Endpoint "gallery feed" GET "/api/gallery/feed?city=Shanghai"
Test-Endpoint "match candidates" GET "/api/match/candidates"
Test-Endpoint "match session" POST "/api/match/session"
Test-Endpoint "conversations" GET "/api/chat/conversations"
Test-Endpoint "persona current" GET "/api/persona/current"
Test-Endpoint "drinks collection" GET "/api/drinks/collection"
Test-Endpoint "upload image stub" POST "/api/uploads/image"
Test-Endpoint "ai recognize drink" POST "/api/ai/recognize-drink" -Body '{"imageUrl":"https://x.jpg"}'
Test-Endpoint "ai generate persona" POST "/api/ai/generate-persona"
Test-Endpoint "ai match reason" POST "/api/ai/match-reason" -Body '{"candidateId":"user_101"}'
Test-Endpoint "ai icebreakers" POST "/api/ai/icebreakers" -Body '{"conversationId":"conv_001"}'

$loginBody = '{"email":"demo@barlog.app","password":"password123"}'
$loginResp = Test-Endpoint "login" POST "/api/auth/login" -Body $loginBody
Test-Endpoint "auth me" GET "/api/auth/me"
Test-Endpoint "auth refresh" POST "/api/auth/refresh" -Body '{"refreshToken":"dummy"}'

$checkinBody = @'
{"photoUrl":"https://images.barlog.local/smoke.jpg","drinkName":"Smoke Test","drinkCategory":"cocktail","moodTags":["warm"],"cardStyle":"receipt","visibility":"private"}
'@
$created = Test-Endpoint "create checkin" POST "/api/checkins" -Body $checkinBody -ExpectStatus @(201)

if ($created -match '"id"\s*:\s*"([^"]+)"') {
    $id = $Matches[1]
    Test-Endpoint "checkin detail" GET "/api/checkins/$id"
}

Test-Endpoint "ai card copy" POST "/api/ai/generate-card-copy" -Body '{"drinkName":"Martini"}'

if ($Failed -eq 0) {
    Write-Host "`nAll smoke tests passed." -ForegroundColor Green
    exit 0
} else {
    Write-Host "`n$Failed test(s) failed." -ForegroundColor Red
    exit 1
}
