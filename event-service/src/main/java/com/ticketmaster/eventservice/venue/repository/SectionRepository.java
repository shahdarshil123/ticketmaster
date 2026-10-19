package com.ticketmaster.eventservice.venue.repository;

import com.ticketmaster.eventservice.venue.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findByVenueIdOrderByName(Long venueId);

    boolean existsByVenueIdAndName(Long venueId, String name);

    long countByVenueId(Long venueId);
}
