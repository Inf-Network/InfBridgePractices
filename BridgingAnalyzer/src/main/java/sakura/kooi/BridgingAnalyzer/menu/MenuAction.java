package sakura.kooi.BridgingAnalyzer.menu;

import java.util.Objects;

sealed interface MenuAction {
    record Open(Screen screen) implements MenuAction {
        public Open {
            Objects.requireNonNull(screen, "screen");
        }
    }

    record PlayerCommand(String command) implements MenuAction {
        public PlayerCommand {
            command = validateCommand(command);
        }
    }

    record ConsoleCommand(String command) implements MenuAction {
        public ConsoleCommand {
            command = validateCommand(command);
        }
    }

    record Paid(double cost, MenuAction action, boolean closeOnDeny) implements MenuAction {
        public Paid {
            if (!Double.isFinite(cost) || cost <= 0.0D) {
                throw new IllegalArgumentException("菜单付费金额必须是正有限数");
            }
            Objects.requireNonNull(action, "action");
        }
    }

    record Connect(String serverName) implements MenuAction {
        public Connect {
            if (serverName == null || !serverName.matches("[A-Za-z0-9_.-]+")) {
                throw new IllegalArgumentException("代理服务器名无效");
            }
        }
    }

    enum ClearAll implements MenuAction {
        INSTANCE
    }

    enum Close implements MenuAction {
        INSTANCE
    }

    enum Screen {
        MAIN,
        WARP
    }

    private static String validateCommand(String command) {
        if (command == null) {
            throw new IllegalArgumentException("菜单命令不能为 null");
        }
        String normalized = command.strip();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1).stripLeading();
        }
        if (normalized.isEmpty() || normalized.indexOf('\n') >= 0 || normalized.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("菜单命令无效");
        }
        return normalized;
    }
}
