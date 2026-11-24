package com.carinsurance.location;

import com.carinsurance.location.dto.LocalityResponse;
import com.carinsurance.location.dto.ZoneResponse;
import org.springframework.stereotype.Component;

@Component
public class LocalityMapper {

    public LocalityResponse toResponse(Locality locality) {
        return new LocalityResponse(
                locality.getId(),
                locality.getName(),
                locality.getProvince(),
                locality.getPostalCode(),
                toResponse(locality.getZone()));
    }

    public ZoneResponse toResponse(Zone zone) {
        return new ZoneResponse(zone.getId(), zone.getName());
    }
}
