$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'dev-env.ps1')
$livedInProject = Split-Path -Parent $PSScriptRoot
$livedInIdea = Join-Path $livedInProject '.tools\idea\bin\idea64.exe'
if (-not (Test-Path -LiteralPath $livedInIdea)) {
    throw "IntelliJ IDEA is missing from $livedInIdea. See README.md."
}
$livedInIdeaProperties = Join-Path $livedInProject '.tools\idea-local.properties'
# Keep disposable IDE caches local; an old Windows socket can block the default cache.
Set-Content -LiteralPath $livedInIdeaProperties -Value ('idea.system.path=' + ($livedInProject.Replace('\', '/') + '/.tools/idea-system')) -Encoding utf8
$env:IDEA_PROPERTIES = $livedInIdeaProperties
Start-Process -FilePath $livedInIdea -ArgumentList ('"' + $livedInProject + '"')
