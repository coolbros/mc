package com.agast.minecraft.smpbots;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

final class BotManager {
    private final SmpWorkerBotsPlugin plugin;
    private final Map<String, WorkerBot> bots = new LinkedHashMap<>();
    private BukkitTask task;
    private int nextBotNumber = 1;

    BotManager(SmpWorkerBotsPlugin plugin) {
        this.plugin = plugin;
    }

    void start() {
        int interval = Math.max(1, plugin.getConfig().getInt("tick-interval", 10));
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, interval, interval);
    }

    void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }

        for (WorkerBot bot : new ArrayList<>(bots.values())) {
            bot.remove();
        }
        bots.clear();
    }

    WorkerBot spawn(Player owner, String requestedName, int requestedRadius) {
        String name = requestedName == null || requestedName.isBlank()
            ? "Worker" + nextBotNumber++
            : requestedName;
        name = uniqueName(name);

        int defaultRadius = plugin.getConfig().getInt("default-quarry-radius", 48);
        int radius = requestedRadius > 0 ? requestedRadius : defaultRadius;
        Location home = owner.getLocation().clone();

        WorkerBot bot = new WorkerBot(plugin, name, home, radius);
        bots.put(key(name), bot);
        return bot;
    }

    boolean remove(String target) {
        WorkerBot bot = bots.remove(key(target));
        if (bot == null) {
            return false;
        }

        bot.remove();
        return true;
    }

    int removeAll() {
        int count = bots.size();
        for (WorkerBot bot : new ArrayList<>(bots.values())) {
            bot.remove();
        }
        bots.clear();
        return count;
    }

    int setRunning(String target, boolean running) {
        Collection<WorkerBot> selected = select(target);
        for (WorkerBot bot : selected) {
            bot.setRunning(running);
        }
        return selected.size();
    }

    Collection<WorkerBot> all() {
        return bots.values();
    }

    WorkerBot get(String name) {
        return bots.get(key(name));
    }

    private void tick() {
        for (WorkerBot bot : new ArrayList<>(bots.values())) {
            try {
                bot.tick();
            } catch (RuntimeException exception) {
                plugin.getLogger().warning("Bot " + bot.name() + " tick failed: " + exception.getMessage());
            }
        }
    }

    private Collection<WorkerBot> select(String target) {
        if (target == null || target.equalsIgnoreCase("all")) {
            return new ArrayList<>(bots.values());
        }

        WorkerBot bot = get(target);
        if (bot == null) {
            return java.util.List.of();
        }
        return java.util.List.of(bot);
    }

    private String uniqueName(String requestedName) {
        String base = requestedName.replaceAll("[^A-Za-z0-9_-]", "");
        if (base.isBlank()) {
            base = "Worker" + nextBotNumber++;
        }

        String candidate = base;
        int suffix = 2;
        while (bots.containsKey(key(candidate))) {
            candidate = base + suffix++;
        }
        return candidate;
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
