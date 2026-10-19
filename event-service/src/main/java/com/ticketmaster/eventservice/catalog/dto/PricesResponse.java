package com.ticketmaster.eventservice.catalog.dto;

import java.math.BigDecimal;
import java.util.List;

public record PricesResponse(Long eventId, List<SectionPrice> prices) {

    public record SectionPrice(Long sectionId, String sectionName, BigDecimal price) {
    }
}
