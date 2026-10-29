package com.infotact.venuevista.service;

import com.infotact.venuevista.model.*;
import com.infotact.venuevista.repo.EventRepository;
import com.infotact.venuevista.repo.OrderRepository;
import com.infotact.venuevista.repo.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class BookingService {
    public static final int MAX_SEATS = 8;
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RNG = new SecureRandom();

    public record SeatRef(Long sectionId, int row, int seat) {}

    private final EventRepository events;
    private final OrderRepository orders;
    private final TicketRepository tickets;

    public BookingService(EventRepository events, OrderRepository orders, TicketRepository tickets) {
        this.events = events;
        this.orders = orders;
        this.tickets = tickets;
    }

    public static String code(String prefix, int len) {
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < len; i++) sb.append(ALPHABET.charAt(RNG.nextInt(ALPHABET.length())));
        return sb.toString();
    }

    /**
     * Books the seats atomically. A pre-check gives friendly errors; the unique constraint on Ticket
     * guards against concurrent buyers (callers should handle DataIntegrityViolationException).
     */
    @Transactional
    public Order book(Long eventId, String name, String email, List<SeatRef> seats) {
        Event ev = events.findById(eventId).orElseThrow(() -> new BookingException("Event not found."));
        if (!ev.isOnSale()) throw new BookingException("Tickets for this event are not on sale.");
        if (name == null || name.isBlank()) throw new BookingException("Please enter your name.");
        if (email == null || !EMAIL.matcher(email.trim()).matches()) throw new BookingException("Please enter a valid email.");
        if (seats == null || seats.isEmpty()) throw new BookingException("Select at least one seat.");
        if (seats.size() > MAX_SEATS) throw new BookingException("You can book at most " + MAX_SEATS + " seats at a time.");
        if (new HashSet<>(seats).size() != seats.size()) throw new BookingException("Duplicate seats selected.");

        Map<Long, Section> sections = new HashMap<>();
        ev.getVenue().getSections().forEach(s -> sections.put(s.getId(), s));

        Set<String> taken = new HashSet<>();
        for (Object[] r : tickets.takenSeats(eventId)) taken.add(r[0] + "-" + r[1] + "-" + r[2]);

        Order order = new Order();
        order.setReference(code("VVO-", 8));
        order.setEvent(ev);
        order.setCustomerName(name.trim());
        order.setEmail(email.trim().toLowerCase());

        int total = 0;
        for (SeatRef ref : seats) {
            Section s = sections.get(ref.sectionId());
            if (s == null || ref.row() < 1 || ref.row() > s.getRowsCount() || ref.seat() < 1 || ref.seat() > s.getSeatsPerRow())
                throw new BookingException("Invalid seat selected.");
            if (taken.contains(ref.sectionId() + "-" + ref.row() + "-" + ref.seat()))
                throw new BookingException("Sorry, " + s.getName() + " row " + Seats.rowLabel(ref.row()) + " seat " + ref.seat() + " was just taken.");
            Ticket t = new Ticket();
            t.setCode(code("VV-", 10));
            t.setEvent(ev);
            t.setSection(s);
            t.setRowNo(ref.row());
            t.setSeatNo(ref.seat());
            t.setPrice(ev.priceFor(s.getTier()));
            total += t.getPrice();
            order.addTicket(t);
        }
        order.setTotal(total);
        return orders.saveAndFlush(order);
    }

    /** Validates a ticket at the gate. */
    @Transactional
    public Map<String, Object> checkIn(String rawCode) {
        Map<String, Object> out = new LinkedHashMap<>();
        String code = rawCode == null ? "" : rawCode.trim().toUpperCase();
        Optional<Ticket> opt = tickets.findByCode(code);
        if (opt.isEmpty()) {
            out.put("status", "INVALID");
            out.put("message", "No ticket matches that code.");
            return out;
        }
        Ticket t = opt.get();
        out.put("code", t.getCode());
        out.put("event", t.getEvent().getTitle());
        out.put("holder", t.getOrder().getCustomerName());
        out.put("seat", t.getSection().getName() + " · Row " + t.getRowLabel() + " · Seat " + t.getSeatNo());
        out.put("tier", t.getSection().getTier().getLabel());
        if (t.getEvent().getStatus() == EventStatus.CANCELLED) {
            out.put("status", "INVALID");
            out.put("message", "This event was cancelled.");
        } else if (t.isCheckedIn()) {
            out.put("status", "ALREADY");
            out.put("message", "Already scanned at " + t.getCheckedInAt().toString().replace('T', ' ').substring(0, 16));
        } else {
            t.setCheckedIn(true);
            t.setCheckedInAt(java.time.LocalDateTime.now());
            out.put("status", "OK");
            out.put("message", "Welcome in! Ticket validated.");
        }
        return out;
    }
}
