package com.infotact.venuevista.service;

import com.infotact.venuevista.model.*;
import com.infotact.venuevista.repo.EventRepository;
import com.infotact.venuevista.repo.OrderRepository;
import com.infotact.venuevista.repo.VenueRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** Loads three venues, a season of events and a month of realistic sales on first start. */
@Component
public class SeedData implements CommandLineRunner {
    private static final String[] FIRST = {"Aarav", "Ananya", "Vihaan", "Diya", "Kabir", "Meera", "Rohan", "Ishita", "Arjun", "Saanvi",
            "Neel", "Tara", "Dev", "Riya", "Kiran", "Naina", "Veer", "Zoya", "Aditya", "Mira", "Karan", "Pooja", "Imran", "Sneha"};
    private static final String[] LAST = {"Sharma", "Iyer", "Reddy", "Menon", "Kapoor", "Nair", "Das", "Patel", "Khan", "Rao",
            "Banerjee", "Gupta", "Joshi", "Singh", "Pillai", "Mehta"};

    private final VenueRepository venues;
    private final EventRepository events;
    private final OrderRepository orders;
    private final TransactionTemplate tx;

    public SeedData(VenueRepository venues, EventRepository events, OrderRepository orders, TransactionTemplate tx) {
        this.venues = venues;
        this.events = events;
        this.orders = orders;
        this.tx = tx;
    }

    @Override
    public void run(String... args) {
        if (venues.count() > 0) return;
        tx.executeWithoutResult(s -> seed());
    }

    private void seed() {
        Venue aurora = venue("Aurora Arena", "Bengaluru", "Whitefield Main Rd, Bengaluru, Karnataka",
                "A 2,000-seat indoor arena with a thrust stage, tiered terraces and a cathedral-high lighting rig.");
        aurora.addSection(new Section("Royal Circle", Tier.VIP, 4, 14));
        aurora.addSection(new Section("Gold Left", Tier.PREMIUM, 6, 8));
        aurora.addSection(new Section("Gold Centre", Tier.PREMIUM, 6, 12));
        aurora.addSection(new Section("Gold Right", Tier.PREMIUM, 6, 8));
        aurora.addSection(new Section("Terrace A", Tier.STANDARD, 7, 9));
        aurora.addSection(new Section("Terrace B", Tier.STANDARD, 7, 12));
        aurora.addSection(new Section("Terrace C", Tier.STANDARD, 7, 9));

        Venue lantern = venue("The Lantern Theatre", "Mumbai", "Kala Ghoda, Mumbai, Maharashtra",
                "A restored 1930s proscenium theatre. Velvet seats, perfect sightlines, zero bad rows.");
        lantern.addSection(new Section("Front Stalls", Tier.VIP, 3, 12));
        lantern.addSection(new Section("Stalls Left", Tier.PREMIUM, 6, 6));
        lantern.addSection(new Section("Stalls Centre", Tier.PREMIUM, 6, 10));
        lantern.addSection(new Section("Stalls Right", Tier.PREMIUM, 6, 6));
        lantern.addSection(new Section("Balcony", Tier.STANDARD, 6, 18));

        Venue skyline = venue("Skyline Open Air", "Hyderabad", "Gachibowli Grounds, Hyderabad, Telangana",
                "Rooftop amphitheatre under the stars with a city-skyline backdrop.");
        skyline.addSection(new Section("Front Deck", Tier.VIP, 3, 16));
        skyline.addSection(new Section("Lounge", Tier.PREMIUM, 5, 14));
        skyline.addSection(new Section("Lawn West", Tier.STANDARD, 6, 10));
        skyline.addSection(new Section("Lawn East", Tier.STANDARD, 6, 10));
        venues.saveAll(List.of(aurora, lantern, skyline));

        List<Event> evs = new ArrayList<>();
        // past events (feed the analytics) -----------------------------------------------------------
        evs.add(event("Monsoon Echoes Tour", "Neon Monsoon", "Concert", aurora, -18, 19, 30, 4500, 2800, 1200, 195, 305, EventStatus.PUBLISHED,
                "An indie-electronic odyssey: analog synths, monsoon field recordings and a visual show projected across the whole arena."));
        evs.add(event("Midnight Raga Sessions", "The Tabla Collective", "Concert", lantern, -9, 21, 0, 3200, 1800, 900, 28, 350, EventStatus.PUBLISHED,
                "Classical meets club: a late-night fusion set that begins in a raga and ends on a bass drop."));
        evs.add(event("Open Mic Finals", "City Comedy Circuit", "Comedy", skyline, -4, 20, 0, 2400, 1400, 600, 150, 10, EventStatus.PUBLISHED,
                "The ten funniest voices from the city's open mics battle it out under the open sky."));
        // upcoming --------------------------------------------------------------------------------------
        evs.add(event("Synthwave Sunrise", "Kavya & The Static", "Concert", aurora, 4, 19, 0, 5200, 3200, 1500, 285, 20, EventStatus.PUBLISHED,
                "Retro-futurist pop with a 12-piece band, laser harps and a surprise guest list. Neon encouraged."));
        evs.add(event("Hamlet, Reimagined", "Rangmanch Ensemble", "Theatre", lantern, 7, 19, 0, 3800, 2400, 1100, 345, 15, EventStatus.PUBLISHED,
                "Shakespeare's prince in a Mumbai high-rise boardroom. Ninety unbroken minutes of ambition, doubt and chai."));
        evs.add(event("Laugh Riot Live", "Vikram & Friends", "Comedy", skyline, 10, 20, 30, 2800, 1600, 700, 52, 320, EventStatus.PUBLISHED,
                "A rooftop night of stand-up, improv and audience-powered chaos. Strictly no front-row heckling."));
        evs.add(event("DevSummit Bengaluru", "Infotact Developer Community", "Conference", aurora, 16, 9, 30, 6000, 3500, 1800, 210, 160, EventStatus.PUBLISHED,
                "One day, five tracks: cloud, AI, web, data and security. Talks, live-coding, and a hallway track worth the ticket alone."));
        evs.add(event("Starlight Cinema: Live Orchestra", "Lantern Philharmonic", "Concert", lantern, 23, 19, 0, 4200, 2600, 1300, 260, 330, EventStatus.PUBLISHED,
                "Your favourite film scores performed live by a 60-piece orchestra while the picture plays above them."));
        evs.add(event("Desert Bass Festival", "Dune Collective", "Festival", skyline, 37, 17, 0, 5500, 3300, 1600, 35, 300, EventStatus.PUBLISHED,
                "Eight hours, three stages of light, one skyline. Techno, house and Indian electronica under the stars."));
        evs.add(event("Indie Folk Night (Draft)", "Parvati Lane", "Concert", lantern, 45, 20, 0, 2200, 1500, 800, 100, 200, EventStatus.DRAFT,
                "Acoustic evening - announcement coming soon."));
        events.saveAll(evs);

        Random rnd = new Random(2026);
        double[] fill = {0.86, 0.78, 0.92, 0.46, 0.55, 0.38, 0.30, 0.22, 0.12, 0};
        List<Order> batch = new ArrayList<>();
        for (int i = 0; i < evs.size(); i++) {
            if (fill[i] > 0) batch.addAll(sales(evs.get(i), fill[i], rnd));
        }
        orders.saveAll(batch);
    }

