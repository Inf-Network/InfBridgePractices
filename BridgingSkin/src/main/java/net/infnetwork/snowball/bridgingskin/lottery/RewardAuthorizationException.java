package net.infnetwork.snowball.bridgingskin.lottery;

public final class RewardAuthorizationException extends Exception {
    private static final long serialVersionUID = 1L;

    public RewardAuthorizationException(String message) {
        super(message);
    }

    public RewardAuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
