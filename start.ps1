param(
    [switch]$SkipSeed  = $false,
    [switch]$InfraOnly = $false,
    [switch]$AppOnly   = $false
)

$ErrorActionPreference = "Stop"

function Write-Step  { param($msg) Write-Host "`n==> $msg" -ForegroundColor Cyan }
function Write-Ok    { param($msg) Write-Host "[OK] $msg" -ForegroundColor Green }
function Write-Warn  { param($msg) Write-Host "[WARN] $msg" -ForegroundColor Yellow }

Write-Host "`n==== Football Analytics Platform - Local Stack Startup ====" -ForegroundColor Magenta

Write-Step "Checking port conflicts on critical ports (8123, 8000, 3000)..."
@(8123, 8000, 3000) | ForEach-Object {
    $p = $_
    $inUse = netstat -ano 2>$null | Select-String ":$p " | Select-String "LISTENING"
    if ($inUse) { Write-Warn "Port $p is already in use!" } else { Write-Host "  Port $p : OK" -ForegroundColor DarkGray }
}

if (-not $AppOnly) {
    Write-Step "Starting DE Infrastructure (ClickHouse, Redpanda, MinIO, Kestra, Postgres, Redis)..."
    docker-compose up -d clickhouse redpanda minio postgres redis kestra
    Write-Ok "Infrastructure containers started."

    Write-Step "Waiting for ClickHouse to become healthy (up to 90 seconds)..."
    $maxWait = 90; $elapsed = 0; $ready = $false
    while ($elapsed -lt $maxWait) {
        try {
            $r = docker-compose exec -T clickhouse clickhouse-client --query "SELECT 1" 2>$null
            if ($r -eq "1") { $ready = $true; break }
        } catch { }
        Start-Sleep -Seconds 5; $elapsed += 5
        Write-Host "  ... $elapsed / $maxWait s" -ForegroundColor DarkGray
    }
    if ($ready) { Write-Ok "ClickHouse is healthy!" } else { Write-Warn "ClickHouse timeout - seeding may fail." }
}

if (-not $SkipSeed -and -not $InfraOnly -and -not $AppOnly) {
    Write-Step "Seeding ClickHouse with real StatsBomb La Liga data..."
    $env:CLICKHOUSE_HOST = "localhost"
    $env:CLICKHOUSE_HTTP_PORT = "8123"
    $env:CLICKHOUSE_DB = "football_analytics"
    $env:CLICKHOUSE_USER = "default"
    $env:CLICKHOUSE_PASSWORD = "clickhouse_dev"
    $env:CLICKHOUSE_SECURE = "false"
    try {
        python data-platform/ingestion/seed_data.py
        Write-Ok "Seed data loaded into ClickHouse successfully!"
    } catch {
        Write-Warn "Seed failed: $_ -- backend will use in-memory SeedDataStore fallback."
    }
}

if ($InfraOnly) {
    Write-Ok "Infrastructure is up. Access points:"
    Write-Host "  ClickHouse HTTP : http://localhost:8123" -ForegroundColor Cyan
    Write-Host "  Redpanda Console: http://localhost:8088" -ForegroundColor Cyan
    Write-Host "  MinIO Console   : http://localhost:9011 (admin/minioadmin123)" -ForegroundColor Cyan
    Write-Host "  Kestra UI       : http://localhost:8081" -ForegroundColor Cyan
    exit 0
}

Write-Step "Building and starting Backend (Spring Boot) + Frontend (React/Nginx)..."
Write-Host "  NOTE: First build may take 3-8 minutes (Maven + npm inside Docker)" -ForegroundColor DarkGray
docker-compose up -d --build backend frontend
Write-Ok "Backend and Frontend containers started!"

Write-Host ""
Write-Host "==== ALL SYSTEMS RUNNING ====" -ForegroundColor Green
Write-Host "  Frontend (UI)     : http://localhost:3000" -ForegroundColor Cyan
Write-Host "  Backend API       : http://localhost:8000/health" -ForegroundColor Cyan
Write-Host "  ClickHouse HTTP   : http://localhost:8123" -ForegroundColor Cyan
Write-Host "  Redpanda Console  : http://localhost:8088" -ForegroundColor Cyan
Write-Host "  MinIO Console     : http://localhost:9011" -ForegroundColor Cyan
Write-Host "  Kestra UI         : http://localhost:8081" -ForegroundColor Cyan
Write-Host ""
Write-Host "  View logs : docker-compose logs -f" -ForegroundColor DarkGray
Write-Host "  Stop all  : docker-compose down" -ForegroundColor DarkGray
