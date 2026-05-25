package com.agast.minecraft.smpbots;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

final class SmpBotCommand implements CommandExecutor, TabCompleter {
    private final BotManager botManager;

    SmpBotCommand(BotManager botManager) {
        this.botManager = botManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "spawn":
                return spawn(sender, args);
            case "start":
                return setRunning(sender, args, true);
            case "stop":
                return setRunning(sender, args, false);
            case "status":
                return status(sender);
            case "remove":
                return remove(sender, args);
            default:
                sendUsage(sender);
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("spawn", "start", "stop", "status", "remove"), args[0]);
        }

        if (args.length == 2 && List.of("start", "stop", "remove").contains(args[0].toLowerCase())) {
            List<String> names = new ArrayList<>();
            names.add("all");
            for (WorkerBot bot : botManager.all()) {
                names.add(bot.name());
            }
            return filter(names, args[1]);
        }

        return List.of();
    }

    private boolean spawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only a player can spawn a bot.");
            return true;
        }

        String name = args.length >= 2 ? args[1] : null;
        int radius = args.length >= 3 ? parsePositiveInt(args[2], -1) : -1;
        if (args.length >= 3 && radius <= 0) {
            sender.sendMessage(ChatColor.RED + "Radius must be a positive number.");
            return true;
        }

        WorkerBot bot = botManager.spawn(player, name, radius);
        sender.sendMessage(ChatColor.GREEN + "Spawned " + bot.name() + " with quarry radius " + bot.radius() + ".");
        return true;
    }

    private boolean setRunning(CommandSender sender, String[] args, boolean running) {
        String target = args.length >= 2 ? args[1] : "all";
        int count = botManager.setRunning(target, running);
        if (count == 0) {
            sender.sendMessage(ChatColor.RED + "No matching bot found.");
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + (running ? "Started " : "Stopped ") + count + " bot(s).");
        return true;
    }

    private boolean status(CommandSender sender) {
        if (botManager.all().isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No bots are active.");
            return true;
        }

        for (WorkerBot bot : botManager.all()) {
            sender.sendMessage(ChatColor.AQUA + bot.statusLine());
        }
        return true;
    }

    private boolean remove(CommandSender sender, String[] args) {
        String target = args.length >= 2 ? args[1] : "";
        if (target.equalsIgnoreCase("all")) {
            int count = botManager.removeAll();
            sender.sendMessage(ChatColor.GREEN + "Removed " + count + " bot(s).");
            return true;
        }

        if (target.isBlank()) {
            sender.sendMessage(ChatColor.RED + "Choose a bot name or 'all'.");
            return true;
        }

        if (!botManager.remove(target)) {
            sender.sendMessage(ChatColor.RED + "No bot named " + target + " exists.");
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + "Removed " + target + ".");
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "/smpbot spawn [name] [radius]");
        sender.sendMessage(ChatColor.YELLOW + "/smpbot start [name|all]");
        sender.sendMessage(ChatColor.YELLOW + "/smpbot stop [name|all]");
        sender.sendMessage(ChatColor.YELLOW + "/smpbot status");
        sender.sendMessage(ChatColor.YELLOW + "/smpbot remove [name|all]");
    }

    private static int parsePositiveInt(String value, int fallback) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static List<String> filter(List<String> values, String prefix) {
        String lowerPrefix = prefix.toLowerCase();
        return values.stream()
            .filter(value -> value.toLowerCase().startsWith(lowerPrefix))
            .toList();
    }
}
