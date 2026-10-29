package com.infotact.venuevista.web;

import com.infotact.venuevista.model.*;
import com.infotact.venuevista.repo.*;
import com.infotact.venuevista.service.CatalogService;
import com.infotact.venuevista.service.EventCard;
import com.infotact.venuevista.service.QrService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;
import java.util.*;

@Controller
public class PublicController {
    private final EventRepository events;
    private final VenueRepository venues;
    private final OrderRepository orders;
    private final TicketRepository tickets;
    private final CatalogService catalog;
    private final QrService qr;

    public PublicController(EventRepository events, VenueRepository venues, OrderRepository orders,
                            TicketRepository tickets, CatalogService catalog, QrService qr) {
        this.events = events;
        this.venues = venues;
        this.orders = orders;
        this.tickets = tickets;
        this.catalog = catalog;
        this.qr = qr;
    }

    private List<Event> onSale() {
        return events.findByStatusOrderByStartTimeAsc(EventStatus.PUBLISHED).stream().filter(Event::isUpcoming).toList();
    }

    @GetMapping("/")
    public String home(Model m) {
        List<EventCard> cards = catalog.cards(onSale());
        m.addAttribute("featured", cards.stream().limit(3).toList());
        m.addAttribute("upcoming", cards.stream().skip(3).limit(6).toList());
        m.addAttribute("venues", venues.findAll());
        m.addAttribute("statEvents", cards.size());
        m.addAttribute("statVenues", venues.count());
        m.addAttribute("statSeats", venues.findAll().stream().mapToInt(Venue::getCapacity).sum());
        m.addAttribute("statTickets", tickets.count());
        m.addAttribute("categories", cards.stream().map(c -> c.getEvent().getCategory()).distinct().toList());
        return "index";
    }

    @GetMapping("/events")
    public String list(@RequestParam(required = false) String q, @RequestParam(required = false) String category, Model m) {
        List<EventCard> all = catalog.cards(onSale());
        String needle = q == null ? "" : q.trim().toLowerCase();
        List<EventCard> shown = all.stream()
                .filter(c -> category == null || category.isBlank() || category.equalsIgnoreCase(c.getEvent().getCategory()))
                .filter(c -> needle.isEmpty() || (c.getEvent().getTitle() + " " + c.getEvent().getPerformer() + " "
                        + c.getEvent().getVenue().getName() + " " + c.getEvent().getVenue().getCity()).toLowerCase().contains(needle))
                .toList();
        m.addAttribute("cards", shown);
        m.addAttribute("categories", all.stream().map(c -> c.getEvent().getCategory()).distinct().sorted().toList());
        m.addAttribute("q", q == null ? "" : q);
        m.addAttribute("category", category == null ? "" : category);
        return "events";
    }

    @GetMapping("/events/{id}")
    public String detail(@PathVariable Long id, Model m) {
        Event e = events.findById(id).orElse(null);
        if (e == null || e.getStatus() == EventStatus.DRAFT) return "redirect:/events";
        m.addAttribute("card", catalog.card(e));
        m.addAttribute("e", e);
        m.addAttribute("tiers", Tier.values());
        return "event";
    }

    @GetMapping("/order/{ref}")
    public String order(@PathVariable String ref, Model m) {
        Optional<Order> o = orders.findByReference(ref.toUpperCase());
        if (o.isEmpty()) return "redirect:/lookup";
        m.addAttribute("o", o.get());
        return "order";
    }

    @GetMapping("/lookup")
    public String lookup() { return "lookup"; }

    @PostMapping("/lookup")
    public String doLookup(@RequestParam String reference, @RequestParam String email, RedirectAttributes ra) {
        Optional<Order> o = orders.findByReference(reference.trim().toUpperCase());
        if (o.isPresent() && o.get().getEmail().equalsIgnoreCase(email.trim())) {
            return "redirect:/order/" + o.get().getReference();
        }
        ra.addFlashAttribute("error", "We couldn't find an order with that reference and email.");
        return "redirect:/lookup";
    }

    @GetMapping(value = "/qr/{code}", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> qr(@PathVariable String code) {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .body(qr.png(code.toUpperCase(), 280));
    }
}
