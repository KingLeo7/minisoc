# Live Demonstration of MiniSOC Real-time Intrusion Detection
# Directly feeds alerts into alerts.json and monitored logs so it displays live in the GUI!

Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "       MiniSOC - Live Intrusion Detection Demonstration         " -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

$authLog = "samples/auth.log"
$accessLog = "samples/access.log"
$alertsFile = "alerts.json"

function Append-Log($path, $text) {
    $fs = [System.IO.File]::Open($path, [System.IO.FileMode]::Append, [System.IO.FileAccess]::Write, [System.IO.FileShare]::ReadWrite)
    $sw = [System.IO.StreamWriter]::new($fs)
    $sw.WriteLine($text)
    $sw.Dispose()
    $fs.Dispose()
}

Write-Host "[1] Connecting to MiniSOC sensor engine (monitoring $authLog & $accessLog)..." -ForegroundColor Yellow
$process = Start-Process -FilePath "java" -ArgumentList "-cp", "out", "minisoc.Main", "--out", $alertsFile, $authLog, $accessLog -PassThru -NoNewWindow

Start-Sleep -Milliseconds 1000

Write-Host "[2] Normal traffic: Legitimate user 'alice' logs in via SSH..." -ForegroundColor Gray
Append-Log $authLog "Nov  8 21:50:01 sensor sshd[1001]: Accepted publickey for alice from 192.168.1.50 port 54200 ssh2"
Start-Sleep -Milliseconds 600

Write-Host "`n[3] ATTACK DETECTED: Attacker (10.0.0.99) launching SSH brute force..." -ForegroundColor Red
for ($i = 1; $i -le 5; $i++) {
    $port = 50000 + $i
    Append-Log $authLog "Nov  8 21:50:02 sensor sshd[101$i]: Failed password for root from 10.0.0.99 port $port ssh2"
    Start-Sleep -Milliseconds 250
}

Write-Host "`n[4] CRITICAL COMPROMISE: Attacker cracked root password and logged in!" -ForegroundColor DarkRed
Append-Log $authLog "Nov  8 21:50:03 sensor sshd[1020]: Accepted password for root from 10.0.0.99 port 50010 ssh2"
Start-Sleep -Milliseconds 1000

Write-Host "`n[5] WEB ATTACK: Attacker injects SQL Injection payload into web server..." -ForegroundColor Red
Append-Log $accessLog "10.0.0.99 - - [08/Nov/2026:21:50:04 +0530] `"GET /dvwa/vulnerabilities/sqli/?id=1%27+UNION+SELECT+user,password+FROM+users--+&submit=submit HTTP/1.1`" 200 4520 `"-`" `"Mozilla/5.0`""
Start-Sleep -Milliseconds 1200

Write-Host "`n[6] Stopping background sensor..." -ForegroundColor Yellow
Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue

Write-Host "`n================================================================" -ForegroundColor Cyan
Write-Host "               Generated Alerts in $alertsFile                  " -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan

if (Test-Path $alertsFile) {
    Get-Content $alertsFile -Tail 5 | ForEach-Object {
        Write-Host $_ -ForegroundColor Green
    }
}

Write-Host "`n-> Alerts are saved in $alertsFile and synced LIVE to the GUI Dashboard!" -ForegroundColor Cyan
Write-Host "[Demo completed successfully]`n" -ForegroundColor Cyan
