package com.festora.kitchenservice.enums;

public enum TicketStatus {
    OPEN,           // Just received, not started
    IN_PROGRESS,    // Kitchen/bar working on it
    READY,          // Done, captain notified
    PICKED_UP       // Captain collected and served
}
