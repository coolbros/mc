package com.agast.minecraft.smpbots;

final class BotInventory {
    int logs;
    int planks;
    int sticks;
    int craftingTables;
    int cobblestone;
    int rawIron;
    int stoneMined;

    void convertOneLogToPlanks() {
        if (logs <= 0) {
            return;
        }

        logs--;
        planks += 4;
    }

    boolean craftCraftingTable() {
        if (planks < 4) {
            return false;
        }

        planks -= 4;
        craftingTables++;
        return true;
    }

    boolean craftSticks() {
        if (planks < 2) {
            return false;
        }

        planks -= 2;
        sticks += 4;
        return true;
    }

    boolean hasWoodenPickaxeParts() {
        return planks >= 3 && sticks >= 2;
    }

    boolean hasStonePickaxeParts() {
        return cobblestone >= 3 && sticks >= 2;
    }

    void useWoodenPickaxeParts() {
        planks -= 3;
        sticks -= 2;
    }

    void useStonePickaxeParts() {
        cobblestone -= 3;
        sticks -= 2;
    }

    int totalMined() {
        return stoneMined + rawIron;
    }

    String summary() {
        return "logs=" + logs
            + ", planks=" + planks
            + ", sticks=" + sticks
            + ", tables=" + craftingTables
            + ", cobble=" + cobblestone
            + ", iron=" + rawIron
            + ", mined=" + totalMined();
    }
}
