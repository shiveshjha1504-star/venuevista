package com.infotact.venuevista.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "CustomerOrder")
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String reference;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private Event event;

    @Column(nullable = false)
    private String customerName;
    @Column(nullable = false)
    private String email;

    private int total;
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @OrderBy("id ASC")
    private List<Ticket> tickets = new ArrayList<>();

    public int getTicketCount() { return tickets.size(); }

    public void addTicket(Ticket t) {
        t.setOrder(this);
        tickets.add(t);
    }
}
