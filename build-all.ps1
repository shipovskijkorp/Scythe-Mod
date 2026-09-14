$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$Release = Join-Path $Root 'build\release'

if (Test-Path $Release) { Remove-Item $Release -Recurse -Force }
New-Item -ItemType Directory -Force -Path $Release | Out-Null

foreach ($Family in @('legacy', 'modern', 'current')) {
    Write-Host "==> Building Scythe Mod family: $Family"
    $BuildRoot = Join-Path $Root "builds\$Family"
    Push-Location $BuildRoot
    try {
        & .\gradlew.bat buildAndCollect
        if ($LASTEXITCODE -ne 0) { throw "Gradle failed for $Family with exit code $LASTEXITCODE" }
    }
    finally {
        Pop-Location
    }
}

Write-Host '==> Release jars'
Get-ChildItem $Release -Filter '*.jar' | ForEach-Object { Write-Host $_.FullName }
