package com.agast.minecraft.smpbots;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

final class WorkerBot {
    private final SmpWorkerBotsPlugin plugin;
    private final String name;
    private final Location home;
    private final int radius;
    private final BotInventory inventory = new BotInventory();
    private final TargetScanner scanner;
    private final boolean mineStone;
    private final int woodSearchRadius;
    private final double moveBlocksPerAction;
    private final double mineReach;

    private BotTool tool;
    private BotPhase phase = BotPhase.COLLECTING_WOOD;
    private boolean running = true;
    private UUID standId;
    private ArmorStand stand;
    private Block currentTarget;

    WorkerBot(SmpWorkerBotsPlugin plugin, String name, Location home, int radius) {
        this.plugin = plugin;
        this.name = name;
        this.home = home;
        this.radius = radius;
        this.mineStone = plugin.getConfig().getBoolean("mine-stone", true);
        this.woodSearchRadius = Math.max(4, plugin.getConfig().getInt("wood-search-radius", 32));
        this.moveBlocksPerAction = Math.max(0.1, plugin.getConfig().getDouble("move-blocks-per-action", 1.25));
        this.mineReach = Math.max(1.0, plugin.getConfig().getDouble("mine-reach", 3.2));
        this.scanner = new TargetScanner(home, radius);
        this.stand = spawnStand(home);
        refreshName();
    }

    String name() {
        return name;
    }

    int radius() {
        return radius;
    }

    void setRunning(boolean running) {
        this.running = running;
        if (!running) {
            phase = BotPhase.PAUSED;
        } else if (tool == null) {
            phase = BotPhase.COLLECTING_WOOD;
        } else {
            phase = BotPhase.MINING;
        }
        refreshName();
    }

    void tick() {
        ensureStand();

        if (!running) {
            refreshName();
            return;
        }

        if (phase == BotPhase.COLLECTING_WOOD) {
            collectWoodTick();
        } else {
            miningTick();
        }

        refreshName();
    }

    void remove() {
        if (stand != null && !stand.isDead()) {
            stand.remove();
        }
    }

    String statusLine() {
        String toolText = tool == null ? "no pickaxe" : tool.label();
        return name + " [" + phase.label() + "] at " + formatLocation(currentLocation())
            + " radius=" + radius
            + " tool=" + toolText
            + " inv={" + inventory.summary() + "}";
    }

    private void collectWoodTick() {
        if (craftUntilReadyToMine()) {
            phase = BotPhase.MINING;
            currentTarget = null;
            return;
        }

        Block log = findNearestLog();
        if (log == null) {
            return;
        }

        if (!moveWithinReach(log.getLocation().add(0.5, 0.5, 0.5))) {
            return;
        }

        log.setType(Material.AIR);
        inventory.logs++;
    }

    private void miningTick() {
        if (!ensurePickaxe()) {
            phase = BotPhase.COLLECTING_WOOD;
            currentTarget = null;
            return;
        }

        if (currentTarget == null || !isMineTarget(currentTarget.getType())) {
            currentTarget = scanner.findNext(this::isMineTarget);
        }

        if (currentTarget == null) {
            return;
        }

        if (!moveWithinReach(currentTarget.getLocation().add(0.5, 0.5, 0.5))) {
            return;
        }

        mine(currentTarget);
        currentTarget = null;
    }

    private boolean craftUntilReadyToMine() {
        while (inventory.planks < 9 && inventory.logs > 0) {
            inventory.convertOneLogToPlanks();
        }

        if (inventory.craftingTables == 0 && inventory.planks >= 4) {
            inventory.craftCraftingTable();
            placeCraftingTable();
        }

        craftSticksIfNeeded();

        if (tool == null && inventory.hasWoodenPickaxeParts()) {
            inventory.useWoodenPickaxeParts();
            tool = BotTool.woodenPickaxe();
            updateHeldTool();
        }

        return tool != null && inventory.craftingTables > 0;
    }

