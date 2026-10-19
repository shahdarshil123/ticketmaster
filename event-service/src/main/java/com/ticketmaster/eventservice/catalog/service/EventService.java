package com.ticketmaster.eventservice.catalog.service;

import com.ticketmaster.eventservice.catalog.dto.CreateEventRequest;
import com.ticketmaster.eventservice.catalog.dto.EventResponse;
import com.ticketmaster.eventservice.catalog.dto.PricesResponse;
import com.ticketmaster.eventservice.catalog.dto.SetPricesRequest;
import com.ticketmaster.eventservice.catalog.dto.UpdateEventRequest;
import com.ticketmaster.eventservice.catalog.entity.Event;
import com.ticketmaster.eventservice.catalog.entity.EventStatus;
import com.ticketmaster.eventservice.catalog.entity.PriceTier;
import com.ticketmaster.eventservice.catalog.repository.EventRepository;
import com.ticketmaster.eventservice.catalog.repository.PriceTierRepository;
import com.ticketmaster.eventservice.common.exception.ConflictException;
import com.ticketmaster.eventservice.common.exception.NotFoundException;
import com.ticketmaster.eventservice.venue.entity.Section;
import com.ticketmaster.eventservice.venue.entity.Venue;
import com.ticketmaster.eventservice.venue.repository.SectionRepository;
import com.ticketmaster.eventservice.venue.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final PriceTierRepository priceTierRepository;
    private final VenueRepository venueRepository;
    private final SectionRepository sectionRepository;

    @Transactional
    public EventResponse createEvent(CreateEventRequest request) {
        Venue venue = venueRepository.findById(request.venueId())
                .orElseThrow(() -> new NotFoundException("Venue " + request.venueId() + " not found"));

        Event event = new Event();
        event.setVenue(venue);
        event.setCategory(request.category());
        event.setName(request.name());
        event.setDescription(request.description());
        event.setStartTime(request.startTime());
        event.setStatus(EventStatus.DRAFT);
        return toEventResponse(eventRepository.save(event));
    }

    public EventResponse getEvent(Long eventId) {
        return toEventResponse(findEvent(eventId));
    }

    @Transactional
    public EventResponse updateEvent(Long eventId, UpdateEventRequest request) {
        Event event = findEvent(eventId);
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new ConflictException("Event " + eventId + " is cancelled");
        }

        event.setCategory(request.category());
        event.setName(request.name());
        event.setDescription(request.description());
        event.setStartTime(request.startTime());
        return toEventResponse(event);
    }

    @Transactional
    public EventResponse publishEvent(Long eventId) {
        Event event = findEvent(eventId);
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new ConflictException("Event " + eventId + " is " + event.getStatus() + ", only DRAFT events can be published");
        }

        long sectionCount = sectionRepository.countByVenueId(event.getVenue().getId());
        long pricedCount = priceTierRepository.countByEventId(eventId);
        if (sectionCount == 0 || pricedCount < sectionCount) {
            throw new ConflictException("Every section of the venue needs a price before event " + eventId + " can be published");
        }

        // v1: status change only; calls to Booking and Search services come later
        event.setStatus(EventStatus.PUBLISHED);
        return toEventResponse(event);
    }

    @Transactional
    public EventResponse cancelEvent(Long eventId) {
        Event event = findEvent(eventId);
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new ConflictException("Event " + eventId + " is already cancelled");
        }

        event.setStatus(EventStatus.CANCELLED);
        return toEventResponse(event);
    }

    @Transactional
    public PricesResponse setPrices(Long eventId, SetPricesRequest request) {
        Event event = findEvent(eventId);
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new ConflictException("Prices can only be changed while event " + eventId + " is DRAFT");
        }

        Long venueId = event.getVenue().getId();
        List<PriceTier> tiers = request.prices().stream()
                .map(entry -> {
                    Section section = sectionRepository.findById(entry.sectionId())
                            .filter(s -> s.getVenue().getId().equals(venueId))
                            .orElseThrow(() -> new NotFoundException(
                                    "Section " + entry.sectionId() + " not found in venue " + venueId));
                    PriceTier tier = new PriceTier();
                    tier.setEvent(event);
                    tier.setSection(section);
                    tier.setPrice(entry.price());
                    return tier;
                })
                .toList();

        // Flush the deletes first, otherwise Hibernate inserts before deleting and hits the (event, section) unique key
        priceTierRepository.deleteByEventId(eventId);
        priceTierRepository.flush();
        priceTierRepository.saveAll(tiers);
        return toPricesResponse(eventId, tiers);
    }

    public PricesResponse getPrices(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException("Event " + eventId + " not found");
        }
        return toPricesResponse(eventId, priceTierRepository.findByEventId(eventId));
    }

    private Event findEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event " + eventId + " not found"));
    }

    private EventResponse toEventResponse(Event event) {
        Venue venue = event.getVenue();
        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getCategory(),
                event.getStartTime(),
                event.getStatus(),
                new EventResponse.VenueSummary(venue.getId(), venue.getName(), venue.getCity()));
    }

    private PricesResponse toPricesResponse(Long eventId, List<PriceTier> tiers) {
        List<PricesResponse.SectionPrice> prices = tiers.stream()
                .map(tier -> new PricesResponse.SectionPrice(
                        tier.getSection().getId(),
                        tier.getSection().getName(),
                        tier.getPrice()))
                .toList();
        return new PricesResponse(eventId, prices);
    }
}
