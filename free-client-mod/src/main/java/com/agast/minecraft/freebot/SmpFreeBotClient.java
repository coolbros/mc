package com.agast.minecraft.freebot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class SmpFreeBotClient implements ClientModInitializer {
    private static final int DEFAULT_RADIUS = 10;
    private static final int CONTROL_READ_INTERVAL = 20;
    private static final int AUTO_JOIN_DELAY_TICKS = 80;
    private static final int AUTO_JOIN_RETRY_TICKS = 400;
    private static final String DEFAULT_SERVER = "dauntless31-Rvm4.aternos.me:25565";
    private static final double REACH = 4.25;
    private static final Set<Block> LOG_BLOCKS = Set.of(
        Blocks.OAK_LOG,
        Blocks.SPRUCE_LOG,
        Blocks.BIRCH_LOG,
        Blocks.JUNGLE_LOG,
        Blocks.ACACIA_LOG,
        Blocks.DARK_OAK_LOG,
        Blocks.MANGROVE_LOG,
        Blocks.CHERRY_LOG,
        Blocks.PALE_OAK_LOG
    );
    private static final Set<Block> MINE_BLOCKS = Set.of(
        Blocks.STONE,
        Blocks.DEEPSLATE,
        Blocks.IRON_ORE,
        Blocks.DEEPSLATE_IRON_ORE
    );
    private static final Set<Block> DANGER_BLOCKS = Set.of(
        Blocks.LAVA,
        Blocks.FIRE,
        Blocks.SOUL_FIRE,
        Blocks.MAGMA_BLOCK,
        Blocks.WATER
    );
    private static final Set<Item> AXES = Set.of(
        Items.WOODEN_AXE,
        Items.STONE_AXE,
        Items.IRON_AXE,
        Items.GOLDEN_AXE,
        Items.DIAMOND_AXE,
        Items.NETHERITE_AXE
    );
    private static final Set<Item> LOG_ITEMS = Set.of(
        Items.OAK_LOG,
        Items.SPRUCE_LOG,
        Items.BIRCH_LOG,
        Items.JUNGLE_LOG,
        Items.ACACIA_LOG,
        Items.DARK_OAK_LOG,
        Items.MANGROVE_LOG,
        Items.CHERRY_LOG,
        Items.PALE_OAK_LOG
    );
    private static final Set<Item> PLANK_ITEMS = Set.of(
        Items.OAK_PLANKS,
        Items.SPRUCE_PLANKS,
        Items.BIRCH_PLANKS,
        Items.JUNGLE_PLANKS,
        Items.ACACIA_PLANKS,
        Items.DARK_OAK_PLANKS,
        Items.MANGROVE_PLANKS,
        Items.CHERRY_PLANKS,
        Items.PALE_OAK_PLANKS
    );
    private static final Set<Item> CRAFTING_TABLE_ITEM = Set.of(Items.CRAFTING_TABLE);
    private static final Set<Item> STICK_ITEM = Set.of(Items.STICK);
    private static final Set<Item> WOOD_PICKAXE_ITEM = Set.of(Items.WOODEN_PICKAXE);
    private static final Set<Item> STONE_PICKAXE_ITEM = Set.of(Items.STONE_PICKAXE);
    private static final Set<Item> PICKAXES = Set.of(
        Items.WOODEN_PICKAXE,
        Items.STONE_PICKAXE,
        Items.IRON_PICKAXE,
        Items.GOLDEN_PICKAXE,
        Items.DIAMOND_PICKAXE,
        Items.NETHERITE_PICKAXE
    );

    private static Mode mode = Mode.STOP;
    private static int radius = DEFAULT_RADIUS;
    private static boolean autoJoin = true;
    private static String serverAddress = DEFAULT_SERVER;
    private static int ticks;
    private static BlockPos target;
    private static BlockPos breakingTarget;
    private static Direction breakingFace;
    private static boolean announced;
    private static int lastJoinAttemptTick = -AUTO_JOIN_RETRY_TICKS;
    private static int lastCraftAttemptTick;
    private static final AtomicBoolean LOOP_STARTED = new AtomicBoolean();

    @Override
    public void onInitializeClient() {
        if (!LOOP_STARTED.compareAndSet(false, true)) {
            return;
        }

        Thread botThread = new Thread(() -> {
            while (true) {
                try {
                    Minecraft minecraft = Minecraft.getInstance();
                    if (minecraft != null) {
                        minecraft.execute(() -> tick(minecraft));
                    }
                    Thread.sleep(50L);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (RuntimeException ignored) {
                    // Keep the helper loop alive; the next tick can recover.
                }
            }
        }, "smp-free-bot-loop");
        botThread.setDaemon(true);
        botThread.start();
    }

    private static void tick(Minecraft minecraft) {
        ticks++;
        if (ticks % CONTROL_READ_INTERVAL == 0) {
            readControlFile(minecraft);
        }

        if (mode == Mode.STOP) {
            releaseMovement(minecraft);
            return;
        }

        if (minecraft.player == null || minecraft.level == null) {
            releaseMovement(minecraft);
            tryAutoJoin(minecraft);
            return;
        }

        if (minecraft.screen != null && !(mode == Mode.AUTO && needsCrafting(minecraft.player))) {
            releaseMovement(minecraft);
            return;
        }

        LocalPlayer player = minecraft.player;
        if (!announced) {
            player.sendSystemMessage(Component.literal("[SMPFreeBot] Running " + mode.name().toLowerCase(Locale.ROOT) + " mode."));
            announced = true;
        }

        if (nearDanger(minecraft, player.blockPosition())) {
            releaseMovement(minecraft);
            player.sendOverlayMessage(Component.literal("[SMPFreeBot] Danger nearby. Paused."));
            return;
        }

        if (mode == Mode.AUTO) {
            runAutoMode(minecraft);
        } else if (mode == Mode.WOOD) {
            runBlockMode(minecraft, LOG_BLOCKS, AXES, "logs");
        } else if (mode == Mode.MINE) {
            runBlockMode(minecraft, MINE_BLOCKS, PICKAXES, "stone/iron");
        }
    }

    private static void tryAutoJoin(Minecraft minecraft) {
        if (!autoJoin || serverAddress.isBlank()) {
            return;
        }

        if (ticks < AUTO_JOIN_DELAY_TICKS || ticks - lastJoinAttemptTick < AUTO_JOIN_RETRY_TICKS) {
            return;
        }

        if (minecraft.screen instanceof ConnectScreen) {
            return;
        }

        lastJoinAttemptTick = ticks;
        Screen parent = minecraft.screen == null ? new TitleScreen() : minecraft.screen;
        ServerData server = new ServerData("SMP Free Bot", serverAddress, ServerData.Type.OTHER);
        System.out.println("[SMPFreeBot] Auto-joining " + serverAddress);
        ConnectScreen.startConnecting(parent, minecraft, ServerAddress.parseString(serverAddress), server, false, null);
    }

    private static void runAutoMode(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        if (needsCrafting(player) && tryCraftingProgress(minecraft)) {
            releaseMovement(minecraft);
            return;
        }

        if (!hasAny(player.getInventory(), PICKAXES)) {
            runBlockMode(minecraft, LOG_BLOCKS, AXES, "logs");
            return;
        }

        if (minecraft.screen != null) {
            player.closeContainer();
        }
        runBlockMode(minecraft, MINE_BLOCKS, PICKAXES, "stone/iron");
    }

    private static boolean needsCrafting(LocalPlayer player) {
        Inventory inventory = player.getInventory();
        return !hasAny(inventory, PICKAXES)
            || (countItems(inventory, LOG_ITEMS) > 0 && countItems(inventory, PLANK_ITEMS) < 8)
            || (countItems(inventory, PLANK_ITEMS) >= 4 && countItems(inventory, CRAFTING_TABLE_ITEM) == 0)
            || (countItems(inventory, PLANK_ITEMS) >= 2 && countItems(inventory, STICK_ITEM) < 2);
    }

    private static boolean tryCraftingProgress(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.gameMode == null) {
            return false;
        }

        if (ticks - lastCraftAttemptTick < 10) {
            return true;
        }
        lastCraftAttemptTick = ticks;

        Inventory inventory = player.getInventory();
        int logs = countItems(inventory, LOG_ITEMS);
        int planks = countItems(inventory, PLANK_ITEMS);
        int sticks = countItems(inventory, STICK_ITEM);
        int tables = countItems(inventory, CRAFTING_TABLE_ITEM);
        int cobble = countItems(inventory, Set.of(Items.COBBLESTONE, Items.COBBLED_DEEPSLATE));
        boolean hasCraftingSurface = tables > 0
            || player.containerMenu instanceof CraftingMenu
            || findNearbyBlock(minecraft, Blocks.CRAFTING_TABLE, 3) != null;

        if (hasAny(inventory, PICKAXES)) {
            return false;
        }

        if (planks < 4 && logs > 0) {
            return tryCraftItem(minecraft, PLANK_ITEMS, true);
        }

        if (sticks < 2 && planks >= 2) {
            return tryCraftItem(minecraft, STICK_ITEM, false);
        }

        if (!hasCraftingSurface && planks >= 4) {
            return tryCraftItem(minecraft, CRAFTING_TABLE_ITEM, false);
        }

        if (hasCraftingSurface && cobble >= 3 && sticks >= 2) {
            return tryCraftWithTable(minecraft, STONE_PICKAXE_ITEM, false);
        }

        if (hasCraftingSurface && planks >= 3 && sticks >= 2) {
            return tryCraftWithTable(minecraft, WOOD_PICKAXE_ITEM, false);
        }

        return false;
    }

    private static boolean tryCraftWithTable(Minecraft minecraft, Set<Item> resultItems, boolean useMax) {
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return false;
        }

        if (player.containerMenu instanceof CraftingMenu) {
            boolean crafted = tryCraftItem(minecraft, resultItems, useMax);
            if (crafted && hasAny(player.getInventory(), PICKAXES)) {
                player.closeContainer();
            }
            return crafted;
        }

        openOrPlaceCraftingTable(minecraft);
        return true;
    }

    private static boolean tryCraftItem(Minecraft minecraft, Set<Item> resultItems, boolean useMax) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.gameMode == null) {
            return false;
        }

        RecipeDisplayId recipe = findRecipeFor(player, resultItems);
        if (recipe == null) {
            player.sendOverlayMessage(Component.literal("[SMPFreeBot] Waiting for recipe: " + resultItems.iterator().next()));
            return false;
        }

        int containerId = player.containerMenu.containerId;
        minecraft.gameMode.handlePlaceRecipe(containerId, recipe, useMax);
        minecraft.gameMode.handleContainerInput(containerId, 0, 0, ContainerInput.QUICK_MOVE, player);
        return true;
    }

    private static RecipeDisplayId findRecipeFor(LocalPlayer player, Set<Item> resultItems) {
        for (RecipeCollection collection : player.getRecipeBook().getCollections()) {
            for (RecipeDisplayEntry entry : collection.getRecipes()) {
                Item result = resultItem(entry);
                if (result != null && resultItems.contains(result)) {
                    return entry.id();
                }
            }
        }
        return null;
    }

    private static Item resultItem(RecipeDisplayEntry entry) {
        SlotDisplay result = entry.display().result();
        if (result instanceof SlotDisplay.ItemSlotDisplay itemDisplay) {
            return itemDisplay.item().value();
        }
        if (result instanceof SlotDisplay.ItemStackSlotDisplay stackDisplay) {
            return stackDisplay.stack().item().value();
        }
        return null;
    }

    private static void openOrPlaceCraftingTable(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.gameMode == null) {
            return;
        }

        BlockPos nearbyTable = findNearbyBlock(minecraft, Blocks.CRAFTING_TABLE, 3);
        if (nearbyTable != null) {
            lookAt(player, Vec3.atCenterOf(nearbyTable));
            minecraft.gameMode.useItemOn(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(nearbyTable), Direction.UP, nearbyTable, false));
            return;
        }

        if (!ensureHotbarItem(minecraft, Items.CRAFTING_TABLE, 8)) {
            player.sendOverlayMessage(Component.literal("[SMPFreeBot] Crafting table is not reachable in inventory yet."));
            return;
        }

        BlockPos playerPos = player.blockPosition();
        int[][] offsets = {
            {1, 0},
            {-1, 0},
            {0, 1},
            {0, -1}
        };

        for (int[] offset : offsets) {
            BlockPos support = playerPos.offset(offset[0], -1, offset[1]);
            BlockPos place = support.offset(0, 1, 0);
            if (!minecraft.level.getBlockState(support).isAir() && minecraft.level.getBlockState(place).isAir()) {
                lookAt(player, Vec3.atCenterOf(place));
                minecraft.gameMode.useItemOn(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(support), Direction.UP, support, false));
                stopBreaking(minecraft);
                target = null;
                return;
            }
        }
    }

    private static void runBlockMode(Minecraft minecraft, Set<Block> targets, Set<Item> tools, String label) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.gameMode == null) {
            return;
        }

        selectTool(player, tools);

        if (target == null || !targets.contains(minecraft.level.getBlockState(target).getBlock())) {
            stopBreaking(minecraft);
            target = findNearestTarget(minecraft, targets);
        }

        if (target == null) {
            releaseMovement(minecraft);
            player.sendOverlayMessage(Component.literal("[SMPFreeBot] No nearby " + label + " found."));
            return;
        }

        Vec3 targetCenter = Vec3.atCenterOf(target);
        lookAt(player, targetCenter);

        double distance = player.getEyePosition().distanceTo(targetCenter);
        if (distance > REACH) {
            stopBreaking(minecraft);
            moveToward(minecraft, player, targetCenter);
            return;
        }

        releaseWalkKeys(minecraft);
        Direction face = faceFromPlayer(player, target);
        holdBreak(minecraft, player, target, face);
    }

    private static BlockPos findNearestTarget(Minecraft minecraft, Set<Block> targets) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return null;
        }

        BlockPos origin = player.blockPosition();
        List<BlockPos> candidates = new ArrayList<>();
        int vertical = Math.min(radius, 8);
        for (int x = -radius; x <= radius; x++) {
            for (int y = -vertical; y <= vertical; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    BlockState state = minecraft.level.getBlockState(pos);
                    if (targets.contains(state.getBlock())) {
                        candidates.add(pos);
                    }
                }
            }
        }

        return candidates.stream()
            .min(Comparator.comparingDouble(pos -> Vec3.atCenterOf(pos).distanceToSqr(player.position())))
            .orElse(null);
    }

    private static boolean nearDanger(Minecraft minecraft, BlockPos origin) {
        if (minecraft.level == null) {
            return false;
        }

        for (int x = -3; x <= 3; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -3; z <= 3; z++) {
                    BlockState state = minecraft.level.getBlockState(origin.offset(x, y, z));
                    if (DANGER_BLOCKS.contains(state.getBlock()) || state.liquid()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void moveToward(Minecraft minecraft, LocalPlayer player, Vec3 targetCenter) {
        minecraft.options.keyAttack.setDown(false);
        minecraft.options.keyUp.setDown(true);
        minecraft.options.keySprint.setDown(true);
        minecraft.options.keyJump.setDown(player.horizontalCollision);

        Vec3 delta = targetCenter.subtract(player.position());
        double horizontalDistance = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        if (horizontalDistance < 1.2 && delta.y > 1.0) {
            minecraft.options.keyJump.setDown(true);
        }
    }

    private static void holdBreak(Minecraft minecraft, LocalPlayer player, BlockPos pos, Direction face) {
        Direction activeFace = breakingFace == null ? face : breakingFace;
        minecraft.hitResult = new BlockHitResult(Vec3.atCenterOf(pos), activeFace, pos, false);
        minecraft.options.keyAttack.setDown(true);

        boolean newTarget = breakingTarget == null || !breakingTarget.equals(pos);
        if (newTarget) {
            breakingTarget = pos;
            breakingFace = face;
            if (!minecraft.gameMode.startDestroyBlock(pos, face)) {
                minecraft.options.keyAttack.setDown(false);
                breakingTarget = null;
                breakingFace = null;
                return;
            }
            player.swing(InteractionHand.MAIN_HAND);
        }

        if (minecraft.gameMode.continueDestroyBlock(pos, activeFace)) {
            minecraft.level.addBreakingBlockEffect(pos, activeFace);
            if (ticks % 4 == 0) {
                player.swing(InteractionHand.MAIN_HAND);
            }
        } else if (minecraft.level.getBlockState(pos).isAir()) {
            breakingTarget = null;
            breakingFace = null;
        }
    }

    private static void stopBreaking(Minecraft minecraft) {
        minecraft.options.keyAttack.setDown(false);
        if (breakingTarget != null && minecraft.gameMode != null && minecraft.gameMode.isDestroying()) {
            minecraft.gameMode.stopDestroyBlock();
        }
        breakingTarget = null;
        breakingFace = null;
    }

    private static void lookAt(LocalPlayer player, Vec3 targetCenter) {
        Vec3 eye = player.getEyePosition();
        Vec3 delta = targetCenter.subtract(eye);
        double horizontalDistance = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float yaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, horizontalDistance));
        player.setYRot(yaw);
        player.setXRot(clamp(pitch, -89.0f, 89.0f));
    }

    private static Direction faceFromPlayer(LocalPlayer player, BlockPos pos) {
        Vec3 delta = Vec3.atCenterOf(pos).subtract(player.getEyePosition());
        double absX = Math.abs(delta.x);
        double absY = Math.abs(delta.y);
        double absZ = Math.abs(delta.z);
        if (absY >= absX && absY >= absZ) {
            return delta.y > 0 ? Direction.DOWN : Direction.UP;
        }
        if (absX >= absZ) {
            return delta.x > 0 ? Direction.WEST : Direction.EAST;
        }
        return delta.z > 0 ? Direction.NORTH : Direction.SOUTH;
    }

    private static void selectTool(LocalPlayer player, Set<Item> tools) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && tools.contains(stack.getItem())) {
                inventory.setSelectedSlot(slot);
                return;
            }
        }
    }

    private static boolean ensureHotbarItem(Minecraft minecraft, Item item, int preferredHotbarSlot) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.gameMode == null) {
            return false;
        }

        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && stack.getItem() == item) {
                inventory.setSelectedSlot(slot);
                return true;
            }
        }

        for (int menuSlot = 0; menuSlot < player.inventoryMenu.slots.size(); menuSlot++) {
            ItemStack stack = player.inventoryMenu.slots.get(menuSlot).getItem();
            if (!stack.isEmpty() && stack.getItem() == item) {
                minecraft.gameMode.handleContainerInput(
                    player.inventoryMenu.containerId,
                    menuSlot,
                    preferredHotbarSlot,
                    ContainerInput.SWAP,
                    player
                );
                inventory.setSelectedSlot(preferredHotbarSlot);
                return true;
            }
        }

        return false;
    }

    private static BlockPos findNearbyBlock(Minecraft minecraft, Block block, int searchRadius) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return null;
        }

        BlockPos origin = player.blockPosition();
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -1; y <= 2; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (minecraft.level.getBlockState(pos).getBlock() == block) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    private static boolean hasAny(Inventory inventory, Set<Item> items) {
        return countItems(inventory, items) > 0;
    }

    private static int countItems(Inventory inventory, Set<Item> items) {
        int count = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && items.contains(stack.getItem())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void releaseMovement(Minecraft minecraft) {
        stopBreaking(minecraft);
        releaseWalkKeys(minecraft);
    }

    private static void releaseWalkKeys(Minecraft minecraft) {
        minecraft.options.keyUp.setDown(false);
        minecraft.options.keyLeft.setDown(false);
        minecraft.options.keyRight.setDown(false);
        minecraft.options.keyDown.setDown(false);
        minecraft.options.keyJump.setDown(false);
        minecraft.options.keySprint.setDown(false);
    }

    private static void readControlFile(Minecraft minecraft) {
        Path controlPath = minecraft.gameDirectory.toPath().resolve("smp-free-bot-control.txt");
        if (!Files.exists(controlPath)) {
            return;
        }

        try {
            Mode nextMode = mode;
            int nextRadius = radius;
            boolean nextAutoJoin = autoJoin;
            String nextServerAddress = serverAddress;
            for (String rawLine : Files.readAllLines(controlPath)) {
                String line = rawLine.trim();
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("=", 2);
                if (parts.length != 2) {
                    continue;
                }

                String key = parts[0].trim().toLowerCase(Locale.ROOT);
                String rawValue = parts[1].trim();
                String value = rawValue.toLowerCase(Locale.ROOT);
                if (key.equals("mode")) {
                    nextMode = Mode.from(value);
                } else if (key.equals("radius")) {
                    nextRadius = parseRadius(value);
                } else if (key.equals("autojoin") || key.equals("auto_join")) {
                    nextAutoJoin = parseBoolean(value, true);
                } else if (key.equals("server") || key.equals("address")) {
                    nextServerAddress = rawValue;
                }
            }

            if (nextMode != mode) {
                target = null;
                announced = false;
                stopBreaking(minecraft);
            }
            if (!nextServerAddress.equals(serverAddress)) {
                lastJoinAttemptTick = -AUTO_JOIN_RETRY_TICKS;
            }
            mode = nextMode;
            radius = nextRadius;
            autoJoin = nextAutoJoin;
            serverAddress = nextServerAddress;
        } catch (IOException ignored) {
            // Try again next read interval.
        }
    }

    private static int parseRadius(String value) {
        try {
            return Math.max(4, Math.min(24, Integer.parseInt(value)));
        } catch (NumberFormatException ignored) {
            return DEFAULT_RADIUS;
        }
    }

    private static boolean parseBoolean(String value, boolean fallback) {
        return switch (value) {
            case "true", "yes", "y", "1", "on" -> true;
            case "false", "no", "n", "0", "off" -> false;
            default -> fallback;
        };
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum Mode {
        STOP,
        AUTO,
        WOOD,
        MINE;

        static Mode from(String value) {
            return switch (value) {
                case "auto", "woodtoiron", "wood_to_iron" -> AUTO;
                case "wood", "chop", "logs" -> WOOD;
                case "mine", "stone", "iron" -> MINE;
                default -> STOP;
            };
        }
    }
}
