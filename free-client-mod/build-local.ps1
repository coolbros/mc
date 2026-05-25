$ErrorActionPreference = "Stop"

$MinecraftDir = Join-Path $env:APPDATA ".minecraft"
$JavaHome = Join-Path $MinecraftDir "runtime\java-runtime-epsilon\windows-x64\java-runtime-epsilon"
$Javac = Join-Path $JavaHome "bin\javac.exe"
$Jar = Join-Path $JavaHome "bin\jar.exe"
$MinecraftJar = Join-Path $MinecraftDir "versions\26.1.2\26.1.2.jar"
$ModsDir = Join-Path $MinecraftDir "mods"

if (!(Test-Path $Javac)) { throw "javac.exe not found at $Javac" }
if (!(Test-Path $MinecraftJar)) { throw "Minecraft 26.1.2 jar not found at $MinecraftJar" }

$FabricLoaderJar = Get-ChildItem (Join-Path $MinecraftDir "libraries\net\fabricmc\fabric-loader") -Recurse -Filter "fabric-loader-*.jar" -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if ($null -eq $FabricLoaderJar) {
    throw "Fabric Loader jar not found. Run install-fabric-and-build.ps1 first."
}

$BuildDir = Join-Path $PSScriptRoot "build"
$ClassesDir = Join-Path $BuildDir "classes"
$JarPath = Join-Path $BuildDir "smp-free-bot-1.0.0.jar"

if (Test-Path $BuildDir) {
    Remove-Item $BuildDir -Recurse -Force
}
New-Item -ItemType Directory -Path $ClassesDir | Out-Null

$Sources = Get-ChildItem (Join-Path $PSScriptRoot "src\main\java") -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
$LibraryJars = Get-ChildItem (Join-Path $MinecraftDir "libraries") -Recurse -Filter "*.jar" | ForEach-Object { $_.FullName }
$Classpath = @($MinecraftJar, $FabricLoaderJar.FullName) + $LibraryJars
$ClasspathText = $Classpath -join ";"
$SourceText = $Sources -join "`r`n"
$ArgFile = Join-Path $BuildDir "javac.args"

@"
--release
25
-encoding
UTF-8
-cp
$ClasspathText
-d
$ClassesDir
$SourceText
"@ | Set-Content -Path $ArgFile -Encoding ASCII

& $Javac "@$ArgFile"
if ($LASTEXITCODE -ne 0) {
    throw "javac failed with exit code $LASTEXITCODE"
}

Copy-Item (Join-Path $PSScriptRoot "src\main\resources\*") $ClassesDir -Recurse
& $Jar --create --file $JarPath -C $ClassesDir .
if ($LASTEXITCODE -ne 0) {
    throw "jar failed with exit code $LASTEXITCODE"
}

New-Item -ItemType Directory -Path $ModsDir -Force | Out-Null
Copy-Item $JarPath (Join-Path $ModsDir "smp-free-bot-1.0.0.jar") -Force

Write-Host "Built: $JarPath"
Write-Host "Installed to: $(Join-Path $ModsDir "smp-free-bot-1.0.0.jar")"
