package net.infnetwork.snowball.bridgingskin.storage;

public class SkinStorageException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SkinStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public SkinStorageException(String message) {
        super(message);
    }
}
