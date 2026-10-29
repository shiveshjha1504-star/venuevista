package com.infotact.venuevista.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter @NoArgsConstructor
public class Venue {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;
    private String city;
    private String address;
    @Column(length = 1000)
    private String description;

    @OneToMany(mappedBy = "venue", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<Section> sections = new ArrayList<>();

    public int getCapacity() {
        return sections.stream().mapToInt(Section::getCapacity).sum();
    }

    public void addSection(Section s) {
        s.setVenue(this);
        s.setSortOrder(sections.size());
        sections.add(s);
    }
}
