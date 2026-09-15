$ErrorActionPreference = 'Stop'
$script = Join-Path $PSScriptRoot 'scripts/build_all.py'
if ($env:SCYTHE_PYTHON) {
    & $env:SCYTHE_PYTHON $script @args
} elseif ($env:PYTHON) {
    & $env:PYTHON $script @args
} elseif (Get-Command py -ErrorAction SilentlyContinue) {
    & py -3 $script @args
} else {
    & python $script @args
}
exit $LASTEXITCODE
