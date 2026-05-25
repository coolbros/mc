param(
    [ValidateSet("WoodToIron", "ChopWood", "MineStoneIron", "AntiStuck")]
    [string] $Goal = "WoodToIron",

    [ValidateSet("Anthropic", "Kimi")]
    [string] $Provider = "Anthropic",

    [int] $Steps = 80,
    [int] $DelayMs = 450,

    [string] $Model = "",
    [string] $ApiBase = "",
    [string] $StopFile = ".\STOP_BOT"
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($Model)) {
    $Model = $env:KIMI_MODEL
}
if ([string]::IsNullOrWhiteSpace($Model)) {
    $Model = $env:ANTHROPIC_MODEL
}
if ([string]::IsNullOrWhiteSpace($Model)) {
    $Model = if ($Provider -eq "Anthropic") { "claude-3-5-haiku-20241022" } else { "kimi-k2.6" }
}

if ([string]::IsNullOrWhiteSpace($ApiBase)) {
    $ApiBase = if ($Provider -eq "Anthropic") { "https://api.anthropic.com/v1" } else { "https://api.moonshot.ai/v1" }
}

$ApiKey = if ($Provider -eq "Anthropic") { $env:ANTHROPIC_API_KEY } else { $env:KIMI_API_KEY }
if ($Provider -eq "Kimi" -and [string]::IsNullOrWhiteSpace($ApiKey)) {
    $ApiKey = $env:MOONSHOT_API_KEY
}
if ([string]::IsNullOrWhiteSpace($ApiKey)) {
    if ($Provider -eq "Anthropic") {
        throw "Set ANTHROPIC_API_KEY first. Example: `$env:ANTHROPIC_API_KEY='your-key-here'"
    }
    throw "Set KIMI_API_KEY or MOONSHOT_API_KEY first. Example: `$env:KIMI_API_KEY='your-key-here'"
}

Add-Type -AssemblyName System.Drawing
Add-Type @"
using System;
using System.Runtime.InteropServices;

public static class VisionBotWin32 {
    [DllImport("user32.dll")]
    public static extern bool SetForegroundWindow(IntPtr hWnd);

    [DllImport("user32.dll")]
    public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);

    [DllImport("user32.dll")]
    public static extern bool GetWindowRect(IntPtr hWnd, out Rect lpRect);

    [DllImport("user32.dll")]
    public static extern void keybd_event(byte bVk, byte bScan, uint dwFlags, UIntPtr dwExtraInfo);

    [DllImport("user32.dll")]
    public static extern void mouse_event(uint dwFlags, int dx, int dy, uint dwData, UIntPtr dwExtraInfo);

    public const int SW_RESTORE = 9;
    public const uint KEYEVENTF_KEYUP = 0x0002;
    public const uint MOUSEEVENTF_LEFTDOWN = 0x0002;
    public const uint MOUSEEVENTF_LEFTUP = 0x0004;
    public const uint MOUSEEVENTF_RIGHTDOWN = 0x0008;
    public const uint MOUSEEVENTF_RIGHTUP = 0x0010;
    public const uint MOUSEEVENTF_MOVE = 0x0001;
}

[StructLayout(LayoutKind.Sequential)]
public struct Rect {
    public int Left;
    public int Top;
    public int Right;
    public int Bottom;
}
"@

$Keys = @{
    W = 0x57
    A = 0x41
    S = 0x53
    D = 0x44
    Space = 0x20
    Shift = 0x10
    Ctrl = 0x11
    E = 0x45
    One = 0x31
    Two = 0x32
    Three = 0x33
    Four = 0x34
    Five = 0x35
    Six = 0x36
    Seven = 0x37
    Eight = 0x38
    Nine = 0x39
    Escape = 0x1B
}

