package com.infotact.venuevista.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infotact.venuevista.model.*;
import com.infotact.venuevista.repo.*;
import com.infotact.venuevista.service.AnalyticsService;
import com.infotact.venuevista.service.BookingService;
import com.infotact.venuevista.service.CatalogService;
import jakarta.servlet.http.HttpSession;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/admin")
public class AdminController {
    @Data
    public static class EventForm {
        private Long id;
        private String title, performer, category, description, startTime, status = "DRAFT";
        private Long venueId;
        private int priceVip, pricePremium, priceStandard, hue1 = 20, hue2 = 280;
    }

    private final EventRepository events;
    private final VenueRepository venues;
    private final OrderRepository orders;
    private final TicketRepository tickets;
    private final AnalyticsService analytics;
    private final CatalogService catalog;
    private final BookingService booking;
    private final ObjectMapper mapper;
    private final String adminUser, adminPass;

    public AdminController(EventRepository events, VenueRepository venues, OrderRepository orders, TicketRepository tickets,
                           AnalyticsService analytics, CatalogService catalog, BookingService booking, ObjectMapper mapper,
                           @Value("${venuevista.admin.username}") String adminUser,
                           @Value("${venuevista.admin.password}") String adminPass) {
        this.events = events;
        this.venues = venues;
        this.orders = orders;
        this.tickets = tickets;
        this.analytics = analytics;
        this.catalog = catalog;
        this.booking = booking;
        this.mapper = mapper;
        this.adminUser = adminUser;
        this.adminPass = adminPass;
    }

