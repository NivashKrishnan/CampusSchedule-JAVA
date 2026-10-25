param([switch]$NoBrowser)
. (Join-Path $PSScriptRoot 'Local-Common.ps1')
$runtime=Get-LocalRuntime
if(-not(Test-Path -LiteralPath (Join-Path $localState 'initialized'))){throw 'Local database initialization has not completed.'}
Start-LocalDatabase
if(Test-LocalPort 8080){
    if(-not(Get-OwnedProcess 'server')){throw 'Port 8080 belongs to another app. No existing server was stopped.'}
    Write-Host 'Project is already running at http://localhost:8080/Timetable/'
    if(-not $NoBrowser){Start-Process 'http://localhost:8080/Timetable/'}
    return
}
& (Join-Path $PSScriptRoot 'Build.ps1') -RuntimeHome $runtime.runtimeHome
$serverBase=Join-Path $localState 'tomcat'
foreach($name in @('conf','logs','temp','webapps','work')){New-Item -ItemType Directory -Force -Path (Join-Path $serverBase $name) | Out-Null}
Copy-Item -Path (Join-Path $runtime.tomcat 'conf/*') -Destination (Join-Path $serverBase 'conf') -Recurse -Force
@'
<Server port="-1"><Service name="Catalina"><Connector address="127.0.0.1" port="8080" protocol="HTTP/1.1" connectionTimeout="20000" URIEncoding="UTF-8"/><Engine name="Catalina" defaultHost="localhost"><Host name="localhost" appBase="webapps" unpackWARs="true" autoDeploy="false"/></Engine></Service></Server>
'@ | Set-Content -LiteralPath (Join-Path $serverBase 'conf/server.xml') -Encoding UTF8
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'Timetable.war') -Destination (Join-Path $serverBase 'webapps/Timetable.war') -Force
$classPath=(Join-Path $runtime.tomcat 'bin/bootstrap.jar')+';'+(Join-Path $runtime.tomcat 'bin/tomcat-juli.jar')
$javaArgs=@('-Dfile.encoding=UTF-8',('-Dcatalina.home="'+$runtime.tomcat+'"'),('-Dcatalina.base="'+$serverBase+'"'),('-Djava.io.tmpdir="'+(Join-Path $serverBase 'temp')+'"'),'-cp',('"'+$classPath+'"'),'org.apache.catalina.startup.Bootstrap','start')
$credential=Import-Clixml -LiteralPath (Join-Path $localState 'app.credential.xml')
$priorUrl=$env:TIMETABLE_DB_URL;$priorUser=$env:TIMETABLE_DB_USER;$priorPassword=$env:TIMETABLE_DB_PASSWORD
try {
    $env:TIMETABLE_DB_URL='jdbc:mysql://localhost:3306/timetable_project?useUnicode=true&characterEncoding=UTF-8'
    $env:TIMETABLE_DB_USER=$credential.UserName
    $env:TIMETABLE_DB_PASSWORD=$credential.GetNetworkCredential().Password
    $p=Start-Process -FilePath (Join-Path $runtime.jdk 'bin/java.exe') -ArgumentList $javaArgs -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $localState 'server.log') -RedirectStandardError (Join-Path $localState 'server-error.log')
    $p.Id | Set-Content -LiteralPath (Join-Path $localState 'server.pid')
} finally {$env:TIMETABLE_DB_URL=$priorUrl;$env:TIMETABLE_DB_USER=$priorUser;$env:TIMETABLE_DB_PASSWORD=$priorPassword}
for($i=0;$i -lt 60;$i++){
    try{$response=Invoke-WebRequest 'http://127.0.0.1:8080/Timetable/' -UseBasicParsing -TimeoutSec 10;if($response.StatusCode -eq 200){break}}catch{}
    Start-Sleep -Milliseconds 500
}
if($i -eq 60){throw 'App startup did not complete. See .local/server-error.log.'}
Write-Host 'Running: http://localhost:8080/Timetable/ | MariaDB: localhost:3306'
if(-not $NoBrowser){Start-Process 'http://localhost:8080/Timetable/'}
