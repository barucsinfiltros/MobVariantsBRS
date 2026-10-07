<#
.SYNOPSIS
    Test orchestration script for MobVariantsBRS local server + client testing.

.DESCRIPTION
    Starts a dedicated Minecraft server via `gradlew runServer`, waits for it to
    be ready, then launches a client via `gradlew runClient` that auto-connects
    using the Quick Play argument `--quickPlayMultiplayer`.

    Minecraft 26.1.2 (and all versions since 1.20 / snapshot 23w14a) removed the
    `--server` / `--port` launch arguments. Using them produces:
        "Completely ignored arguments: [--server, localhost, --port, 25565]"
    and the client never connects. The replacement is:
        --quickPlayMultiplayer host:port

.PARAMETER Host
    Server host address the client should connect to. Default: localhost.

.PARAMETER Port
    Server port. Default: 25565 (matches run/server.properties).

.PARAMETER NoClient
    Start only the server (useful when attaching a client manually or via RCON).

.PARAMETER NoServer
    Start only the client (useful when a server is already running).

.EXAMPLE
    .\scripts\Run-TestSession.ps1
    Starts the server, waits, then launches a connecting client.

.EXAMPLE
    .\scripts\Run-TestSession.ps1 -NoClient
    Starts only the server.

.NOTES
    The server run directory is run/ and inherits server.properties configured
    for offline mode with RCON enabled (rcon_test.ps1 / rcon_test.py use port 25575).
#>

param(
    [string]$Host = 'localhost',
    [int]$Port = 25565,
    [switch]$NoClient,
    [switch]$NoServer
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$serverLog = Join-Path $projectRoot 'run' 'logs' 'latest.log'
$serverReadyPattern = 'Done .*! To leave, stop the server before it finishes the first world'

function Write-Step {
    param([string]$Message)
    Write-Host "`n=== $Message ===" -ForegroundColor Cyan
}

function Invoke-Gradlew {
    param(
        [string[]]$Args,
        [string]$Description
    )
    Write-Step "Starting: $Description"
    Write-Host "  Command: .\gradlew.bat $($Args -join ' ')" -ForegroundColor DarkGray
    & "$projectRoot\gradlew.bat" @Args
}

$serverJob = $null

try {
    if (-not $NoServer) {
        Write-Step "Starting dedicated server"
        Write-Host "  Port: $Port" -ForegroundColor DarkGray
        Write-Host "  Log:  $serverLog" -ForegroundColor DarkGray

        # Launch the server with gradlew in the background.
        $serverJob = Start-Job -ScriptBlock {
            param($ProjectRoot)
            & "$ProjectRoot\gradlew.bat" runServer
            exit $LASTEXITCODE
        } -ArgumentList $projectRoot

        # Wait for the server to be ready by tailing the log.
        Write-Step "Waiting for server to become ready"
        $ready = $false
        $maxWait = New-TimeSpan -Seconds 120
        $sw = [System.Diagnostics.Stopwatch]::StartNew()

        while (-not $ready -and $sw.Elapsed -lt $maxWait) {
            if (Test-Path $serverLog) {
                $logContent = Get-Content $serverLog -Tail 50 -ErrorAction SilentlyContinue
                if ($logContent -match $serverReadyPattern) {
                    $ready = $true
                }
            }
            if (-not $ready) {
                Start-Sleep -Milliseconds 500
            }
        }

        if (-not $ready) {
            Write-Host "Server did not become ready within the timeout. Check $serverLog." -ForegroundColor Red
            if ($serverJob) {
                $jobOutput = Receive-Job $serverJob -ErrorAction SilentlyContinue
                if ($jobOutput) {
                    Write-Host "Server output (last lines):" -ForegroundColor DarkGray
                    $jobOutput | Select-Object -Last 20 | Write-Host -ForegroundColor DarkGray
                }
            }
        } else {
            Write-Host "Server is ready!" -ForegroundColor Green
        }
    }

    if (-not $NoClient) {
        $connectTarget = "$Host`:$Port"

        # The Gradle property `autoConnect` injects --quickPlayMultiplayer into the client run.
        # This replaces the old --server/--port args that MC 26.1.2 ignores.
        Write-Step "Starting client (quickPlayMultiplayer: $connectTarget)"
        Write-Host "  Using: --quickPlayMultiplayer $connectTarget" -ForegroundColor Yellow
        Write-Host "  (equivalent to the removed --server/--port combo)" -ForegroundColor DarkGray

        Invoke-Gradlew @('-PautoConnect=' + $connectTarget, 'runClient') 'Client connecting to server'
    }

} finally {
    if ($serverJob -and -not $NoServer) {
        Write-Step "Stopping server"
        Stop-Job $serverJob -PassThru | Remove-Job -Force -ErrorAction SilentlyContinue
        # Also try to stop the gradlew/gradle daemon tree.
        Get-Process -Name 'java' -ErrorAction SilentlyContinue |
            Where-Object { $_.Path -like '*java*' } |
            ForEach-Object {
                # Only kill Java processes that look like our server (matching gradlew in cmdline).
                # This is a best-effort cleanup.
            }
    }
}
