# Local Minecraft Client Bot

This controls the Minecraft window on your laptop with keyboard and mouse input. It does not need your Aternos password.

## Claude Vision Bot

The controller is `vision-agent.ps1`. It captures the Minecraft window, sends the screenshot to Claude Haiku vision, asks for a tiny JSON action, then executes only bounded keyboard/mouse actions.

This is useful for experiments, but the free route in `../free-client-mod` is still better long-term because it reads real Minecraft block/inventory state instead of pixels.

Set your API key in the same PowerShell window:

```powershell
$env:ANTHROPIC_API_KEY="your-api-key"
```

Then run:

```powershell
powershell.exe -ExecutionPolicy Bypass -NoProfile -File .\vision-agent.ps1 -Provider Anthropic -Goal WoodToIron -Steps 80
```

or:

```powershell
powershell.exe -ExecutionPolicy Bypass -NoProfile -File .\vision-agent.ps1 -Provider Anthropic -Goal MineStoneIron -Steps 120
```

Easy wrappers:

```powershell
.\run-vision-wood-to-iron.cmd
.\run-vision-miner.cmd
.\run-claude-supervisor.cmd
```

Defaults:

- Anthropic API base: `https://api.anthropic.com/v1`
- Model: `claude-3-5-haiku-20241022`
- You can override with `$env:ANTHROPIC_MODEL="claude-3-haiku-20240307"` or another vision-capable Claude model.
- Kimi is still supported with `-Provider Kimi` if you later add Kimi credits.

## Blind Macro

The older macro controller is still here, but it is intentionally dumb. Use it only for short manual experiments.

## What The Blind Macro Can Do

- Focus the running Minecraft client.
- Run a hands-off wood-chopping loop when you stand in front of a tree.
- Run a hands-off tunnel-mining loop when you equip a pickaxe.
- Stop automatically after the number of seconds you choose.

This is a controller, not a real Minecraft brain. It cannot reliably inspect your inventory, pathfind around caves, avoid lava, or craft from the GUI yet. For the fully smart version, we need a client-side Fabric bot mod.

## Before Running

1. Open SKLauncher.
2. Launch Minecraft and join `dauntless31-Rvm4.aternos.me`.
3. Click into the Minecraft world so your player can move.
4. Make sure the game is not paused or in chat.

## Commands

From this folder:

```powershell
.\minecraft-control.ps1 -Mode ChopWood -Seconds 90
```

Stand facing the lower log of a tree before starting.

```powershell
.\minecraft-control.ps1 -Mode MineTunnel -Seconds 300
```

Equip a pickaxe and face the wall you want to mine before starting.

Emergency stop: press `Ctrl+C` in the PowerShell window, or switch back to Minecraft and press `Esc`.

## Safer Defaults

Start with short runs like `-Seconds 20`. Long unattended mining can kill your player if you hit lava, mobs, cliffs, or water.
