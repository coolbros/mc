param(
    [ValidateSet("auto", "wood", "mine", "stop")]
    [string] $Mode = "stop",

    [int] $Radius = 10,

    [string] $Server = "dauntless31-Rvm4.aternos.me:25565",

    [switch] $NoAutoJoin
)

$MinecraftDir = Join-Path $env:APPDATA ".minecraft"
$ControlFile = Join-Path $MinecraftDir "smp-free-bot-control.txt"
$Radius = [Math]::Max(4, [Math]::Min(24, $Radius))
$AutoJoin = if ($NoAutoJoin) { "false" } else { "true" }

@"
mode=$Mode
radius=$Radius
server=$Server
autojoin=$AutoJoin
"@ | Set-Content -Path $ControlFile -Encoding ASCII

Write-Host "Wrote $ControlFile"
Write-Host "mode=$Mode radius=$Radius server=$Server autojoin=$AutoJoin"
