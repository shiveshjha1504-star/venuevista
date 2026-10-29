package com.infotact.venuevista.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter @NoArgsConstructor
public class Section {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Venue venue;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tier tier = Tier.STANDARD;

    private int rowsCount;
    private int seatsPerRow;
    private int sortOrder;

    public Section(String name, Tier tier, int rowsCount, int seatsPerRow) {
        this.name = name;
        this.tier = tier;
        this.rowsCount = rowsCount;
        this.seatsPerRow = seatsPerRow;
    }

    public int getCapacity() { return rowsCount * seatsPerRow; }
}
