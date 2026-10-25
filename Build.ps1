param([string]$RuntimeHome = "$env:USERPROFILE\Downloads\server")
$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$jdk = Get-ChildItem -LiteralPath $RuntimeHome -Directory | Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin/javac.exe') } | Select-Object -First 1
$tomcat = Get-ChildItem -LiteralPath $RuntimeHome -Directory | Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'lib/servlet-api.jar') } | Select-Object -First 1
if (-not $jdk -or -not $tomcat) { throw 'Java JDK and Tomcat 9 folders must be in RuntimeHome. See README.md.' }
$classes = Join-Path $projectRoot 'WebContent/WEB-INF/classes'
New-Item -ItemType Directory -Force -Path $classes | Out-Null
$sources = Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src/app') -Filter '*.java' | ForEach-Object { $_.FullName }
& (Join-Path $jdk.FullName 'bin/javac.exe') -encoding UTF-8 -source 8 -target 8 -cp (Join-Path $tomcat.FullName 'lib/servlet-api.jar') -d $classes @sources
if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed.' }
& (Join-Path $jdk.FullName 'bin/jar.exe') cf (Join-Path $projectRoot 'Timetable.war') -C (Join-Path $projectRoot 'WebContent') .
if ($LASTEXITCODE -ne 0) { throw 'WAR packaging failed.' }
Write-Host 'Build complete: Timetable.war'
