param([switch]$KeepDatabase)
. (Join-Path $PSScriptRoot 'Local-Common.ps1')
$names=if($KeepDatabase){@('server')}else{@('server','database')}
foreach($name in $names){
    $p=Get-OwnedProcess $name
    if(-not $p){continue}
    if($name -eq 'database'){
        $runtime=Get-LocalRuntime
        $credential=Import-Clixml -LiteralPath (Join-Path $localState 'admin.credential.xml')
        $previousPwd=$env:MYSQL_PWD
        try{
            $env:MYSQL_PWD=$credential.GetNetworkCredential().Password
            & (Join-Path $runtime.mariadb 'bin/mysqladmin.exe') --host=127.0.0.1 --port=3306 --user=root shutdown
            if($LASTEXITCODE -ne 0){throw 'MariaDB did not shut down cleanly.'}
        }finally{$env:MYSQL_PWD=$previousPwd}
    }else{Stop-Process -Id $p.ProcessId}
    Write-Host "$name stopped."
}
