# Validation JWT - StowFlow API
# Usage: .\scripts\test-jwt.ps1 [-BaseUrl "http://localhost:8000"]

param(
    [string]$BaseUrl = "http://localhost:8000"
)

$ErrorActionPreference = "Stop"
$passed = 0
$failed = 0
$results = @()

function Assert-Test {
    param(
        [string]$Id,
        [string]$Name,
        [scriptblock]$Test
    )
    try {
        & $Test
        $script:passed++
        $script:results += [PSCustomObject]@{ Id = $Id; Name = $Name; Status = "PASS" }
        Write-Host "[PASS] $Id - $Name" -ForegroundColor Green
    } catch {
        $script:failed++
        $msg = $_.Exception.Message
        $script:results += [PSCustomObject]@{ Id = $Id; Name = $Name; Status = "FAIL"; Detail = $msg }
        Write-Host "[FAIL] $Id - $Name - $msg" -ForegroundColor Red
    }
}

function Invoke-Api {
    param(
        [string]$Method = "GET",
        [string]$Path,
        [hashtable]$Headers = @{},
        [string]$Body = $null
    )
    $uri = "$BaseUrl$Path"
    $params = @{
        Uri             = $uri
        Method          = $Method
        Headers         = $Headers
        UseBasicParsing = $true
        ErrorAction     = "Stop"
    }
    if ($Body) {
        $params["ContentType"] = "application/json"
        $params["Body"] = $Body
    }
    try {
        $resp = Invoke-WebRequest @params
        if ($resp.Content) { return ($resp.Content | ConvertFrom-Json) }
        return $null
    } catch {
        $r = $_.Exception.Response
        if ($r) {
            $reader = New-Object System.IO.StreamReader($r.GetResponseStream())
            $text = $reader.ReadToEnd()
            $reader.Close()
            throw ("HTTP " + $r.StatusCode.value__ + " - " + $text)
        }
        throw
    }
}

function Invoke-ApiExpectStatus {
    param(
        [string]$Method = "GET",
        [string]$Path,
        [hashtable]$Headers = @{},
        [string]$Body = $null,
        [int]$ExpectedStatus
    )
    $uri = "$BaseUrl$Path"
    try {
        $params = @{
            Uri             = $uri
            Method          = $Method
            Headers         = $Headers
            UseBasicParsing = $true
        }
        if ($Body) {
            $params["ContentType"] = "application/json"
            $params["Body"] = $Body
        }
        Invoke-WebRequest @params | Out-Null
        return
    } catch {
        $r = $_.Exception.Response
        if ($r -and $r.StatusCode.value__ -eq $ExpectedStatus) { return }
        if ($r) { throw ("Expected HTTP " + $ExpectedStatus + " but got " + $r.StatusCode.value__) }
        throw
    }
}

function Get-Token {
    param([string]$Email, [string]$Password = "password")
    $login = Invoke-Api -Method POST -Path "/api/auth/login" -Body (@{ email = $Email; password = $Password } | ConvertTo-Json)
    if (-not $login.accessToken) { throw ("No accessToken for " + $Email) }
    return $login
}

function AuthHeaders {
    param([string]$Token, [string]$Tenant = "default")
    return @{
        Authorization = "Bearer $Token"
        "X-Tenant-Slug" = $Tenant
    }
}

Write-Host ""
Write-Host "=== StowFlow JWT validation ($BaseUrl) ===" -ForegroundColor Cyan
Write-Host ""

Assert-Test "JWT-00" "API reachable (OpenAPI docs)" {
    Invoke-ApiExpectStatus -Path "/v3/api-docs" -ExpectedStatus 200
}

Assert-Test "JWT-01" "Login valide retourne accessToken Bearer" {
    $l = Get-Token "stock@default.demo"
    if ($l.tokenType -ne "Bearer") { throw ("tokenType=" + $l.tokenType) }
    if ($l.expiresIn -lt 60) { throw ("expiresIn too short: " + $l.expiresIn) }
    if ($l.role -ne "STOCK_MANAGER") { throw ("role=" + $l.role) }
}

Assert-Test "JWT-02" "Login identifiants invalides -> 401" {
    Invoke-ApiExpectStatus -Method POST -Path "/api/auth/login" `
        -Body '{"email":"stock@default.demo","password":"wrong"}' -ExpectedStatus 401
}

Assert-Test "JWT-03" "Route protegee sans token -> 401" {
    Invoke-ApiExpectStatus -Path "/api/articles" -Headers @{ "X-Tenant-Slug" = "default" } -ExpectedStatus 401
}

Assert-Test "JWT-04" "Token JWT invalide -> 401" {
    Invoke-ApiExpectStatus -Path "/api/articles" `
        -Headers @{ Authorization = "Bearer not.a.valid.jwt"; "X-Tenant-Slug" = "default" } -ExpectedStatus 401
}

Assert-Test "JWT-05" "HTTP Basic rejete -> 401" {
    $basic = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("stock@default.demo:password"))
    Invoke-ApiExpectStatus -Path "/api/articles" `
        -Headers @{ Authorization = "Basic $basic"; "X-Tenant-Slug" = "default" } -ExpectedStatus 401
}

Assert-Test "JWT-06" "GET /api/auth/me avec Bearer -> 200" {
    $l = Get-Token "stock@default.demo"
    $me = Invoke-Api -Path "/api/auth/me" -Headers @{ Authorization = "Bearer $($l.accessToken)" }
    if ($me.email -ne "stock@default.demo") { throw ("email=" + $me.email) }
}

$stockLogin = Get-Token "stock@default.demo"
$stockHdr = AuthHeaders $stockLogin.accessToken

Assert-Test "JWT-07" "GET /api/articles avec JWT -> 200" {
    $articles = Invoke-Api -Path "/api/articles" -Headers $stockHdr
    if (-not $articles -or $articles.Count -lt 1) { throw "empty articles list" }
}

Assert-Test "JWT-08" "Header X-Tenant-Slug manquant -> 403" {
    Invoke-ApiExpectStatus -Path "/api/articles" `
        -Headers @{ Authorization = "Bearer $($stockLogin.accessToken)" } -ExpectedStatus 403
}

