param(
    [string]$SdkRoot=(Join-Path $PSScriptRoot '.local/android-sdk'),
    [string]$JdkRoot='C:/Program Files/Eclipse Adoptium/jdk-17.0.20.101-hotspot'
)
$ErrorActionPreference='Stop'
$taskTools=Join-Path $SdkRoot 'build-tools/35.0.0'
$taskAndroid=Join-Path $SdkRoot 'platforms/android-35/android.jar'
$taskJava=Join-Path $JdkRoot 'bin/java.exe'
$taskBuild=Join-Path $PSScriptRoot ('build/'+(Get-Date -Format 'yyyyMMdd-HHmmss'))
$taskPrivate=Join-Path $PSScriptRoot '.local/signing'
foreach($taskDir in @($taskBuild,"$taskBuild/classes","$taskBuild/dex",$taskPrivate)){New-Item -ItemType Directory -Force -Path $taskDir | Out-Null}
if(!(Test-Path -LiteralPath $taskAndroid)){throw 'Android platform 35 is missing.'}
function Confirm-Exit([string]$taskStep){if($LASTEXITCODE -ne 0){throw "$taskStep failed ($LASTEXITCODE)."}}
& (Join-Path $taskTools 'aapt2.exe') compile --dir (Join-Path $PSScriptRoot 'res') -o "$taskBuild/resources.zip"
Confirm-Exit 'Resource compilation'
& (Join-Path $taskTools 'aapt2.exe') link -o "$taskBuild/base.apk" --manifest (Join-Path $PSScriptRoot 'AndroidManifest.xml') -I $taskAndroid "$taskBuild/resources.zip"
Confirm-Exit 'Manifest/resource packaging'
$taskSources=@(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'src') -Recurse -Filter '*.java' | ForEach-Object FullName)
& (Join-Path $JdkRoot 'bin/javac.exe') --release 8 -encoding UTF-8 -cp $taskAndroid -d "$taskBuild/classes" @taskSources
Confirm-Exit 'Java compilation'
$taskClasses=@(Get-ChildItem -LiteralPath "$taskBuild/classes" -Recurse -Filter '*.class' | ForEach-Object FullName)
& $taskJava -cp (Join-Path $taskTools 'lib/d8.jar') com.android.tools.r8.D8 --release --min-api 26 --lib $taskAndroid --output "$taskBuild/dex" @taskClasses
Confirm-Exit 'DEX compilation'
& (Join-Path $JdkRoot 'bin/jar.exe') uf "$taskBuild/base.apk" -C "$taskBuild/dex" classes.dex
Confirm-Exit 'APK assembly'
& (Join-Path $taskTools 'zipalign.exe') -f -p 4 "$taskBuild/base.apk" "$taskBuild/aligned.apk"
Confirm-Exit 'APK alignment'
$taskKey=Join-Path $taskPrivate 'campusschedule.p12'
$taskCredential=Join-Path $taskPrivate 'signing.credential.xml'
$taskIdentity=[Security.Principal.WindowsIdentity]::GetCurrent().Name
& icacls.exe $taskPrivate /inheritance:r /grant:r "${taskIdentity}:(OI)(CI)F" 'SYSTEM:(OI)(CI)F' | Out-Null
Confirm-Exit 'Private signing folder protection'
if((Test-Path -LiteralPath $taskKey) -and !(Test-Path -LiteralPath $taskCredential)){throw 'Signing key password file missing; do not replace the key.'}
if(!(Test-Path -LiteralPath $taskCredential)){
    $taskBytes=New-Object byte[] 32
    $taskRng=[Security.Cryptography.RandomNumberGenerator]::Create();$taskRng.GetBytes($taskBytes);$taskRng.Dispose()
    $taskPassword=[Convert]::ToBase64String($taskBytes)
    $taskSecure=ConvertTo-SecureString $taskPassword -AsPlainText -Force
    $taskSecret=New-Object Management.Automation.PSCredential('campusschedule',$taskSecure)
    $taskSecret | Export-Clixml -LiteralPath $taskCredential
}
$taskSaved=Import-Clixml -LiteralPath $taskCredential
$env:TIMETABLE_SIGN_PASS=$taskSaved.GetNetworkCredential().Password
try{
    if(!(Test-Path -LiteralPath $taskKey)){
        & (Join-Path $JdkRoot 'bin/keytool.exe') -genkeypair -noprompt -keystore $taskKey -storetype PKCS12 -storepass:env TIMETABLE_SIGN_PASS -keypass:env TIMETABLE_SIGN_PASS -alias campusschedule -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=CampusSchedule App'
        Confirm-Exit 'Signing key creation'
    }
    $taskApk=Join-Path $taskBuild 'CampusSchedule-1.0.apk'
    & $taskJava -jar (Join-Path $taskTools 'lib/apksigner.jar') sign --ks $taskKey --ks-key-alias campusschedule --ks-pass env:TIMETABLE_SIGN_PASS --key-pass env:TIMETABLE_SIGN_PASS --out $taskApk "$taskBuild/aligned.apk"
    Confirm-Exit 'APK signing'
    & $taskJava -jar (Join-Path $taskTools 'lib/apksigner.jar') verify --verbose --print-certs $taskApk
    Confirm-Exit 'Signature verification'
    & (Join-Path $taskTools 'zipalign.exe') -c -p 4 $taskApk
    Confirm-Exit 'Alignment verification'
    & (Join-Path $taskTools 'aapt2.exe') dump badging $taskApk
    Confirm-Exit 'Manifest verification'
    Write-Output "APK_FILE=$taskApk"
}finally{$env:TIMETABLE_SIGN_PASS=$null}