$AllowedKeys = @("W", "A", "S", "D", "Space", "Shift", "Ctrl", "E", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Escape")
$HeldKeys = New-Object System.Collections.Generic.HashSet[string]

function Get-MinecraftWindow {
    $windows = Get-Process javaw -ErrorAction SilentlyContinue |
        Where-Object { $_.MainWindowHandle -ne 0 -and $_.MainWindowTitle -match "Minecraft|SKlauncher" } |
        Sort-Object StartTime -Descending

    if (-not $windows) {
        throw "No visible Minecraft Java window found. Launch Minecraft through SKLauncher and join the SMP first."
    }

    return $windows[0]
}

function Focus-Minecraft {
    param($Window)
    [VisionBotWin32]::ShowWindow($Window.MainWindowHandle, [VisionBotWin32]::SW_RESTORE) | Out-Null
    Start-Sleep -Milliseconds 100
    [VisionBotWin32]::SetForegroundWindow($Window.MainWindowHandle) | Out-Null
    Start-Sleep -Milliseconds 250
}

function Key-Down([string] $Key) {
    if (-not $Keys.ContainsKey($Key)) {
        return
    }
    [VisionBotWin32]::keybd_event([byte] $Keys[$Key], 0, 0, [UIntPtr]::Zero)
    $HeldKeys.Add($Key) | Out-Null
}

function Key-Up([string] $Key) {
    if (-not $Keys.ContainsKey($Key)) {
        return
    }
    [VisionBotWin32]::keybd_event([byte] $Keys[$Key], 0, [VisionBotWin32]::KEYEVENTF_KEYUP, [UIntPtr]::Zero)
    $HeldKeys.Remove($Key) | Out-Null
}

function Release-All {
    [VisionBotWin32]::mouse_event([VisionBotWin32]::MOUSEEVENTF_LEFTUP, 0, 0, 0, [UIntPtr]::Zero)
    [VisionBotWin32]::mouse_event([VisionBotWin32]::MOUSEEVENTF_RIGHTUP, 0, 0, 0, [UIntPtr]::Zero)
    foreach ($key in @($HeldKeys)) {
        Key-Up $key
    }
}

function Tap-Key([string] $Key, [int] $Ms) {
    Key-Down $Key
    Start-Sleep -Milliseconds ([Math]::Min([Math]::Max($Ms, 40), 650))
    Key-Up $Key
}

function Move-Mouse([int] $Dx, [int] $Dy) {
    $safeDx = [Math]::Min([Math]::Max($Dx, -160), 160)
    $safeDy = [Math]::Min([Math]::Max($Dy, -120), 120)
    [VisionBotWin32]::mouse_event([VisionBotWin32]::MOUSEEVENTF_MOVE, $safeDx, $safeDy, 0, [UIntPtr]::Zero)
}

function Capture-MinecraftPngBase64 {
    param($Window)

    $rect = New-Object Rect
    [VisionBotWin32]::GetWindowRect($Window.MainWindowHandle, [ref] $rect) | Out-Null
    $width = [Math]::Max(1, $rect.Right - $rect.Left)
    $height = [Math]::Max(1, $rect.Bottom - $rect.Top)

    $bitmap = New-Object System.Drawing.Bitmap $width, $height
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.CopyFromScreen($rect.Left, $rect.Top, 0, 0, $bitmap.Size)

    $targetWidth = 960
    if ($width -gt $targetWidth) {
        $scale = $targetWidth / $width
        $targetHeight = [int]($height * $scale)
        $small = New-Object System.Drawing.Bitmap $targetWidth, $targetHeight
        $smallGraphics = [System.Drawing.Graphics]::FromImage($small)
        $smallGraphics.DrawImage($bitmap, 0, 0, $targetWidth, $targetHeight)
        $bitmap.Dispose()
        $graphics.Dispose()
        $bitmap = $small
        $graphics = $smallGraphics
    }

    $stream = New-Object System.IO.MemoryStream
    $bitmap.Save($stream, [System.Drawing.Imaging.ImageFormat]::Png)
    $bytes = $stream.ToArray()

    $stream.Dispose()
    $graphics.Dispose()
    $bitmap.Dispose()

    return [Convert]::ToBase64String($bytes)
}

function Get-SystemPrompt {
    @"
You are driving a Minecraft survival player from screenshots. The user wants a useful hands-off helper, not a blind macro.

Goal: $Goal.

Act like a cautious Minecraft bot:
- Prefer looking at and mining/chopping the exact block under the crosshair.
- If collecting wood: find visible logs, center the crosshair, move closer carefully, hold mine only when the crosshair is on wood.
- If mining: mine stone/deepslate/iron ore, move forward only when the tunnel looks open, avoid lava, cliffs, mobs, and water.
- If crafting is needed but the inventory/crafting UI is not visible, ask for a small setup action instead of pretending.
- Never attack players. Never type chat. Never send commands.
- Keep actions short. You get another screenshot after every action.

Return only compact JSON with this schema:
{
  "thought": "short reason",
  "danger": "none|lava|mob|fall|water|stuck|unknown",
  "actions": [
    {"type":"look","dx":0,"dy":0},
    {"type":"tap","key":"W","ms":120},
    {"type":"hold","key":"W","ms":300},
    {"type":"mine","ms":700},
    {"type":"use","ms":120},
    {"type":"wait","ms":300}
  ]
}

Allowed keys: W, A, S, D, Space, Shift, Ctrl, E, One, Two, Three, Four, Five, Six, Seven, Eight, Nine, Escape.
Use at most 3 actions. ms must be between 40 and 1200. look dx/dy must be between -160 and 160.
"@
}

function Invoke-KimiVision {
    param([string] $ImageBase64, [int] $StepNumber)

    $headers = @{
        Authorization = "Bearer $ApiKey"
        "Content-Type" = "application/json"
    }

    $userText = "Step $StepNumber. Decide the next tiny action for Minecraft. Return JSON only."
    $body = @{
        model = $Model
        temperature = 0.1
        messages = @(
            @{
                role = "system"
                content = Get-SystemPrompt
            },
            @{
                role = "user"
                content = @(
                    @{
                        type = "text"
                        text = $userText
                    },
                    @{
                        type = "image_url"
                        image_url = @{
                            url = "data:image/png;base64,$ImageBase64"
                        }
                    }
                )
            }
        )
    } | ConvertTo-Json -Depth 12 -Compress

    $uri = "$($ApiBase.TrimEnd('/'))/chat/completions"
    $response = Invoke-RestMethod -Method Post -Uri $uri -Headers $headers -Body $body -TimeoutSec 60
    return $response.choices[0].message.content
}

function Invoke-AnthropicVision {
    param([string] $ImageBase64, [int] $StepNumber)

    $headers = @{
        "x-api-key" = $ApiKey
        "anthropic-version" = "2023-06-01"
        "Content-Type" = "application/json"
    }

    $userText = "Step $StepNumber. Decide the next tiny action for Minecraft. Return JSON only."
    $body = @{
        model = $Model
        max_tokens = 350
        temperature = 0.1
        system = Get-SystemPrompt
        messages = @(
            @{
                role = "user"
                content = @(
                    @{
                        type = "text"
                        text = $userText
                    },
                    @{
                        type = "image"
                        source = @{
                            type = "base64"
                            media_type = "image/png"
                            data = $ImageBase64
                        }
                    }
                )
            }
        )
    } | ConvertTo-Json -Depth 12 -Compress

    $uri = "$($ApiBase.TrimEnd('/'))/messages"
    $response = Invoke-RestMethod -Method Post -Uri $uri -Headers $headers -Body $body -TimeoutSec 60
    return ($response.content | Where-Object { $_.type -eq "text" } | Select-Object -First 1).text
}

function Invoke-VisionModel {
    param([string] $ImageBase64, [int] $StepNumber)

    if ($Provider -eq "Anthropic") {
        return Invoke-AnthropicVision -ImageBase64 $ImageBase64 -StepNumber $StepNumber
    }

    return Invoke-KimiVision -ImageBase64 $ImageBase64 -StepNumber $StepNumber
}

function Read-AgentJson {
    param([string] $Text)

    $trimmed = $Text.Trim()
    if ($trimmed.StartsWith('```')) {
        $trimmed = $trimmed -replace '^```json\s*', ''
        $trimmed = $trimmed -replace '^```\s*', ''
        $trimmed = $trimmed -replace '\s*```$', ''
    }

    $start = $trimmed.IndexOf("{")
    $end = $trimmed.LastIndexOf("}")
    if ($start -lt 0 -or $end -lt $start) {
        throw "Model did not return JSON: $Text"
    }

    return $trimmed.Substring($start, $end - $start + 1) | ConvertFrom-Json
}

function Invoke-BotAction {
    param($Action)

    $type = [string] $Action.type
    $ms = 250
    if ($null -ne $Action.ms) {
        $ms = [int] $Action.ms
    }
    $ms = [Math]::Min([Math]::Max($ms, 40), 1200)

    switch ($type) {
        "look" {
            $dx = 0
            $dy = 0
            if ($null -ne $Action.dx) { $dx = [int] $Action.dx }
            if ($null -ne $Action.dy) { $dy = [int] $Action.dy }
            Move-Mouse $dx $dy
        }
        "tap" {
            $key = [string] $Action.key
            if ($AllowedKeys -contains $key) {
                Tap-Key $key $ms
            }
        }
        "hold" {
            $key = [string] $Action.key
            if ($AllowedKeys -contains $key) {
                Key-Down $key
                Start-Sleep -Milliseconds $ms
                Key-Up $key
            }
        }
        "mine" {
            [VisionBotWin32]::mouse_event([VisionBotWin32]::MOUSEEVENTF_LEFTDOWN, 0, 0, 0, [UIntPtr]::Zero)
            Start-Sleep -Milliseconds $ms
            [VisionBotWin32]::mouse_event([VisionBotWin32]::MOUSEEVENTF_LEFTUP, 0, 0, 0, [UIntPtr]::Zero)
        }
        "use" {
            [VisionBotWin32]::mouse_event([VisionBotWin32]::MOUSEEVENTF_RIGHTDOWN, 0, 0, 0, [UIntPtr]::Zero)
            Start-Sleep -Milliseconds $ms
            [VisionBotWin32]::mouse_event([VisionBotWin32]::MOUSEEVENTF_RIGHTUP, 0, 0, 0, [UIntPtr]::Zero)
        }
        "wait" {
            Start-Sleep -Milliseconds $ms
        }
        default {
            Start-Sleep -Milliseconds 150
        }
    }
}

try {
    if (Test-Path $StopFile) {
        Remove-Item $StopFile -Force
    }

    $window = Get-MinecraftWindow
    Focus-Minecraft $window
    Write-Host "Vision bot focused: $($window.MainWindowTitle)"
    Write-Host "Provider: $Provider"
    Write-Host "Model: $Model"
    Write-Host "Goal: $Goal"
    Write-Host "Create $StopFile or press Ctrl+C to stop."
    Start-Sleep -Seconds 2

    for ($step = 1; $step -le $Steps; $step++) {
        if (Test-Path $StopFile) {
            Write-Host "Stop file found. Stopping."
            break
        }

        $window = Get-MinecraftWindow
        Focus-Minecraft $window
        $imageBase64 = Capture-MinecraftPngBase64 $window
        $raw = Invoke-VisionModel -ImageBase64 $imageBase64 -StepNumber $step
        $decision = Read-AgentJson $raw

        Write-Host ("[{0}/{1}] danger={2} {3}" -f $step, $Steps, $decision.danger, $decision.thought)

        foreach ($action in @($decision.actions)) {
            if (Test-Path $StopFile) {
                break
            }
            Invoke-BotAction $action
            Start-Sleep -Milliseconds $DelayMs
        }
    }
} finally {
    Release-All
    Write-Host "Vision bot stopped."
}
