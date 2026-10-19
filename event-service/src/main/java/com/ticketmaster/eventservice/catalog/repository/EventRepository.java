package com.ticketmaster.eventservice.catalog.repository;

import com.ticketmaster.eventservice.catalog.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
}
