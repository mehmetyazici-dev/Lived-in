# Dot-source before using Gradle: . .\scripts\dev-env.ps1
$livedInRoot = Split-Path -Parent $PSScriptRoot
$livedInJdk = Join-Path $livedInRoot '.tools\jdk-21.0.12.1+1'
if (-not (Test-Path -LiteralPath (Join-Path $livedInJdk 'bin\javac.exe'))) {
    throw "Microsoft JDK 21 is missing from $livedInJdk. See README.md for setup."
}
$env:JAVA_HOME = $livedInJdk
$livedInJavaBin = Join-Path $livedInJdk 'bin'
if (($env:Path -split ';') -notcontains $livedInJavaBin) {
    $env:Path = "$livedInJavaBin;$env:Path"
}
$env:GRADLE_USER_HOME = Join-Path $livedInRoot '.gradle-user-home'
Write-Host "Lived-In development environment: Java 21 at $env:JAVA_HOME"
