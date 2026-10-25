param([string]$RuntimeHome="$env:USERPROFILE\Downloads\server")
$ErrorActionPreference='Stop'
& (Join-Path $PSScriptRoot 'Build.ps1') -RuntimeHome $RuntimeHome
$jdk=Get-ChildItem -LiteralPath $RuntimeHome -Directory | Where-Object {Test-Path -LiteralPath (Join-Path $_.FullName 'bin/javac.exe')} | Select-Object -First 1
$classes=Join-Path $PSScriptRoot 'WebContent/WEB-INF/classes'
$testClasses=Join-Path $PSScriptRoot 'test-classes'
New-Item -ItemType Directory -Force -Path $testClasses|Out-Null
& (Join-Path $jdk.FullName 'bin/javac.exe') -encoding UTF-8 -cp $classes -d $testClasses (Join-Path $PSScriptRoot 'tests/SchedulerCheck.java')
if($LASTEXITCODE -ne 0){throw 'Test compilation failed'}
& (Join-Path $jdk.FullName 'bin/java.exe') -cp ($classes+';'+$testClasses) SchedulerCheck
if($LASTEXITCODE -ne 0){throw 'Scheduler test failed'}
