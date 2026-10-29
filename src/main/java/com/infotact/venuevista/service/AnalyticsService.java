package com.infotact.venuevista.service;

import com.infotact.venuevista.model.*;
import com.infotact.venuevista.repo.EventRepository;
import com.infotact.venuevista.repo.OrderRepository;
import com.infotact.venuevista.repo.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {
    private final EventRepository events;
    private final OrderRepository orders;
    private final TicketRepository tickets;

    public AnalyticsService(EventRepository events, OrderRepository orders, TicketRepository tickets) {
        this.events = events;
        this.orders = orders;
        this.tickets = tickets;
    }

    public Map<String, Object> dashboard() {
        List<Event> allEvents = events.findAll();
        List<Order> allOrders = orders.findAllByOrderByCreatedAtDesc();
        List<Ticket> allTickets = tickets.findAll();

        long revenue = allOrders.stream().mapToLong(Order::getTotal).sum();
        long sold = allTickets.size();
        long checkedIn = allTickets.stream().filter(Ticket::isCheckedIn).count();

        Map<Long, List<Ticket>> byEvent = allTickets.stream().collect(Collectors.groupingBy(t -> t.getEvent().getId()));

        List<Event> live = allEvents.stream().filter(e -> e.getStatus() == EventStatus.PUBLISHED).toList();
        long liveCap = live.stream().mapToLong(e -> e.getVenue().getCapacity()).sum();
        long liveSold = live.stream().mapToLong(e -> byEvent.getOrDefault(e.getId(), List.of()).size()).sum();

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("revenue", revenue);
        kpis.put("tickets", sold);
        kpis.put("orders", allOrders.size());
        kpis.put("avgOrder", allOrders.isEmpty() ? 0 : revenue / allOrders.size());
        kpis.put("liveEvents", allEvents.stream().filter(Event::isOnSale).count());
        kpis.put("occupancy", liveCap == 0 ? 0 : Math.round(liveSold * 1000.0 / liveCap) / 10.0);
        kpis.put("checkedIn", checkedIn);

        // revenue per day, last 30 days
        LocalDate today = LocalDate.now();
        Map<LocalDate, long[]> perDay = new TreeMap<>();
        for (int i = 29; i >= 0; i--) perDay.put(today.minusDays(i), new long[]{0, 0});
        for (Order o : allOrders) {
            long[] cell = perDay.get(o.getCreatedAt().toLocalDate());
            if (cell != null) { cell[0] += o.getTotal(); cell[1] += o.getTicketCount(); }
        }
        DateTimeFormatter lbl = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
        List<Map<String, Object>> daily = new ArrayList<>();
        perDay.forEach((d, v) -> daily.add(Map.of("date", d.toString(), "label", d.format(lbl), "revenue", v[0], "tickets", v[1])));

        // by tier
        List<Map<String, Object>> tiers = new ArrayList<>();
        for (Tier t : Tier.values()) {
            List<Ticket> ts = allTickets.stream().filter(x -> x.getSection().getTier() == t).toList();
            tiers.add(Map.of("tier", t.name(), "label", t.getLabel(), "sold", ts.size(),
                    "revenue", ts.stream().mapToLong(Ticket::getPrice).sum()));
        }

        // by event
        List<Map<String, Object>> evRows = new ArrayList<>();
        for (Event e : allEvents) {
            List<Ticket> ts = byEvent.getOrDefault(e.getId(), List.of());
            int cap = e.getVenue().getCapacity();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", e.getId());
            row.put("title", e.getTitle());
            row.put("category", e.getCategory());
            row.put("venue", e.getVenue().getName());
            row.put("start", e.getStartTime().format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)));
            row.put("status", e.getStatus().name());
            row.put("past", !e.isUpcoming());
            row.put("sold", ts.size());
            row.put("capacity", cap);
            row.put("revenue", ts.stream().mapToLong(Ticket::getPrice).sum());
            row.put("occupancy", cap == 0 ? 0 : Math.round(ts.size() * 1000.0 / cap) / 10.0);
            row.put("hue1", e.getHue1());
            row.put("hue2", e.getHue2());
            evRows.add(row);
        }
        evRows.sort((a, b) -> Long.compare((Long) b.get("revenue"), (Long) a.get("revenue")));

        // by category
        Map<String, long[]> cat = new TreeMap<>();
        for (Ticket t : allTickets) {
            String c = Optional.ofNullable(t.getEvent().getCategory()).orElse("Other");
            long[] cell = cat.computeIfAbsent(c, k -> new long[2]);
            cell[0] += t.getPrice();
            cell[1]++;
        }
        List<Map<String, Object>> categories = new ArrayList<>();
        cat.forEach((k, v) -> categories.add(Map.of("name", k, "revenue", v[0], "tickets", v[1])));
        categories.sort((a, b) -> Long.compare((Long) b.get("revenue"), (Long) a.get("revenue")));

        // recent orders
        List<Map<String, Object>> recent = allOrders.stream().limit(8).map(o -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ref", o.getReference());
            m.put("customer", o.getCustomerName());
            m.put("event", o.getEvent().getTitle());
            m.put("tickets", o.getTicketCount());
            m.put("total", o.getTotal());
            m.put("at", o.getCreatedAt().format(DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.ENGLISH)));
            return m;
        }).toList();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("kpis", kpis);
        out.put("daily", daily);
        out.put("tiers", tiers);
        out.put("events", evRows);
        out.put("categories", categories);
        out.put("recent", recent);
        return out;
    }
}
