package com.infotact.venuevista.web;

import com.infotact.venuevista.model.Event;
import com.infotact.venuevista.model.Order;
import com.infotact.venuevista.repo.EventRepository;
import com.infotact.venuevista.service.BookingException;
import com.infotact.venuevista.service.BookingService;
import com.infotact.venuevista.service.BookingService.SeatRef;
import com.infotact.venuevista.service.CatalogService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {
    public record CheckoutRequest(String name, String email, List<SeatRef> seats) {}

    private final EventRepository events;
    private final CatalogService catalog;
    private final BookingService booking;

    public ApiController(EventRepository events, CatalogService catalog, BookingService booking) {
        this.events = events;
        this.catalog = catalog;
        this.booking = booking;
    }

    @GetMapping("/events/{id}/seatmap")
    public ResponseEntity<Map<String, Object>> seatMap(@PathVariable Long id) {
        Event e = events.findById(id).orElse(null);
        if (e == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(catalog.seatMap(e));
    }

    @PostMapping("/events/{id}/checkout")
    public ResponseEntity<Map<String, Object>> checkout(@PathVariable Long id, @RequestBody CheckoutRequest req) {
        try {
            Order o = booking.book(id, req.name(), req.email(), req.seats());
            return ResponseEntity.ok(Map.of("reference", o.getReference(), "total", o.getTotal()));
        } catch (BookingException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Someone grabbed one of those seats a moment ago. Please pick again."));
        }
    }
}
