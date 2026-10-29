package com.infotact.venuevista.repo;

import com.infotact.venuevista.model.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueRepository extends JpaRepository<Venue, Long> {}
