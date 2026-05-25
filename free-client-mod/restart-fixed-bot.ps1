$ErrorActionPreference = "Stop"

$MinecraftDir = Join-Path $env:APPDATA ".minecraft"
$SetModeScript = Join-Path $PSScriptRoot "set-mode.ps1"

$GameProcesses = @(Get-CimInstance Win32_Process -Filter "name = 'javaw.exe'" |
    Where-Object { $_.CommandLine -like "*net.fabricmc.loader.impl.launch.knot.KnotClient*" } |
    Sort-Object ProcessId -Descending)

if ($GameProcesses.Count -eq 0) {
    throw "No running Fabric Minecraft process found to clone launch settings from."
}

$GameProcess = $GameProcesses[0]
$CommandLine = $GameProcess.CommandLine
$NativeMatch = [regex]::Match($CommandLine, "-Djava\.library\.path=([^ ]+)")
if (!$NativeMatch.Success) {
    throw "Could not find the native library folder in the Minecraft command line."
}

$NativeDir = $NativeMatch.Groups[1].Value
if (!(Test-Path $NativeDir)) {
    throw "Native library folder does not exist: $NativeDir"
}

$NewNativeDir = Join-Path (Split-Path $NativeDir -Parent) ("codex-natives-" + [DateTimeOffset]::Now.ToUnixTimeSeconds())
Copy-Item -LiteralPath $NativeDir -Destination $NewNativeDir -Recurse -Force
$CommandLine = $CommandLine.Replace($NativeDir, $NewNativeDir)

foreach ($Process in $GameProcesses) {
    Stop-Process -Id $Process.ProcessId -Force -ErrorAction SilentlyContinue
}
Start-Sleep -Seconds 2

& $SetModeScript auto 12

$ExeEnd = $CommandLine.IndexOf(".exe")
if ($ExeEnd -lt 0) {
    throw "Could not parse javaw.exe from the Minecraft command line."
}

$JavaExe = $CommandLine.Substring(0, $ExeEnd + 4).Trim('"')
$Arguments = $CommandLine.Substring($ExeEnd + 4).Trim()
Start-Process -FilePath $JavaExe -ArgumentList $Arguments -WorkingDirectory $MinecraftDir

Write-Host "Launched fixed bot using native folder: $NewNativeDir"
