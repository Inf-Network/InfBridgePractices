package net.infnetwork.snowball.bridgingskin;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.connection.PlayerConnection;
import io.papermc.paper.connection.PlayerLoginConnection;
import java.util.UUID;

final class LoginIdentityResolver {
    private LoginIdentityResolver() {
    }

    static Resolution resolve(PlayerConnection connection) {
        if (connection == null) {
            return Resolution.failure("登录事件没有连接对象");
        }

        PlayerProfile profile;
        if (connection instanceof PlayerConfigurationConnection configurationConnection) {
            profile = configurationConnection.getProfile();
        } else if (connection instanceof PlayerLoginConnection loginConnection) {
            profile = loginConnection.getAuthenticatedProfile();
        } else {
            return Resolution.failure(
                    "不支持的登录连接类型: " + connection.getClass().getName());
        }

        String connectionType = connection.getClass().getName();
        if (profile == null) {
            return Resolution.failure("登录连接尚未提供已认证玩家档案: " + connectionType);
        }
        UUID uuid = profile.getId();
        if (uuid == null) {
            return Resolution.failure("已认证玩家档案缺少 UUID: " + connectionType);
        }
        String name = profile.getName();
        if (name == null || name.isBlank()) {
            return Resolution.failure("已认证玩家档案缺少名称: " + connectionType);
        }
        return Resolution.success(uuid, name);
    }

    record Resolution(UUID uuid, String name, String failureReason) {
        static Resolution success(UUID uuid, String name) {
            return new Resolution(uuid, name, null);
        }

        static Resolution failure(String reason) {
            return new Resolution(null, null, reason);
        }

        boolean resolved() {
            return failureReason == null;
        }
    }
}
