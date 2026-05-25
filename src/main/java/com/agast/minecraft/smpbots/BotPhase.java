package com.agast.minecraft.smpbots;

enum BotPhase {
    COLLECTING_WOOD("wood"),
    MINING("mining"),
    PAUSED("paused");

    private final String label;

    BotPhase(String label) {
        this.label = label;
    }

    String label() {
        return label;
    }
}
