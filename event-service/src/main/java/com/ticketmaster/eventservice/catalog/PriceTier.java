package com.ticketmaster.eventservice.catalog;

import com.ticketmaster.eventservice.common.BaseEntity;
import com.ticketmaster.eventservice.venue.Section;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Price of one section for one event.
 */
@Entity
@Table(name = "price_tiers",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "section_id"}))
@Getter
@Setter
public class PriceTier extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
}
