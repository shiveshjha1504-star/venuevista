package com.infotact.venuevista.service;

import com.infotact.venuevista.model.*;
import com.infotact.venuevista.repo.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class CatalogService {
    private final TicketRepository tickets;

    public CatalogService(TicketRepository tickets) { this.tickets = tickets; }

    public Map<Long, Long> soldCounts() {
        Map<Long, Long> m = new HashMap<>();
        for (Object[] r : tickets.soldCounts()) m.put((Long) r[0], (Long) r[1]);
        return m;
    }

    public List<EventCard> cards(List<Event> events) {
        Map<Long, Long> sold = soldCounts();
        return events.stream()
                .map(e -> new EventCard(e, sold.getOrDefault(e.getId(), 0L), e.getVenue().getCapacity()))
                .toList();
    }

    public EventCard card(Event e) { return cards(List.of(e)).get(0); }

    /** Seating chart payload consumed by seatmap.js */
    public Map<String, Object> seatMap(Event ev) {
        Venue v = ev.getVenue();
        List<Map<String, Object>> bands = new ArrayList<>();
        for (Tier t : Tier.values()) {
            List<Map<String, Object>> secs = v.getSections().stream()
                    .filter(s -> s.getTier() == t)
                    .map(s -> Map.<String, Object>of("id", s.getId(), "name", s.getName(),
                            "rows", s.getRowsCount(), "seats", s.getSeatsPerRow()))
                    .toList();
            if (!secs.isEmpty()) {
                bands.add(Map.of("tier", t.name(), "label", t.getLabel(), "price", ev.priceFor(t), "sections", secs));
            }
        }
        List<String> taken = tickets.takenSeats(ev.getId()).stream().map(r -> r[0] + "-" + r[1] + "-" + r[2]).toList();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("eventId", ev.getId());
        out.put("open", ev.isOnSale());
        out.put("max", BookingService.MAX_SEATS);
        out.put("bands", bands);
        out.put("taken", taken);
        return out;
    }
}
