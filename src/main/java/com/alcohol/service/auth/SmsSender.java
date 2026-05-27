package com.alcohol.service.auth;

public interface SmsSender {

    void sendOtp(String phoneE164, String code, String locale);
}
