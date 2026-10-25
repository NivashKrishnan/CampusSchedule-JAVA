$ErrorActionPreference='Stop'
$localState=Join-Path $PSScriptRoot '.local'
function Get-LocalRuntime {
    $path=Join-Path $localState 'runtime.json'
    if(-not (Test-Path -LiteralPath $path)){throw 'Run Initialize-Local.ps1 with your SQL export first.'}
    Get-Content -Raw -LiteralPath $path | ConvertFrom-Json
}
function Test-LocalPort([int]$Port) {
    $client=New-Object Net.Sockets.TcpClient
    try{$client.Connect('127.0.0.1',$Port);return $true}catch{return $false}finally{$client.Dispose()}
}
function Get-OwnedProcess([string]$Name) {
    $file=Join-Path $localState ($Name+'.pid')
    if(-not(Test-Path -LiteralPath $file)){return $null}
    $taskPid=[int](Get-Content -LiteralPath $file)
    $p=Get-CimInstance Win32_Process -Filter "ProcessId=$taskPid" -ErrorAction SilentlyContinue
    if($p -and $p.CommandLine -like ('*'+$localState+'*')){return $p}
    return $null
}
function Start-LocalDatabase {
    $runtime=Get-LocalRuntime
    if(Test-LocalPort 3306){
        if(-not(Get-OwnedProcess 'database')){throw 'Port 3306 belongs to another process. No existing database was modified.'}
        return
    }
    $dbArgs=@('--no-defaults',('--basedir="'+$runtime.mariadb+'"'),('--datadir="'+(Join-Path $localState 'data')+'"'),'--port=3306','--bind-address=127.0.0.1','--console')
    $p=Start-Process -FilePath (Join-Path $runtime.mariadb 'bin/mysqld.exe') -ArgumentList $dbArgs -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $localState 'database.log') -RedirectStandardError (Join-Path $localState 'database-error.log')
    $p.Id | Set-Content -LiteralPath (Join-Path $localState 'database.pid')
    for($i=0;$i -lt 60 -and -not(Test-LocalPort 3306);$i++){Start-Sleep -Milliseconds 250}
    if(-not(Test-LocalPort 3306)){throw 'MariaDB did not start. See .local/database-error.log.'}
}
