package net.infnetwork.snowball.bridgingskin.storage;

/** An identity cannot be adopted uniquely; callers must not create or save fallback data. */
public final class SkinIdentityConflictException extends SkinStorageException {
    private static final long serialVersionUID = 1L;

    public SkinIdentityConflictException(String message) {
        super(message);
    }

    public SkinIdentityConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