    private boolean ensurePickaxe() {
        craftSticksIfNeeded();

        if (tool != null && tool.isWooden() && inventory.hasStonePickaxeParts()) {
            inventory.useStonePickaxeParts();
            tool = BotTool.stonePickaxe();
            updateHeldTool();
            return true;
        }

        if (tool != null) {
            return true;
        }

        if (inventory.hasStonePickaxeParts()) {
            inventory.useStonePickaxeParts();
            tool = BotTool.stonePickaxe();
            updateHeldTool();
            return true;
        }

        while (inventory.planks < 3 && inventory.logs > 0) {
            inventory.convertOneLogToPlanks();
        }
        craftSticksIfNeeded();

        if (inventory.hasWoodenPickaxeParts()) {
            inventory.useWoodenPickaxeParts();
            tool = BotTool.woodenPickaxe();
            updateHeldTool();
            return true;
        }

        return false;
    }

    private void craftSticksIfNeeded() {
        if (inventory.sticks >= 2) {
            return;
        }

        while (inventory.sticks < 2 && inventory.planks >= 2) {
            inventory.craftSticks();
        }
    }

    private void mine(Block block) {
        Material type = block.getType();

        if (type == Material.IRON_ORE || type == Material.DEEPSLATE_IRON_ORE) {
            inventory.rawIron++;
        } else {
            inventory.cobblestone++;
            inventory.stoneMined++;
        }

        block.setType(Material.AIR);

        if (tool != null && tool.damageAndCheckBroken()) {
            tool = null;
            updateHeldTool();
        }
    }

    private boolean isMineTarget(Material material) {
        if ((material == Material.IRON_ORE || material == Material.DEEPSLATE_IRON_ORE)
            && tool != null && tool.canMineIron()) {
            return true;
        }

        return mineStone && (material == Material.STONE || material == Material.DEEPSLATE);
    }

    private Block findNearestLog() {
        Location center = currentLocation();
        World world = center.getWorld();
        if (world == null) {
            return null;
        }

        int baseX = center.getBlockX();
        int baseY = center.getBlockY();
        int baseZ = center.getBlockZ();
        int minY = Math.max(world.getMinHeight(), baseY - 8);
        int maxY = Math.min(world.getMaxHeight() - 1, baseY + 20);

        List<Block> logs = new ArrayList<>();
        for (int x = baseX - woodSearchRadius; x <= baseX + woodSearchRadius; x++) {
            for (int z = baseZ - woodSearchRadius; z <= baseZ + woodSearchRadius; z++) {
                if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                    continue;
                }

                for (int y = minY; y <= maxY; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (Tag.LOGS.isTagged(block.getType())) {
                        logs.add(block);
                    }
                }
            }
        }

