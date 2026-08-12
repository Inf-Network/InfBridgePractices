package net.infnetwork.snowball.bridginganalyzer.utils;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class NoAIUtils {

    /** Non-living entities are ignored. */
    public static void setAI(Entity bukkitEntity, boolean hasAI) {
        if (bukkitEntity instanceof LivingEntity living) {
            living.setAI(hasAI);
        }
    }
}
