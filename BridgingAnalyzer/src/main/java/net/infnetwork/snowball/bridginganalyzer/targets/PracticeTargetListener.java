package net.infnetwork.snowball.bridginganalyzer.targets;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;

public final class PracticeTargetListener implements Listener {
    private static final double MINIMUM_LETHAL_DAMAGE = 2.0;

    private final Plugin plugin;
    private final PracticeTargetService targets;
    private final Set<UUID> pendingFinishes = new HashSet<>();

    public PracticeTargetListener(Plugin plugin, PracticeTargetService targets) {
        this.plugin = plugin;
        this.targets = targets;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)
                || !(event.getEntity() instanceof Villager target)
                || !isDirectAttack(event.getCause())
                || !targets.isPracticeTarget(target)) {
            return;
        }

        // These disposable targets intentionally bypass animal-damage region flags. The
        // exception is gated by the plugin PDC/strict legacy identity, so normal villagers
        // keep the protection applied by WorldGuard and other plugins.
        event.setCancelled(false);

        // Modern attack cooldown can reduce an uncharged punch below one point of damage.
        // Replacing it with lethal damage preserves the normal death pipeline and killer
        // attribution while making target behavior independent of that scaling.
        event.setDamage(lethalDamage(target.getHealth(), target.getAbsorptionAmount()));
        scheduleMechanicalFinish(target.getUniqueId());
    }

    static double lethalDamage(double health, double absorption) {
        double required = Math.max(0.0, health) + Math.max(0.0, absorption) + 1.0;
        return Math.max(MINIMUM_LETHAL_DAMAGE, required);
    }

    private boolean isDirectAttack(EntityDamageEvent.DamageCause cause) {
        return cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                || cause == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK;
    }

    private void scheduleMechanicalFinish(UUID targetId) {
        if (!pendingFinishes.add(targetId)) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                Entity entity = plugin.getServer().getEntity(targetId);
                if (entity instanceof Villager target
                        && target.isValid()
                        && !target.isDead()
                        && targets.isPracticeTarget(target)) {
                    // Final correctness does not depend on damage modifiers or a listener
                    // changing damage after us. The original event gets one tick to keep
                    // normal killer attribution; any survivor is then ended mechanically.
                    target.setHealth(0.0);
                }
            } finally {
                pendingFinishes.remove(targetId);
            }
        });
    }
}
