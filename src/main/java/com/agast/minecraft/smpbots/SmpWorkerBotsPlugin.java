package com.agast.minecraft.smpbots;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SmpWorkerBotsPlugin extends JavaPlugin {
    private BotManager botManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        botManager = new BotManager(this);
        botManager.start();

        PluginCommand command = getCommand("smpbot");
        if (command != null) {
            SmpBotCommand executor = new SmpBotCommand(botManager);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        getLogger().info("SMP worker bots enabled.");
    }

    @Override
    public void onDisable() {
        if (botManager != null) {
            botManager.shutdown();
        }
    }
}
