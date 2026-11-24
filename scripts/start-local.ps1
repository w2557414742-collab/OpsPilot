# Starts the locally built Java API and deterministic Python worker on loopback.
$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$python = if ($env:OPSPILOT_PYTHON) { $env:OPSPILOT_PYTHON } else { Join-Path $repo '.local/venv/Scripts/python.exe' }
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { 'java' }
$jar = Join-Path $repo 'backend-java/ops-bootstrap/target/ops-bootstrap-0.1.0-SNAPSHOT.jar'
$port = if ($env:OPSPILOT_AGENT_PORT) { [int]$env:OPSPILOT_AGENT_PORT } else { 8000 }
foreach ($name in @('OPSPILOT_DB_URL', 'OPSPILOT_DB_USER', 'OPSPILOT_DB_PASSWORD', 'OPSPILOT_JWT_KEY', 'OPSPILOT_AGENT_TOKEN')) {
    if (-not [Environment]::GetEnvironmentVariable($name)) { throw "Set $name in the current terminal." }
}
if (-not (Test-Path -LiteralPath $jar)) { throw 'Build the Java jar first.' }
Get-Command $python, $java -ErrorAction Stop | Out-Null

$probe = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $port)
try {
    $probe.Start()
} finally {
    $probe.Stop()
}

$managed = @('SERVER_ADDRESS', 'OPSPILOT_AGENT_URL', 'OPSPILOT_PROVIDER', 'PYTHONPATH', 'OPSPILOT_AGENT_DB')
$previous = @{}
foreach ($name in $managed) { $previous[$name] = [Environment]::GetEnvironmentVariable($name) }
$job = $null
Push-Location -LiteralPath $repo
try {
    $env:SERVER_ADDRESS = '127.0.0.1'
    $env:OPSPILOT_AGENT_URL = "http://127.0.0.1:$port"
    $env:OPSPILOT_PROVIDER = 'DETERMINISTIC_TEST'
    $env:PYTHONPATH = Join-Path $repo 'agent-python/src'
    if (-not $env:OPSPILOT_AGENT_DB) { $env:OPSPILOT_AGENT_DB = Join-Path $repo '.local/agent.sqlite3' }
    $job = Start-Job -ArgumentList $repo, $python, $port -ScriptBlock {
        param($root, $runtime, $listenPort)
        Set-Location -LiteralPath $root
        & $runtime -m uvicorn opspilot_agent.api:from_env --factory --host 127.0.0.1 --port $listenPort
    }
    $ready = $false
    for ($i = 0; $i -lt 40 -and $job.State -in @('NotStarted', 'Running'); $i++) {
        try {
            $health = Invoke-RestMethod -Uri "$env:OPSPILOT_AGENT_URL/health" -TimeoutSec 1
            if ($health.status -eq 'UP' -and $health.provider -eq 'DETERMINISTIC_TEST') { $ready = $true; break }
        } catch {
            Start-Sleep -Milliseconds 250
        }
    }
    if (-not $ready) { throw 'The local Python Agent did not become healthy.' }
    & $java -jar $jar
    if ($LASTEXITCODE -ne 0) { throw "Java exited with code $LASTEXITCODE." }
} finally {
    if ($job) {
        Stop-Job $job
        Receive-Job $job -ErrorAction Continue
        Remove-Job $job
    }
    foreach ($name in $managed) { [Environment]::SetEnvironmentVariable($name, $previous[$name]) }
    Pop-Location
}
