package com.infotact.venuevista.service;

import com.infotact.venuevista.model.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** View model for an event tile: the event plus live sales figures. */
@Getter @AllArgsConstructor
public class EventCard {
    private final Event event;
    private final long sold;
    private final int capacity;

    public int getPct() { return capacity == 0 ? 0 : (int) Math.round(sold * 100.0 / capacity); }
    public boolean isSoldOut() { return capacity > 0 && sold >= capacity; }
    public long getLeft() { return Math.max(0, capacity - sold); }
}
