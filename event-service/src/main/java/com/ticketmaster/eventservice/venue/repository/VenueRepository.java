package com.ticketmaster.eventservice.venue.repository;

import com.ticketmaster.eventservice.venue.entity.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueRepository extends JpaRepository<Venue, Long> {
}
