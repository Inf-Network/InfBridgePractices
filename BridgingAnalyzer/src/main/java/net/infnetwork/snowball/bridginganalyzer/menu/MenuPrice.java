package net.infnetwork.snowball.bridginganalyzer.menu;

import java.math.BigDecimal;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;

final class MenuPrice {
    private MenuPrice() {
    }

    static double read(ConfigurationSection config, String path, double fallback,
                       String displayPath) {
        Objects.requireNonNull(config, "config");
        Object configured = config.get(path);
        if (configured == null) {
            return requireValid(fallback, displayPath);
        }
        if (!(configured instanceof Number number)) {
            throw new IllegalArgumentException(displayPath + " 必须是数值");
        }
        return requireValid(number.doubleValue(), displayPath);
    }

    static double requireValid(double amount, String displayPath) {
        if (!Double.isFinite(amount) || amount < 0.0D) {
            throw new IllegalArgumentException(displayPath + " 必须是非负有限数");
        }
        return amount == 0.0D ? 0.0D : amount;
    }

    static String format(double amount) {
        double valid = requireValid(amount, "菜单金额");
        return BigDecimal.valueOf(valid).stripTrailingZeros().toPlainString();
    }
}