        return logs.stream()
            .min(Comparator.comparingDouble(block -> block.getLocation().distanceSquared(center)))
            .orElse(null);
    }

    private boolean moveWithinReach(Location target) {
        Location current = currentLocation();
        if (current.getWorld() == null || !current.getWorld().equals(target.getWorld())) {
            return false;
        }

        if (current.distanceSquared(target) <= mineReach * mineReach) {
            return true;
        }

        Vector delta = target.toVector().subtract(current.toVector());
        double distance = delta.length();
        if (distance < 0.0001) {
            return true;
        }

        Vector step = delta.normalize().multiply(Math.min(moveBlocksPerAction, distance));
        Location next = current.clone().add(step);
        next.setYaw(yawFrom(step));
        stand.teleport(next);
        return false;
    }

    private ArmorStand spawnStand(Location location) {
        World world = location.getWorld();
        if (world == null) {
            throw new IllegalStateException("Cannot spawn a bot without a world.");
        }

        ArmorStand armorStand = (ArmorStand) world.spawnEntity(location, EntityType.ARMOR_STAND);
        armorStand.setCustomNameVisible(true);
        armorStand.setGravity(false);
        armorStand.setInvulnerable(true);
        armorStand.setCanPickupItems(false);
        armorStand.setBasePlate(false);
        armorStand.setArms(true);
        standId = armorStand.getUniqueId();
        return armorStand;
    }

    private void ensureStand() {
        if (stand != null && !stand.isDead()) {
            return;
        }

        Entity entity = standId == null ? null : Bukkit.getEntity(standId);
        if (entity instanceof ArmorStand armorStand) {
            stand = armorStand;
            return;
        }

        stand = spawnStand(home);
        updateHeldTool();
    }

    private void refreshName() {
        if (stand == null || stand.isDead()) {
            return;
        }

        String toolText = tool == null ? "no pick" : tool.material().name().replace("_PICKAXE", "").toLowerCase();
        stand.setCustomName(name + " | " + phase.label() + " | " + toolText);
    }

    private void updateHeldTool() {
        if (stand == null || stand.isDead()) {
            return;
        }

        EntityEquipment equipment = stand.getEquipment();
        if (equipment == null) {
            return;
        }

        equipment.setItemInMainHand(tool == null ? new ItemStack(Material.AIR) : new ItemStack(tool.material()));
    }

    private void placeCraftingTable() {
        World world = home.getWorld();
        if (world == null) {
            return;
        }

        int baseX = currentLocation().getBlockX();
        int baseY = currentLocation().getBlockY();
        int baseZ = currentLocation().getBlockZ();
        int[][] offsets = {
            {1, 0, 0},
            {-1, 0, 0},
            {0, 0, 1},
            {0, 0, -1},
            {1, -1, 0},
            {-1, -1, 0},
            {0, -1, 1},
            {0, -1, -1}
        };

        for (int[] offset : offsets) {
            Block block = world.getBlockAt(baseX + offset[0], baseY + offset[1], baseZ + offset[2]);
            if (block.getType().isAir()) {
                block.setType(Material.CRAFTING_TABLE);
                return;
            }
        }
    }

    private Location currentLocation() {
        return stand == null || stand.isDead() ? home.clone() : stand.getLocation();
    }

    private static float yawFrom(Vector vector) {
        return (float) Math.toDegrees(Math.atan2(-vector.getX(), vector.getZ()));
    }

    private static String formatLocation(Location location) {
        return location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    private final class TargetScanner {
        private final World world;
        private final int minX;
        private final int maxX;
        private final int minY;
        private final int maxY;
        private final int minZ;
        private final int maxZ;
        private int x;
        private int y;
        private int z;

        TargetScanner(Location center, int radius) {
            this.world = center.getWorld();
            if (world == null) {
                throw new IllegalStateException("Cannot scan without a world.");
            }

            this.minX = center.getBlockX() - radius;
            this.maxX = center.getBlockX() + radius;
            this.minZ = center.getBlockZ() - radius;
            this.maxZ = center.getBlockZ() + radius;
            this.minY = Math.max(world.getMinHeight(), plugin.getConfig().getInt("scan-min-y", -64));
            this.maxY = Math.min(world.getMaxHeight() - 1, plugin.getConfig().getInt("scan-max-y", 320));
            this.x = minX;
            this.y = minY;
            this.z = minZ;
        }

        Block findNext(java.util.function.Predicate<Material> predicate) {
            int budget = Math.max(1, plugin.getConfig().getInt("blocks-per-tick", 700));
            for (int inspected = 0; inspected < budget; inspected++) {
                int blockX = x;
                int blockY = y;
                int blockZ = z;
                advance();

                if (!world.isChunkLoaded(blockX >> 4, blockZ >> 4)) {
                    continue;
                }

                Block block = world.getBlockAt(blockX, blockY, blockZ);
                if (predicate.test(block.getType())) {
                    return block;
                }
            }

            return null;
        }

        private void advance() {
            x++;
            if (x <= maxX) {
                return;
            }

            x = minX;
            z++;
            if (z <= maxZ) {
                return;
            }

            z = minZ;
            y++;
            if (y <= maxY) {
                return;
            }

            y = minY;
        }
    }
}
