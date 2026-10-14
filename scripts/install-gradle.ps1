$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$destination = Join-Path $projectRoot '.gradle-local'
$version = '9.2.1'
$gradleFolder = Join-Path $destination "gradle-$version"
if (Test-Path (Join-Path $gradleFolder 'bin\gradle.bat')) { exit 0 }
New-Item -ItemType Directory -Path $destination -Force | Out-Null
$archive = Join-Path $destination "gradle-$version-bin.zip"
Write-Host "Downloading Gradle $version..."
Invoke-WebRequest "https://services.gradle.org/distributions/gradle-$version-bin.zip" -OutFile $archive
Expand-Archive -Path $archive -DestinationPath $destination -Force
Remove-Item $archive
Write-Host "Gradle installed at $gradleFolder"
