package com.festora.authservice.exception;

public class AccountDisabledException extends RuntimeException {
    public AccountDisabledException() {
        super("Account disabled");
    }
}

