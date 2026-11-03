package com.thelostcenturies.core.survival;

import com.thelostcenturies.core.Config;
import java.util.Locale;

/** Per-world survival preset: player thirst and what happens to colonists without water. */
public enum Preset {
    /** No player thirst; colonies don't need water. */
    EASY,
    /** Light player thirst; colonists are slowed while the colony has no water. */
    NORMAL,
    /** Regular player thirst; workers stop working while the colony has no water. */
    HARD;

    /** Multiplier on Thirst Was Taken's exhaustion (how fast the player gets thirsty). */
    public double thirstRate() {
        return switch (this) {
            case EASY -> 0.0;
            case NORMAL -> Config.NORMAL_THIRST_RATE.get();
            case HARD -> Config.HARD_THIRST_RATE.get();
        };
    }

    public boolean colonyNeedsWater() {
        return this != EASY;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Preset byId(String id) {
        for (Preset p : values()) {
            if (p.id().equalsIgnoreCase(id)) {
                return p;
            }
        }
        return null;
    }
}
