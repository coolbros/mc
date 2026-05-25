# SMP Free Client Bot

This is the free, no-credit route: a Fabric client mod that runs inside your Minecraft client and reads real block/inventory state.

It targets your server's current version:

```text
Spigot 26.1.2
protocol 775
```

## What Works First

- `wood` mode: finds nearby logs, turns toward them, walks over, and mines them.
- `mine` mode: finds nearby stone/deepslate/iron ore, turns toward it, walks over, and mines it.
- `auto` mode: chops logs, tries recipe-book crafting for planks/table/sticks/pickaxes, then mines stone/iron.
- Breaking controls: starts each block break once, holds Minecraft's `key.attack`, and calls the same continuous mining tick vanilla uses until the block breaks.
- Auto-join: when the client is at the title screen and the mode is not `stop`, it connects to the configured SMP.
- Tool selection: switches to a pickaxe/axe in your hotbar when available.
- Safety: pauses when it sees nearby lava, water, fire, or magma.

## Not Done Yet

Crafting uses the vanilla recipe book and normal inventory packets. It may need the relevant recipes unlocked, which usually happens after you pick up logs/cobblestone.

## Install/Build

From this folder:

```powershell
powershell.exe -ExecutionPolicy Bypass -NoProfile -File .\install-fabric-and-build.ps1
```

Then launch the Fabric `26.1.2` profile in SKLauncher.

## Control

The mod watches this file:

```text
%APPDATA%\.minecraft\smp-free-bot-control.txt
```

Commands:

```text
mode=auto
radius=12
server=dauntless31-Rvm4.aternos.me:25565
autojoin=true
```

```text
mode=wood
radius=10
```

```text
mode=mine
radius=12
```

```text
mode=stop
```

Helper scripts:

```powershell
.\set-mode.ps1 auto
.\set-mode.ps1 wood
.\set-mode.ps1 mine
.\set-mode.ps1 stop
```

or double-click:

```text
start-auto.cmd
start-wood.cmd
start-mine.cmd
stop-bot.cmd
```

Recommended flow:

1. Launch the `Fabric 26.1.2 Bot` profile in SKLauncher.
2. Leave `mode=auto` with `autojoin=true`; the bot connects to `dauntless31-Rvm4.aternos.me:25565`.
3. Stand near trees when it spawns.
4. Keep `stop-bot.cmd` handy.
