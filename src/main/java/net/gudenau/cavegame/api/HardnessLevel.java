package net.gudenau.cavegame.api;

/**
 * The "hardness levels" of different materials.
 */
public enum HardnessLevel {
    DIRT(1),
    ROCK(10),
    HARD_ROCK(30),
    BEDROCK(-1 /* UNBREAKABLE */),
    ;

    /**
     * An unbreakable material.
     */
    public static final int UNBREAKABLE = -1;

    private final int hardness;

    HardnessLevel(int hardness) {
        this.hardness = hardness;
    }

    /**
     * Gets the base "hardness" of this hardness level. The higher the value the longer it takes to mine.<br>
     * <br>
     * A hardness of {@link #UNBREAKABLE} means it can't be mined.
     *
     * @return The hardness
     */
    public int hardness() {
        return hardness;
    }

    /**
     * Checks if this hardness level can not be mined.
     *
     * @return True if unbreakable, false if breakable
     */
    public boolean unbreakable() {
        return hardness == UNBREAKABLE;
    }
}
