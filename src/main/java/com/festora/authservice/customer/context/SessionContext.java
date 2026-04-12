package com.festora.authservice.customer.context;

import com.festora.authservice.customer.dto.SessionData;
import org.springframework.stereotype.Component;

@Component
public class SessionContext {

    private static final ThreadLocal<SessionData> CONTEXT = new ThreadLocal<>();

    public static void set(SessionData data) {
        CONTEXT.set(data);
    }

    public static SessionData get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
