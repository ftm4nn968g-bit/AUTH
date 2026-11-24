package kz.onlineshop.auth.service;

import java.time.Instant;

/** Аккаунт brute-force қорғауымен уақытша бұғатталған. */
public class AccountLockedException extends RuntimeException {

    private final Instant lockedUntil;

    public AccountLockedException(Instant lockedUntil) {
        super("Account is locked until " + lockedUntil);
        this.lockedUntil = lockedUntil;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }
}
