package com.agast.minecraft.smpbots;

import org.bukkit.Material;

final class BotTool {
    private final Material material;
    private final int maxDurability;
    private int durabilityLeft;

    private BotTool(Material material, int maxDurability) {
        this.material = material;
        this.maxDurability = maxDurability;
        this.durabilityLeft = maxDurability;
    }

    static BotTool woodenPickaxe() {
        return new BotTool(Material.WOODEN_PICKAXE, 59);
    }

    static BotTool stonePickaxe() {
        return new BotTool(Material.STONE_PICKAXE, 131);
    }

    Material material() {
        return material;
    }

    boolean isWooden() {
        return material == Material.WOODEN_PICKAXE;
    }

    boolean canMineIron() {
        return material != Material.WOODEN_PICKAXE;
    }

    boolean damageAndCheckBroken() {
        durabilityLeft--;
        return durabilityLeft <= 0;
    }

    String label() {
        String name = material.name().toLowerCase().replace('_', ' ');
        return name + " " + durabilityLeft + "/" + maxDurability;
    }
}
