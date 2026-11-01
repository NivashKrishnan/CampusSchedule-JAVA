. (Join-Path $PSScriptRoot 'Start-Local.ps1') -NoBrowser
Write-Host "CampusSchedule is live at http://localhost:8080/Timetable/"
$serverPid = [int](Get-Content -Path (Join-Path $PSScriptRoot '.local\server.pid'))
$dbPid = [int](Get-Content -Path (Join-Path $PSScriptRoot '.local\database.pid'))
Wait-Process -Id $serverPid, $dbPid

