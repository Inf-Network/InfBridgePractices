package net.infnetwork.snowball.bridgingskin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.connection.PlayerConnection;
import io.papermc.paper.connection.PlayerLoginConnection;
import java.lang.reflect.Proxy;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LoginIdentityResolverTest {
    private static final UUID PLAYER_UUID =
            UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @Test
    void resolvesAuthenticatedLoginProfile() {
        PlayerProfile profile = profile(PLAYER_UUID, "Snowball_233");
        PlayerLoginConnection connection = proxy(
                PlayerLoginConnection.class,
                (ignored, method, arguments) -> method.getName().equals("getAuthenticatedProfile")
                        ? profile
                        : defaultValue(method.getReturnType()));

        LoginIdentityResolver.Resolution result = LoginIdentityResolver.resolve(connection);

        assertTrue(result.resolved());
        assertEquals(PLAYER_UUID, result.uuid());
        assertEquals("Snowball_233", result.name());
    }

    @Test
    void resolvesConfigurationProfile() {
        PlayerProfile profile = profile(PLAYER_UUID, "Snowball_233");
        PlayerConfigurationConnection connection = proxy(
                PlayerConfigurationConnection.class,
                (ignored, method, arguments) -> method.getName().equals("getProfile")
                        ? profile
                        : defaultValue(method.getReturnType()));

        assertTrue(LoginIdentityResolver.resolve(connection).resolved());
    }

    @Test
    void rejectsUnknownConnectionSubtype() {
        PlayerConnection connection = proxy(
                PlayerConnection.class,
                (ignored, method, arguments) -> defaultValue(method.getReturnType()));

        LoginIdentityResolver.Resolution result = LoginIdentityResolver.resolve(connection);

        assertFalse(result.resolved());
        assertTrue(result.failureReason().contains("不支持的登录连接类型"));
    }

    @Test
    void rejectsMissingProfile() {
        PlayerLoginConnection connection = proxy(
                PlayerLoginConnection.class,
                (ignored, method, arguments) -> defaultValue(method.getReturnType()));

        LoginIdentityResolver.Resolution result = LoginIdentityResolver.resolve(connection);

        assertFalse(result.resolved());
        assertTrue(result.failureReason().contains("尚未提供已认证玩家档案"));
    }

    @Test
    void rejectsMissingUuidOrName() {
        PlayerLoginConnection missingUuid = loginConnection(profile(null, "Snowball_233"));
        PlayerLoginConnection missingName = loginConnection(profile(PLAYER_UUID, null));

        assertTrue(LoginIdentityResolver.resolve(missingUuid)
                .failureReason().contains("缺少 UUID"));
        assertTrue(LoginIdentityResolver.resolve(missingName)
                .failureReason().contains("缺少名称"));
    }

    private static PlayerLoginConnection loginConnection(PlayerProfile profile) {
        return proxy(
                PlayerLoginConnection.class,
                (ignored, method, arguments) -> method.getName().equals("getAuthenticatedProfile")
                        ? profile
                        : defaultValue(method.getReturnType()));
    }

    private static PlayerProfile profile(UUID uuid, String name) {
        return proxy(PlayerProfile.class, (ignored, method, arguments) -> switch (method.getName()) {
            case "getId" -> uuid;
            case "getName" -> name;
            default -> defaultValue(method.getReturnType());
        });
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(
                type.getClassLoader(), new Class<?>[]{type}, handler));
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }
}
