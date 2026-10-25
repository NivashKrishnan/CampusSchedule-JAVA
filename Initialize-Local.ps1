param([Parameter(Mandatory=$true)][string]$SqlExport,[string]$RuntimeHome="$env:USERPROFILE\Downloads\server")
. (Join-Path $PSScriptRoot 'Local-Common.ps1')
$SqlExport=(Resolve-Path -LiteralPath $SqlExport).Path
if(Test-Path -LiteralPath $localState){throw '.local already exists. Initialization will not overwrite an existing database or credentials.'}
if(Test-LocalPort 3306){throw 'Port 3306 is occupied. No existing database was modified.'}
$RuntimeHome=(Resolve-Path -LiteralPath $RuntimeHome).Path
$jdk=Get-ChildItem -LiteralPath $RuntimeHome -Directory | Where-Object {Test-Path -LiteralPath (Join-Path $_.FullName 'bin/javac.exe')} | Select-Object -First 1
$tomcat=Get-ChildItem -LiteralPath $RuntimeHome -Directory | Where-Object {Test-Path -LiteralPath (Join-Path $_.FullName 'lib/servlet-api.jar')} | Select-Object -First 1
$db=Get-ChildItem -LiteralPath $RuntimeHome -Directory | Where-Object {Test-Path -LiteralPath (Join-Path $_.FullName 'bin/mysqld.exe')} | Select-Object -First 1
if(-not $jdk -or -not $tomcat -or -not $db){throw 'Compatible JDK, Tomcat and MariaDB directories are required; nothing will be installed automatically.'}
New-Item -ItemType Directory -Path $localState | Out-Null
$identity=[Security.Principal.WindowsIdentity]::GetCurrent().Name
& icacls.exe $localState /inheritance:r /grant:r ($identity+':(OI)(CI)F') '*S-1-5-18:(OI)(CI)F' | Out-Null
if($LASTEXITCODE -ne 0){throw 'Could not protect the private local state directory.'}
@{runtimeHome=$RuntimeHome;jdk=$jdk.FullName;tomcat=$tomcat.FullName;mariadb=$db.FullName;dbPort=3306;httpPort=8080} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $localState 'runtime.json') -Encoding UTF8
function New-RandomPassword {
    $bytes=New-Object byte[] 32
    $rng=[Security.Cryptography.RandomNumberGenerator]::Create()
    try{$rng.GetBytes($bytes);return [BitConverter]::ToString($bytes).Replace('-','')}finally{$rng.Dispose()}
}
$rootPassword=New-RandomPassword
$appPassword=New-RandomPassword
$rootCredential=New-Object Management.Automation.PSCredential('root',(ConvertTo-SecureString $rootPassword -AsPlainText -Force))
$appCredential=New-Object Management.Automation.PSCredential('timetable_local',(ConvertTo-SecureString $appPassword -AsPlainText -Force))
$rootCredential | Export-Clixml -LiteralPath (Join-Path $localState 'admin.credential.xml')
$appCredential | Export-Clixml -LiteralPath (Join-Path $localState 'app.credential.xml')
& (Join-Path $db.FullName 'bin/mysql_install_db.exe') ('--datadir='+(Join-Path $localState 'data')) --port=3306 ('--password='+$rootPassword) *> (Join-Path $localState 'initialize.log')
if($LASTEXITCODE -ne 0){throw 'MariaDB data initialization failed; see private initialize.log.'}
Start-LocalDatabase
$previousPwd=$env:MYSQL_PWD
$previousEncoding=$OutputEncoding
$env:MYSQL_PWD=$rootPassword
$OutputEncoding=New-Object System.Text.UTF8Encoding($false)
try {
    $client=Join-Path $db.FullName 'bin/mysql.exe'
    $clientArgs=@('--protocol=tcp','--host=127.0.0.1','--port=3306','--user=root','--default-character-set=utf8mb4')
    'CREATE DATABASE timetable_project CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;' | & $client @clientArgs
    if($LASTEXITCODE -ne 0){throw 'Database already exists or could not be created. Import stopped without replacing data.'}
    Get-Content -Raw -Encoding UTF8 -LiteralPath $SqlExport | & $client @clientArgs '--database=timetable_project'
    if($LASTEXITCODE -ne 0){throw 'Import failed. The original export and source database remain unchanged.'}
    $userSql="CREATE USER 'timetable_local'@'localhost' IDENTIFIED BY '$appPassword'; CREATE USER 'timetable_local'@'127.0.0.1' IDENTIFIED BY '$appPassword'; GRANT SELECT,INSERT,UPDATE,DELETE ON timetable_project.* TO 'timetable_local'@'localhost'; GRANT SELECT,INSERT,UPDATE,DELETE ON timetable_project.* TO 'timetable_local'@'127.0.0.1';"
    $userSql | & $client @clientArgs
    if($LASTEXITCODE -ne 0){throw 'Dedicated application user creation failed.'}
    'SELECT COUNT(*) AS imported_tables FROM information_schema.tables WHERE table_schema="timetable_project";' | & $client @clientArgs
    if($LASTEXITCODE -ne 0){throw 'Imported schema verification failed.'}
    'ready' | Set-Content -LiteralPath (Join-Path $localState 'initialized')
} finally {
    $env:MYSQL_PWD=$previousPwd
    $OutputEncoding=$previousEncoding
    $rootPassword=$null;$appPassword=$null;$userSql=$null
}
Write-Host 'Imported the existing database on localhost:3306. Credentials are protected for this Windows account.'
