package com.ticketmaster.eventservice.venue.repository;

import com.ticketmaster.eventservice.venue.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    /**
     * All seats of a venue in one query, for the layout endpoint.
     */
    List<Seat> findBySectionVenueIdOrderByRowLabelAscSeatNumberAsc(Long venueId);

    /**
     * True if any of the given rows already has seats in this section.
     */
    boolean existsBySectionIdAndRowLabelIn(Long sectionId, Collection<String> rowLabels);
}
