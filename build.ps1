# ============================================
# Off to Market (Trading Deluxe) - Build Script
# ============================================
# Usage:
#   .\build.ps1          - Standard build
#   .\build.ps1 -Clean   - Clean build (removes old artifacts first)
#   .\build.ps1 -Run     - Build and launch Minecraft client
#   .\build.ps1 -Jar     - Build the distributable mod JAR only
# ============================================

param(
    [switch]$Clean,
    [switch]$Run,
    [switch]$Jar,
    [switch]$Help
)

$ErrorActionPreference = "Stop"

function Write-Step($msg) {
    Write-Host ""
    Write-Host ">> $msg" -ForegroundColor Cyan
    Write-Host ("-" * 50) -ForegroundColor DarkGray
}

function Write-Success($msg) {
    Write-Host "   [OK] $msg" -ForegroundColor Green
}

function Write-Fail($msg) {
    Write-Host "   [FAIL] $msg" -ForegroundColor Red
}

function Show-Menu {
    Write-Host ""
    Write-Host "==========================================" -ForegroundColor Yellow
    Write-Host "  Off to Market (Trading Deluxe) Builder" -ForegroundColor Yellow
    Write-Host "==========================================" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "  Select an option:" -ForegroundColor White
    Write-Host ""
    Write-Host "    [1]  Build" -ForegroundColor Cyan -NoNewline
    Write-Host "              - Compile and build the mod" -ForegroundColor DarkGray
    Write-Host "    [2]  Clean Build" -ForegroundColor Cyan -NoNewline
    Write-Host "        - Remove old artifacts, then build" -ForegroundColor DarkGray
    Write-Host "    [3]  Build & Run" -ForegroundColor Cyan -NoNewline
    Write-Host "        - Build then launch Minecraft client" -ForegroundColor DarkGray
    Write-Host "    [4]  Clean Build & Run" -ForegroundColor Cyan -NoNewline
    Write-Host "  - Full clean, build, and launch client" -ForegroundColor DarkGray
    Write-Host "    [5]  Build JAR Only" -ForegroundColor Cyan -NoNewline
    Write-Host "     - Build distributable mod JAR" -ForegroundColor DarkGray
    Write-Host "    [6]  Run Client Only" -ForegroundColor Cyan -NoNewline
    Write-Host "   - Launch Minecraft (skip build)" -ForegroundColor DarkGray
    Write-Host ""
    Write-Host "    [H]  Help" -ForegroundColor DarkCyan -NoNewline
    Write-Host "             - Show command-line usage" -ForegroundColor DarkGray
    Write-Host "    [Q]  Quit" -ForegroundColor DarkCyan
    Write-Host ""
}

function Show-Help {
    Write-Host ""
    Write-Host "  Command-line usage:" -ForegroundColor White
    Write-Host ""
    Write-Host "    .\build.ps1" -ForegroundColor Green -NoNewline
    Write-Host "                  Interactive menu" -ForegroundColor DarkGray
    Write-Host "    .\build.ps1 -Clean" -ForegroundColor Green -NoNewline
    Write-Host "           Clean + build" -ForegroundColor DarkGray
    Write-Host "    .\build.ps1 -Run" -ForegroundColor Green -NoNewline
    Write-Host "             Build + launch Minecraft" -ForegroundColor DarkGray
    Write-Host "    .\build.ps1 -Clean -Run" -ForegroundColor Green -NoNewline
    Write-Host "      Clean + build + launch" -ForegroundColor DarkGray
    Write-Host "    .\build.ps1 -Jar" -ForegroundColor Green -NoNewline
    Write-Host "             Build distributable JAR only" -ForegroundColor DarkGray
    Write-Host "    .\build.ps1 -Help" -ForegroundColor Green -NoNewline
    Write-Host "            Show this help" -ForegroundColor DarkGray
    Write-Host ""
}

# -- Help flag --
if ($Help) {
    Show-Help
    exit 0
}

# -- Header --
Write-Host ""
Write-Host "==========================================" -ForegroundColor Yellow
Write-Host "  Off to Market (Trading Deluxe) Builder" -ForegroundColor Yellow
Write-Host "==========================================" -ForegroundColor Yellow

# -- Verify we're in the right directory --
if (-not (Test-Path "build.gradle")) {
    Write-Fail "build.gradle not found. Run this script from the mod root directory."
    exit 1
}

# -- Interactive menu when no flags passed --
$noFlags = -not ($Clean -or $Run -or $Jar)
if ($noFlags) {
    Show-Menu
    $choice = Read-Host "  Enter choice"
    switch ($choice) {
        "1" { $Clean = $false; $Run = $false; $Jar = $false }
        "2" { $Clean = $true;  $Run = $false; $Jar = $false }
        "3" { $Clean = $false; $Run = $true;  $Jar = $false }
        "4" { $Clean = $true;  $Run = $true;  $Jar = $false }
        "5" { $Clean = $false; $Run = $false; $Jar = $true  }
        "6" {
            Write-Step "Launching Minecraft client..."
            .\gradlew.bat runClient --no-daemon
            exit 0
        }
        { $_ -eq "H" -or $_ -eq "h" } { Show-Help; exit 0 }
        { $_ -eq "Q" -or $_ -eq "q" } { Write-Host ""; exit 0 }
        default {
            Write-Fail "Invalid choice. Exiting."
            exit 1
        }
    }
}

$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()

# -- Clean --
if ($Clean) {
    Write-Step "Cleaning previous build artifacts..."
    .\gradlew.bat clean --no-daemon 2>&1 | Out-Null
    if ($LASTEXITCODE -eq 0) {
        Write-Success "Clean complete."
    } else {
        Write-Fail "Clean failed."
        exit 1
    }
}

# -- Build --
if ($Jar) {
    Write-Step "Building distributable mod JAR..."
    $buildOutput = .\gradlew.bat jar --no-daemon 2>&1
} else {
    Write-Step "Compiling and building mod..."
    $buildOutput = .\gradlew.bat build --no-daemon 2>&1
}
$buildExitCode = $LASTEXITCODE

# Show warnings/errors from build output
$buildOutput | ForEach-Object {
    $line = $_
    if ($line -match "error:|ERROR") {
        Write-Host "   $line" -ForegroundColor Red
    } elseif ($line -match "warning:|WARNING") {
        Write-Host "   $line" -ForegroundColor Yellow
    }
}

if ($buildExitCode -ne 0) {
    Write-Fail "Build failed! Check errors above."
    Write-Host ""
    Write-Host "Full output:" -ForegroundColor DarkGray
    $buildOutput | Write-Host
    exit 1
}

Write-Success "Build succeeded."

# -- Find the output JAR --
$jarDir = "build\libs"
if (Test-Path $jarDir) {
    $jars = Get-ChildItem $jarDir -Filter "*.jar" | Sort-Object LastWriteTime -Descending
    if ($jars.Count -gt 0) {
        $latestJar = $jars[0]
        Write-Step "Output JAR"
        Write-Host "   $($latestJar.FullName)" -ForegroundColor White
        Write-Host "   Size: $([math]::Round($latestJar.Length / 1KB, 1)) KB" -ForegroundColor DarkGray
    }
}

# -- Elapsed time --
$stopwatch.Stop()
$elapsed = $stopwatch.Elapsed
Write-Host ""
Write-Host "   Completed in $($elapsed.Minutes)m $($elapsed.Seconds)s" -ForegroundColor DarkGray

# -- Launch client --
if ($Run) {
    Write-Step "Launching Minecraft client with mod..."
    .\gradlew.bat runClient --no-daemon
}

Write-Host ""
Write-Success "Done!"
Write-Host ""
