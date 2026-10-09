$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'dev-env.ps1')
$livedInProject = Split-Path -Parent $PSScriptRoot
$livedInIdea = Join-Path $livedInProject '.tools\idea\bin\idea64.exe'
if (-not (Test-Path -LiteralPath $livedInIdea)) {
    throw "IntelliJ IDEA is missing from $livedInIdea. See README.md."
}
Start-Process -FilePath $livedInIdea -ArgumentList ('"' + $livedInProject + '"')
