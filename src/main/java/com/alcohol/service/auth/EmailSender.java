package com.alcohol.service.auth;

public interface EmailSender {

    void sendOtp(String email, String code, String locale);
}
