package net.infnetwork.snowball.bridgingskin.crate;

public final class CrateSelectionException extends Exception {
    private static final long serialVersionUID = 1L;

    public CrateSelectionException(String message) {
        super(message);
    }

    public CrateSelectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
