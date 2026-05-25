# SMP Worker Bots

This is a Paper/Spigot-style server plugin. It does not use Mineflayer or bot accounts. The server runs worker bots that:

- search for logs and chop enough wood,
- craft planks, sticks, a crafting table, and a wooden pickaxe,
- upgrade to stone pickaxes after mining cobblestone,
- mine stone and iron ore in a configurable quarry area,
- craft another pickaxe when the current one breaks.

The bots are represented by named armor stands and mine server-side. They are not full fake-player clients.

## Aternos

Free Aternos servers do not let you upload arbitrary custom plugin JARs. For `dauntless31-Rvm4.aternos.me`, use one of these paths:

- Use `free-client-mod/` for the no-credit local client bot path. This is the recommended route for your current SKLauncher + Aternos setup.
- Upload the datapack prototype in `aternos-datapack/smp-worker-bots` to your world's `datapacks` folder.
- Move the SMP to a host that allows custom plugin JAR uploads, then use the Paper plugin in this repository.
- Publish/suggest the plugin through Aternos' supported add-on process.

The datapack version is simpler than the Paper plugin. It creates command-driven armor stand workers with:

```text
/function smpbots:spawn
/function smpbots:start_all
/function smpbots:stop_all
/function smpbots:remove_all
```

Spawn the bot beside trees first. It gathers nearby logs, crafts internally, then mines nearby stone and iron.

## Build

This machine currently needs a JDK and Gradle installed before the plugin can be compiled.

```powershell
gradle build
```

The compiled plugin will be:

```text
build/libs/smp-worker-bots-1.0.0.jar
```

Put that JAR in your server's `plugins/` folder, then restart the server.

## Server Version

The default Gradle settings target the current Paper API format:

```properties
paperApiVersion=26.1.2.build.+
pluginApiVersion=26.1.2
javaVersion=25
```

If your SMP is an older 1.21.x server, edit `gradle.properties`. For example:

```properties
paperApiVersion=1.21.8-R0.1-SNAPSHOT
pluginApiVersion=1.21
javaVersion=21
```

## Commands

```text
/smpbot spawn [name] [radius]
/smpbot start [name|all]
/smpbot stop [name|all]
/smpbot status
/smpbot remove [name|all]
```

Examples:

```text
/smpbot spawn Miner 64
/smpbot status
/smpbot stop all
```

## Safety

Minecraft worlds are effectively infinite, so "mine all stone and iron in the world" has to be bounded. The plugin mines inside each bot's quarry radius and throttles work with `blocks-per-tick` so it does not flatten your SMP in one tick.
