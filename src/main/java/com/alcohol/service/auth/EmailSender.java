package com.alcohol.service.auth;

public interface EmailSender {

    void sendOtp(String email, String code, String locale);

    default void sendWelcome(String email, String displayName, String locale) {
        // Optional for providers that do not implement welcome mail yet.
    }
}
