package net.infnetwork.snowball.bridginganalyzer.menu;

import org.bukkit.entity.Player;

/** Supplies the three dynamic lines rendered on the profile head. */
@FunctionalInterface
public interface MenuProfileProvider {
    ProfileSnapshot profile(Player player);

    record ProfileSnapshot(String group, String level, String balance) {
        public ProfileSnapshot {
            group = fallback(group);
            level = fallback(level);
            balance = fallback(balance);
        }

        public static ProfileSnapshot unknown() {
            return new ProfileSnapshot("未知", "未知", "未知");
        }

        private static String fallback(String value) {
            return value == null || value.isBlank() ? "未知" : value;
        }
    }
}