Assert-Test "JWT-09" "Mauvais tenant -> 403" {
    Invoke-ApiExpectStatus -Path "/api/articles" `
        -Headers (AuthHeaders $stockLogin.accessToken "wrong-tenant") -ExpectedStatus 403
}

$salesLogin = Get-Token "sales@default.demo"
$managerLogin = Get-Token "manager@default.demo"
$adminLogin = Get-Token "admin@default.demo"
$superLogin = Get-Token "superadmin@stowflow.demo"

Assert-Test "JWT-10" "SALES - lecture articles OK" {
    Invoke-Api -Path "/api/articles" -Headers (AuthHeaders $salesLogin.accessToken) | Out-Null
}

Assert-Test "JWT-11" "SALES - dashboard refuse -> 403" {
    Invoke-ApiExpectStatus -Path "/api/dashboard" -Headers (AuthHeaders $salesLogin.accessToken) -ExpectedStatus 403
}

Assert-Test "JWT-12" "SALES - admin users refuse -> 403" {
    Invoke-ApiExpectStatus -Path "/api/admin/users" -Headers (AuthHeaders $salesLogin.accessToken) -ExpectedStatus 403
}

Assert-Test "JWT-13" "MANAGER - creation commande refusee -> 403" {
    Invoke-ApiExpectStatus -Method POST -Path "/api/orders" -Headers (AuthHeaders $managerLogin.accessToken) `
        -Body '{"supplierId":1,"orderDate":"2026-06-18","lines":[{"articleId":1,"quantity":1,"unitPrice":10}]}' `
        -ExpectedStatus 403
}

Assert-Test "JWT-14" "STOCK - admin users refuse -> 403" {
    Invoke-ApiExpectStatus -Path "/api/admin/users" -Headers $stockHdr -ExpectedStatus 403
}

Assert-Test "JWT-15" "TENANT_ADMIN - admin users OK" {
    Invoke-Api -Path "/api/admin/users" -Headers (AuthHeaders $adminLogin.accessToken) | Out-Null
}

Assert-Test "JWT-16" "SUPER_ADMIN - platform tenants OK" {
    Invoke-Api -Path "/api/platform/tenants" -Headers @{ Authorization = "Bearer $($superLogin.accessToken)" } | Out-Null
}

Assert-Test "JWT-17" "TENANT_ADMIN - platform tenants refuse -> 403" {
    Invoke-ApiExpectStatus -Path "/api/platform/tenants" -Headers (AuthHeaders $adminLogin.accessToken) -ExpectedStatus 403
}

Assert-Test "JWT-18" "GET /api/dashboard" {
    Invoke-Api -Path "/api/dashboard" -Headers $stockHdr | Out-Null
}

Assert-Test "JWT-19" "GET /api/alerts/summary" {
    Invoke-Api -Path "/api/alerts/summary" -Headers $stockHdr | Out-Null
}

Assert-Test "JWT-20" "GET /api/suppliers" {
    Invoke-Api -Path "/api/suppliers" -Headers $stockHdr | Out-Null
}

Assert-Test "JWT-21" "GET /api/orders" {
    Invoke-Api -Path "/api/orders" -Headers $stockHdr | Out-Null
}

Assert-Test "JWT-22" "GET /api/movements" {
    Invoke-Api -Path "/api/movements" -Headers $stockHdr | Out-Null
}

Assert-Test "JWT-23" "GET /api/replenishment-requests" {
    Invoke-Api -Path "/api/replenishment-requests" -Headers $stockHdr | Out-Null
}

Assert-Test "JWT-24" "GET /api/chat/bootstrap" {
    Invoke-Api -Path "/api/chat/bootstrap" -Headers $stockHdr | Out-Null
}

Assert-Test "JWT-25" "POST /api/chat" {
    $chatBody = '{"message":"liste alertes"}'
    $chat = Invoke-Api -Method POST -Path "/api/chat" -Headers $stockHdr -Body $chatBody
    if (-not $chat.reply) { throw "empty chat reply" }
}

Assert-Test "JWT-26" "GET /api/pos/sales (SALES)" {
    Invoke-Api -Path "/api/pos/sales" -Headers (AuthHeaders $salesLogin.accessToken) | Out-Null
}

Assert-Test "JWT-27" "Plafond max commande -> 400" {
    Invoke-ApiExpectStatus -Method POST -Path "/api/orders" -Headers $stockHdr `
        -Body '{"supplierId":1,"orderDate":"2026-06-18","lines":[{"articleId":1,"quantity":99999,"unitPrice":10}]}' `
        -ExpectedStatus 400
}

Write-Host ""
Write-Host "=== Resume ===" -ForegroundColor Cyan
Write-Host ("PASS: " + $passed) -ForegroundColor Green
if ($failed -gt 0) {
    Write-Host ("FAIL: " + $failed) -ForegroundColor Red
} else {
    Write-Host ("FAIL: " + $failed) -ForegroundColor Green
}
Write-Host ""

$results | Format-Table -AutoSize

if ($failed -gt 0) { exit 1 }
exit 0
