$ErrorActionPreference = "Stop"

$MinecraftDir = Join-Path $env:APPDATA ".minecraft"
$ModsDir = Join-Path $MinecraftDir "mods"
$Java = Join-Path $MinecraftDir "runtime\java-runtime-epsilon\windows-x64\java-runtime-epsilon\bin\java.exe"
$Downloads = Join-Path $PSScriptRoot "downloads"

New-Item -ItemType Directory -Path $Downloads -Force | Out-Null
New-Item -ItemType Directory -Path $ModsDir -Force | Out-Null

if (!(Test-Path $Java)) {
    throw "Java 25 was not found at $Java. Launch Minecraft 26.1.2 once through SKLauncher first."
}

$Installer = Join-Path $Downloads "fabric-installer.jar"
if (!(Test-Path $Installer)) {
    Invoke-WebRequest `
        -Uri "https://maven.fabricmc.net/net/fabricmc/fabric-installer/1.0.3/fabric-installer-1.0.3.jar" `
        -OutFile $Installer `
        -UseBasicParsing
}

& $Java -jar $Installer client -dir $MinecraftDir -mcversion 26.1.2 -loader 0.19.2 -noprofile
if ($LASTEXITCODE -ne 0) {
    throw "Fabric installer failed with exit code $LASTEXITCODE"
}

& (Join-Path $PSScriptRoot "build-local.ps1")

Write-Host ""
Write-Host "Next: choose the Fabric 26.1.2 profile in SKLauncher and launch Minecraft."
