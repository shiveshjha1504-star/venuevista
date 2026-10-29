package com.infotact.venuevista.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** One sold seat for one event. The unique constraint makes double-booking impossible at DB level. */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "section_id", "row_no", "seat_no"}))
@Getter @Setter @NoArgsConstructor
public class Ticket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id")
    private Section section;

    @Column(name = "row_no")
    private int rowNo;
    @Column(name = "seat_no")
    private int seatNo;

    private int price;
    private boolean checkedIn;
    private LocalDateTime checkedInAt;

    public String getRowLabel() { return Seats.rowLabel(rowNo); }
}