    private Venue venue(String name, String city, String address, String desc) {
        Venue v = new Venue();
        v.setName(name);
        v.setCity(city);
        v.setAddress(address);
        v.setDescription(desc);
        return v;
    }

    private Event event(String title, String performer, String category, Venue v, int dayOffset, int h, int m,
                        int vip, int prem, int std, int hue1, int hue2, EventStatus status, String desc) {
        Event e = new Event();
        e.setTitle(title);
        e.setPerformer(performer);
        e.setCategory(category);
        e.setVenue(v);
        e.setStartTime(LocalDate.now().plusDays(dayOffset).atTime(h, m));
        e.setPriceVip(vip);
        e.setPricePremium(prem);
        e.setPriceStandard(std);
        e.setHue1(hue1);
        e.setHue2(hue2);
        e.setStatus(status);
        e.setDescription(desc);
        return e;
    }

    private List<Order> sales(Event ev, double fillRatio, Random rnd) {
        List<Section> secs = ev.getVenue().getSections();
        int capacity = ev.getVenue().getCapacity();
        int target = (int) (capacity * fillRatio);
        Set<String> taken = new HashSet<>();
        List<Order> out = new ArrayList<>();
        boolean past = !ev.isUpcoming();
        LocalDateTime now = LocalDateTime.now();
        int sold = 0, guard = 0;
        while (sold < target && guard++ < 20000) {
            Section s = secs.get(rnd.nextInt(secs.size()));
            // cheaper tiers sell more
            if (s.getTier() == Tier.VIP && rnd.nextInt(3) == 0) continue;
            int qty = 1 + (rnd.nextInt(10) < 4 ? 1 : 0) + (rnd.nextInt(10) < 3 ? 1 : 0) + (rnd.nextInt(10) < 1 ? 2 : 0);
            int row = 1 + rnd.nextInt(s.getRowsCount());
            int start = 1 + rnd.nextInt(Math.max(1, s.getSeatsPerRow() - qty + 1));
            if (start + qty - 1 > s.getSeatsPerRow()) continue;
            boolean free = true;
            for (int k = 0; k < qty; k++) if (taken.contains(s.getId() + "-" + row + "-" + (start + k))) free = false;
            if (!free) continue;

            String name = FIRST[rnd.nextInt(FIRST.length)] + " " + LAST[rnd.nextInt(LAST.length)];
            Order o = new Order();
            o.setReference(BookingService.code("VVO-", 8));
            o.setEvent(ev);
            o.setCustomerName(name);
            o.setEmail(name.toLowerCase().replace(' ', '.') + "@example.com");
            LocalDateTime created = past
                    ? ev.getStartTime().minusHours(3 + rnd.nextInt(24 * 21))
                    : now.minusHours(1 + rnd.nextInt(24 * 28));
            if (created.isAfter(now)) created = now.minusHours(2);
            o.setCreatedAt(created);
            int total = 0;
            for (int k = 0; k < qty; k++) {
                Ticket t = new Ticket();
                t.setCode(BookingService.code("VV-", 10));
                t.setEvent(ev);
                t.setSection(s);
                t.setRowNo(row);
                t.setSeatNo(start + k);
                t.setPrice(ev.priceFor(s.getTier()));
                if (past && rnd.nextInt(100) < 91) {
                    t.setCheckedIn(true);
                    t.setCheckedInAt(ev.getStartTime().minusMinutes(5 + rnd.nextInt(50)));
                }
                taken.add(s.getId() + "-" + row + "-" + (start + k));
                total += t.getPrice();
                o.addTicket(t);
            }
            o.setTotal(total);
            out.add(o);
            sold += qty;
        }
        return out;
    }
}
