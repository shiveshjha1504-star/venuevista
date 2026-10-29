package com.infotact.venuevista.repo;

import com.infotact.venuevista.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByCode(String code);
    boolean existsByEventId(Long eventId);

    @Query("select t.section.id, t.rowNo, t.seatNo from Ticket t where t.event.id = :eid")
    List<Object[]> takenSeats(@Param("eid") Long eventId);

    @Query("select t.event.id, count(t) from Ticket t group by t.event.id")
    List<Object[]> soldCounts();
}
