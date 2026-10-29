package com.infotact.venuevista.repo;

import com.infotact.venuevista.model.Event;
import com.infotact.venuevista.model.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByStatusOrderByStartTimeAsc(EventStatus status);
    List<Event> findAllByOrderByStartTimeDesc();
    boolean existsByVenueId(Long venueId);
}
