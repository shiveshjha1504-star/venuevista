package com.infotact.venuevista.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.stream.IntStream;

@Entity
@Getter @Setter @NoArgsConstructor
public class Event {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;
    private String performer;
    private String category;
    @Column(length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private Venue venue;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Enumerated(EnumType.STRING)
    private EventStatus status = EventStatus.DRAFT;

    private int priceVip;
    private int pricePremium;
    private int priceStandard;

    /** Two hues (0-360) that drive the generative poster artwork. */
    private int hue1 = 20;
    private int hue2 = 280;

    public int priceFor(Tier t) {
        return switch (t) {
            case VIP -> priceVip;
            case PREMIUM -> pricePremium;
            case STANDARD -> priceStandard;
        };
    }

    public int getMinPrice() {
        return venue.getSections().stream().map(Section::getTier).distinct()
                .flatMapToInt(t -> IntStream.of(priceFor(t))).filter(p -> p > 0).min().orElse(0);
    }

    public boolean isUpcoming() { return startTime.isAfter(LocalDateTime.now()); }

    public boolean isOnSale() { return status == EventStatus.PUBLISHED && isUpcoming(); }
}
