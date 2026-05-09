package com.festora.authservice.service;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    @Value("${resend.api-key}")
    private String resendApiKey;

    private final OkHttpClient httpClient = new OkHttpClient();

    private static final String RESEND_API_URL = "https://api.resend.com/emails";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Async
    public void sendOtpEmail(String to, String otp) {
        String body = """
                {
                  "from": "DineSphere <onboarding@resend.dev>",
                  "to": ["%s"],
                  "subject": "DineSphere - Your Verification Code",
                  "text": "Welcome to DineSphere Elite Network!\\n\\nYour verification code is: %s\\nThis code will expire in 5 minutes.\\n\\nIf you didn't request this code, please ignore this email."
                }
                """.formatted(to, otp);

        Request request = new Request.Builder()
                .url(RESEND_API_URL)
                .addHeader("Authorization", "Bearer " + resendApiKey)
                .post(RequestBody.create(body, JSON))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                log.info("OTP email sent successfully to {} via Resend", to);
            } else {
                String responseBody = response.body() != null ? response.body().string() : "empty";
                log.error("Resend API error for {}: HTTP {} - {}", to, response.code(), responseBody);
            }
        } catch (Exception e) {
            log.error("CRITICAL: Failed to call Resend API for {}. Reason: {}", to, e.getMessage());
        }
    }
}
