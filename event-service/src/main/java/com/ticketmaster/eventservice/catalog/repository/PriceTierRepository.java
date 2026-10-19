package com.ticketmaster.eventservice.catalog.repository;

import com.ticketmaster.eventservice.catalog.entity.PriceTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceTierRepository extends JpaRepository<PriceTier, Long> {

    List<PriceTier> findByEventId(Long eventId);

    /**
     * Used when replacing all prices of an event; call inside a transaction.
     */
    void deleteByEventId(Long eventId);

    long countByEventId(Long eventId);
}
