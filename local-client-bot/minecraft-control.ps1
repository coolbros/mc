param(
    [ValidateSet("ChopWood", "MineTunnel", "AntiIdle")]
    [string] $Mode = "MineTunnel",

    [int] $Seconds = 60
)

$ErrorActionPreference = "Stop"

Add-Type @"
using System;
using System.Runtime.InteropServices;

public static class WinInput {
    [DllImport("user32.dll")]
    public static extern bool SetForegroundWindow(IntPtr hWnd);

    [DllImport("user32.dll")]
    public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);

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
"@

$Keys = @{
    W = 0x57
    A = 0x41
    S = 0x53
    D = 0x44
    Space = 0x20
    Shift = 0x10
    Escape = 0x1B
}

function Get-MinecraftWindow {
    $windows = Get-Process javaw -ErrorAction SilentlyContinue |
        Where-Object { $_.MainWindowHandle -ne 0 -and $_.MainWindowTitle -match "Minecraft|SKlauncher" } |
        Sort-Object StartTime -Descending

    if (-not $windows) {
        throw "No visible Minecraft Java window found. Launch Minecraft with SKLauncher and join the SMP first."
    }

    return $windows[0]
}

function Focus-Minecraft {
    $window = Get-MinecraftWindow
    [WinInput]::ShowWindow($window.MainWindowHandle, [WinInput]::SW_RESTORE) | Out-Null
    Start-Sleep -Milliseconds 100
    [WinInput]::SetForegroundWindow($window.MainWindowHandle) | Out-Null
    Start-Sleep -Milliseconds 500
    Write-Host "Focused: $($window.MainWindowTitle)"
}

function Key-Down([string] $Key) {
    [WinInput]::keybd_event([byte] $Keys[$Key], 0, 0, [UIntPtr]::Zero)
}

function Key-Up([string] $Key) {
    [WinInput]::keybd_event([byte] $Keys[$Key], 0, [WinInput]::KEYEVENTF_KEYUP, [UIntPtr]::Zero)
}

function Tap-Key([string] $Key, [int] $Ms = 90) {
    Key-Down $Key
    Start-Sleep -Milliseconds $Ms
    Key-Up $Key
}

function Mouse-LeftDown {
    [WinInput]::mouse_event([WinInput]::MOUSEEVENTF_LEFTDOWN, 0, 0, 0, [UIntPtr]::Zero)
}

function Mouse-LeftUp {
    [WinInput]::mouse_event([WinInput]::MOUSEEVENTF_LEFTUP, 0, 0, 0, [UIntPtr]::Zero)
}

function Mouse-RightClick([int] $Ms = 90) {
    [WinInput]::mouse_event([WinInput]::MOUSEEVENTF_RIGHTDOWN, 0, 0, 0, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds $Ms
    [WinInput]::mouse_event([WinInput]::MOUSEEVENTF_RIGHTUP, 0, 0, 0, [UIntPtr]::Zero)
}

function Move-Mouse([int] $Dx, [int] $Dy) {
    [WinInput]::mouse_event([WinInput]::MOUSEEVENTF_MOVE, $Dx, $Dy, 0, [UIntPtr]::Zero)
}

function Release-All {
    Mouse-LeftUp
    foreach ($key in @("W", "A", "S", "D", "Space", "Shift")) {
        Key-Up $key
    }
}

function Run-ChopWood([int] $DurationSeconds) {
    Write-Host "ChopWood starting. Face a tree trunk first. Stop with Ctrl+C."
    Mouse-LeftDown

    $end = (Get-Date).AddSeconds($DurationSeconds)
    $turn = 22
    while ((Get-Date) -lt $end) {
        Key-Down W
        Start-Sleep -Milliseconds 550
        Key-Up W

        Move-Mouse 0 -90
        Start-Sleep -Milliseconds 700
        Move-Mouse $turn 65
        Start-Sleep -Milliseconds 700
        Move-Mouse (-1 * $turn) 25

        Tap-Key Space 100
        $turn = -1 * $turn
    }
}

function Run-MineTunnel([int] $DurationSeconds) {
    Write-Host "MineTunnel starting. Equip a pickaxe and face stone first. Stop with Ctrl+C."
    Mouse-LeftDown
    Key-Down W

    $end = (Get-Date).AddSeconds($DurationSeconds)
    $sweep = 18
    $counter = 0
    while ((Get-Date) -lt $end) {
        Move-Mouse $sweep 0
        Start-Sleep -Milliseconds 600
        Move-Mouse (-1 * $sweep) 0
        Start-Sleep -Milliseconds 600

        $counter++
        if ($counter % 5 -eq 0) {
            Tap-Key Space 120
        }

        if ($counter % 11 -eq 0) {
            Key-Up W
            Key-Down Shift
            Start-Sleep -Milliseconds 350
            Key-Up Shift
            Key-Down W
        }
    }
}

function Run-AntiIdle([int] $DurationSeconds) {
    Write-Host "AntiIdle starting. Stop with Ctrl+C."
    $end = (Get-Date).AddSeconds($DurationSeconds)
    while ((Get-Date) -lt $end) {
        Tap-Key Space 120
        Move-Mouse 10 0
        Start-Sleep -Seconds 8
        Move-Mouse -10 0
        Start-Sleep -Seconds 8
    }
}

try {
    Focus-Minecraft
    Start-Sleep -Seconds 2

    switch ($Mode) {
        "ChopWood" { Run-ChopWood $Seconds }
        "MineTunnel" { Run-MineTunnel $Seconds }
        "AntiIdle" { Run-AntiIdle $Seconds }
    }
} finally {
    Release-All
    Write-Host "Stopped local Minecraft controller."
}