    // ---- auth -------------------------------------------------------------------------------------
    @GetMapping("/login")
    public String loginPage() { return "admin/login"; }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, HttpSession session, RedirectAttributes ra) {
        if (adminUser.equals(username) && adminPass.equals(password)) {
            session.setAttribute(AuthInterceptor.SESSION_KEY, true);
            return "redirect:/admin";
        }
        ra.addFlashAttribute("error", "Wrong username or password.");
        return "redirect:/admin/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    // ---- dashboard --------------------------------------------------------------------------------
    @GetMapping
    public String dashboard(Model m) throws JsonProcessingException {
        String json = mapper.writeValueAsString(analytics.dashboard()).replace("<", "\\u003c");
        m.addAttribute("analyticsJson", json);
        return "admin/dashboard";
    }

    // ---- events -----------------------------------------------------------------------------------
    @GetMapping("/events")
    public String eventList(Model m) {
        m.addAttribute("cards", catalog.cards(events.findAllByOrderByStartTimeDesc()));
        return "admin/events";
    }

    @GetMapping("/events/new")
    public String newEvent(Model m) {
        EventForm f = new EventForm();
        f.setStartTime(LocalDateTime.now().plusDays(14).withHour(19).withMinute(0).withSecond(0).withNano(0).toString());
        m.addAttribute("form", f);
        m.addAttribute("venues", venues.findAll());
        m.addAttribute("statuses", EventStatus.values());
        return "admin/event-form";
    }

    @GetMapping("/events/{id}/edit")
    public String editEvent(@PathVariable Long id, Model m) {
        Event e = events.findById(id).orElse(null);
        if (e == null) return "redirect:/admin/events";
        EventForm f = new EventForm();
        f.setId(e.getId());
        f.setTitle(e.getTitle());
        f.setPerformer(e.getPerformer());
        f.setCategory(e.getCategory());
        f.setDescription(e.getDescription());
        f.setVenueId(e.getVenue().getId());
        f.setStartTime(e.getStartTime().withSecond(0).withNano(0).toString());
        f.setStatus(e.getStatus().name());
        f.setPriceVip(e.getPriceVip());
        f.setPricePremium(e.getPricePremium());
        f.setPriceStandard(e.getPriceStandard());
        f.setHue1(e.getHue1());
        f.setHue2(e.getHue2());
        m.addAttribute("form", f);
        m.addAttribute("venues", venues.findAll());
        m.addAttribute("statuses", EventStatus.values());
        m.addAttribute("locked", tickets.existsByEventId(id));
        return "admin/event-form";
    }

    @PostMapping("/events")
    @Transactional
    public String saveEvent(@ModelAttribute EventForm f, RedirectAttributes ra) {
        if (f.getTitle() == null || f.getTitle().isBlank() || f.getVenueId() == null || f.getStartTime() == null || f.getStartTime().isBlank()) {
            ra.addFlashAttribute("error", "Title, venue and start time are required.");
            return f.getId() == null ? "redirect:/admin/events/new" : "redirect:/admin/events/" + f.getId() + "/edit";
        }
        Event e = f.getId() == null ? new Event() : events.findById(f.getId()).orElseThrow();
        boolean hasSales = f.getId() != null && tickets.existsByEventId(f.getId());
        e.setTitle(f.getTitle().trim());
        e.setPerformer(f.getPerformer());
        e.setCategory(f.getCategory() == null || f.getCategory().isBlank() ? "Other" : f.getCategory().trim());
        e.setDescription(f.getDescription());
        if (!hasSales) e.setVenue(venues.findById(f.getVenueId()).orElseThrow());
        e.setStartTime(LocalDateTime.parse(f.getStartTime()));
        e.setStatus(EventStatus.valueOf(f.getStatus()));
        e.setPriceVip(Math.max(0, f.getPriceVip()));
        e.setPricePremium(Math.max(0, f.getPricePremium()));
        e.setPriceStandard(Math.max(0, f.getPriceStandard()));
        e.setHue1(f.getHue1());
        e.setHue2(f.getHue2());
        events.save(e);
        ra.addFlashAttribute("msg", "Event “" + e.getTitle() + "” saved.");
        return "redirect:/admin/events";
    }

    @PostMapping("/events/{id}/status")
    public String setStatus(@PathVariable Long id, @RequestParam EventStatus status, RedirectAttributes ra) {
        events.findById(id).ifPresent(e -> {
            e.setStatus(status);
            events.save(e);
            ra.addFlashAttribute("msg", "“" + e.getTitle() + "” is now " + status.name().toLowerCase() + ".");
        });
        return "redirect:/admin/events";
    }

    @PostMapping("/events/{id}/delete")
    public String deleteEvent(@PathVariable Long id, RedirectAttributes ra) {
        if (tickets.existsByEventId(id)) {
            ra.addFlashAttribute("error", "This event has sold tickets, so it can't be deleted. Cancel it instead.");
        } else {
            events.deleteById(id);
            ra.addFlashAttribute("msg", "Event deleted.");
        }
        return "redirect:/admin/events";
    }

    // ---- venues -----------------------------------------------------------------------------------
    @GetMapping("/venues")
    public String venueList(Model m) {
        m.addAttribute("venues", venues.findAll());
        return "admin/venues";
    }

    @GetMapping("/venues/new")
    public String newVenue() { return "admin/venue-form"; }

    @PostMapping("/venues")
    @Transactional
    public String saveVenue(@RequestParam String name, @RequestParam(required = false) String city,
                            @RequestParam(required = false) String address, @RequestParam(required = false) String description,
                            @RequestParam(required = false) List<String> secName, @RequestParam(required = false) List<String> secTier,
                            @RequestParam(required = false) List<Integer> secRows, @RequestParam(required = false) List<Integer> secSeats,
                            RedirectAttributes ra) {
        if (name.isBlank() || secName == null || secName.isEmpty()) {
            ra.addFlashAttribute("error", "A venue needs a name and at least one section.");
            return "redirect:/admin/venues/new";
        }
        Venue v = new Venue();
        v.setName(name.trim());
        v.setCity(city);
        v.setAddress(address);
        v.setDescription(description);
        for (int i = 0; i < secName.size(); i++) {
            if (secName.get(i).isBlank()) continue;
            int rows = Math.min(26, Math.max(1, secRows.get(i)));
            int seats = Math.min(40, Math.max(1, secSeats.get(i)));
            v.addSection(new Section(secName.get(i).trim(), Tier.valueOf(secTier.get(i)), rows, seats));
        }
        venues.save(v);
        ra.addFlashAttribute("msg", "Venue “" + v.getName() + "” created with " + v.getCapacity() + " seats.");
        return "redirect:/admin/venues";
    }

    @PostMapping("/venues/{id}/delete")
    public String deleteVenue(@PathVariable Long id, RedirectAttributes ra) {
        if (events.existsByVenueId(id)) {
            ra.addFlashAttribute("error", "This venue is used by events and can't be deleted.");
        } else {
            venues.deleteById(id);
            ra.addFlashAttribute("msg", "Venue deleted.");
        }
        return "redirect:/admin/venues";
    }

    // ---- orders & check-in ------------------------------------------------------------------------
    @GetMapping("/orders")
    public String orderList(@RequestParam(required = false) String q, Model m) {
        String needle = q == null ? "" : q.trim().toLowerCase();
        List<Order> list = orders.findAllByOrderByCreatedAtDesc().stream()
                .filter(o -> needle.isEmpty() || (o.getReference() + " " + o.getCustomerName() + " " + o.getEmail() + " "
                        + o.getEvent().getTitle()).toLowerCase().contains(needle))
                .limit(200).toList();
        m.addAttribute("orders", list);
        m.addAttribute("q", q == null ? "" : q);
        return "admin/orders";
    }

    @GetMapping("/checkin")
    public String checkinPage() { return "admin/checkin"; }

    @PostMapping("/api/checkin")
    @ResponseBody
    public Map<String, Object> checkin(@RequestBody Map<String, String> body) {
        return booking.checkIn(body.get("code"));
    }
}
