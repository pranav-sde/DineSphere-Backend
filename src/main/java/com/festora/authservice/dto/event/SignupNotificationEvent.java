package com.festora.authservice.dto.event;

import com.festora.authservice.model.User;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class SignupNotificationEvent extends ApplicationEvent {

    private final User user;

    public SignupNotificationEvent(Object source, User user) {
        super(source);
        this.user = user;
    }
}
