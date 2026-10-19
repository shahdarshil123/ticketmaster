package com.ticketmaster.eventservice.venue.service;

import com.ticketmaster.eventservice.common.exception.ConflictException;
import com.ticketmaster.eventservice.common.exception.NotFoundException;
import com.ticketmaster.eventservice.venue.dto.AddSeatsRequest;
import com.ticketmaster.eventservice.venue.dto.AddSeatsResponse;
import com.ticketmaster.eventservice.venue.dto.CreateSectionRequest;
import com.ticketmaster.eventservice.venue.dto.CreateVenueRequest;
import com.ticketmaster.eventservice.venue.dto.SectionResponse;
import com.ticketmaster.eventservice.venue.dto.VenueLayoutResponse;
import com.ticketmaster.eventservice.venue.dto.VenueResponse;
import com.ticketmaster.eventservice.venue.entity.Seat;
import com.ticketmaster.eventservice.venue.entity.Section;
import com.ticketmaster.eventservice.venue.entity.Venue;
import com.ticketmaster.eventservice.venue.repository.SeatRepository;
import com.ticketmaster.eventservice.venue.repository.SectionRepository;
import com.ticketmaster.eventservice.venue.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VenueService {

    private final VenueRepository venueRepository;
    private final SectionRepository sectionRepository;
    private final SeatRepository seatRepository;

    @Transactional
    public VenueResponse createVenue(CreateVenueRequest request) {
        Venue venue = new Venue();
        venue.setName(request.name());
        venue.setAddress(request.address());
        venue.setCity(request.city());
        venue.setLatitude(request.latitude());
        venue.setLongitude(request.longitude());
        return toVenueResponse(venueRepository.save(venue), List.of());
    }

    public VenueResponse getVenue(Long venueId) {
        Venue venue = findVenue(venueId);
        return toVenueResponse(venue, sectionRepository.findByVenueIdOrderByName(venueId));
    }

    @Transactional
    public SectionResponse createSection(Long venueId, CreateSectionRequest request) {
        Venue venue = findVenue(venueId);
        if (sectionRepository.existsByVenueIdAndName(venueId, request.name())) {
            throw new ConflictException("Section '" + request.name() + "' already exists in venue " + venueId);
        }

        Section section = new Section();
        section.setVenue(venue);
        section.setName(request.name());
        Section saved = sectionRepository.save(section);
        return new SectionResponse(saved.getId(), venueId, saved.getName());
    }

    @Transactional
    public AddSeatsResponse addSeats(Long sectionId, AddSeatsRequest request) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new NotFoundException("Section " + sectionId + " not found"));

        List<String> rowLabels = request.rows().stream().map(AddSeatsRequest.Row::rowLabel).toList();
        if (seatRepository.existsBySectionIdAndRowLabelIn(sectionId, rowLabels)) {
            throw new ConflictException("One or more rows already have seats in section " + sectionId);
        }

        List<Seat> seats = new ArrayList<>();
        for (AddSeatsRequest.Row row : request.rows()) {
            for (int number = 1; number <= row.seatsPerRow(); number++) {
                Seat seat = new Seat();
                seat.setSection(section);
                seat.setRowLabel(row.rowLabel());
                seat.setSeatNumber(number);
                seats.add(seat);
            }
        }
        seatRepository.saveAll(seats);
        return new AddSeatsResponse(sectionId, seats.size());
    }

    public VenueLayoutResponse getLayout(Long venueId) {
        if (!venueRepository.existsById(venueId)) {
            throw new NotFoundException("Venue " + venueId + " not found");
        }

        List<Section> sections = sectionRepository.findByVenueIdOrderByName(venueId);
        Map<Long, List<VenueLayoutResponse.SeatSummary>> seatsBySection = seatRepository
                .findBySectionVenueIdOrderByRowLabelAscSeatNumberAsc(venueId)
                .stream()
                .collect(Collectors.groupingBy(
                        seat -> seat.getSection().getId(),
                        Collectors.mapping(
                                seat -> new VenueLayoutResponse.SeatSummary(seat.getId(), seat.getRowLabel(), seat.getSeatNumber()),
                                Collectors.toList())));

        List<VenueLayoutResponse.SectionLayout> layout = sections.stream()
                .map(section -> new VenueLayoutResponse.SectionLayout(
                        section.getId(),
                        section.getName(),
                        seatsBySection.getOrDefault(section.getId(), List.of())))
                .toList();
        return new VenueLayoutResponse(venueId, layout);
    }

    private Venue findVenue(Long venueId) {
        return venueRepository.findById(venueId)
                .orElseThrow(() -> new NotFoundException("Venue " + venueId + " not found"));
    }

    private VenueResponse toVenueResponse(Venue venue, List<Section> sections) {
        List<VenueResponse.SectionSummary> summaries = sections.stream()
                .map(section -> new VenueResponse.SectionSummary(section.getId(), section.getName()))
                .toList();
        return new VenueResponse(
                venue.getId(),
                venue.getName(),
                venue.getAddress(),
                venue.getCity(),
                venue.getLatitude(),
                venue.getLongitude(),
                summaries);
    }
}
