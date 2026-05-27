package com.alcohol.places;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlacesToBarMapperTest {

    private final PlacesToBarMapper mapper = new PlacesToBarMapper();

    @Test
    void mapsGooglePlaceToFrontendBar() throws Exception {
        var node = new ObjectMapper().readTree("""
                {
                  "id": "places/ChIJTest123",
                  "displayName": { "text": "Long Bar" },
                  "formattedAddress": "Raffles Hotel, Singapore",
                  "location": { "latitude": 1.295, "longitude": 103.854 },
                  "rating": 4.3,
                  "primaryType": "bar",
                  "types": ["bar"]
                }
                """);
        GooglePlace place = new GooglePlace(node);
        var bar = mapper.toBar(place, 1.29, 103.85, "Singapore");

        assertEquals("gp_ChIJTest123", bar.getId());
        assertEquals("Long Bar", bar.getName());
        assertEquals(4.3, bar.getRating());
        assertNotNull(bar.getDistanceMeters());
        assertTrue(bar.getTags().contains("bar"));
    }

    @Test
    void extractsPlaceIdFromBarId() {
        assertEquals("ChIJTest123", mapper.extractPlaceId("gp_ChIJTest123"));
        assertNull(mapper.extractPlaceId("bar_001"));
        assertTrue(mapper.isGoogleBarId("gp_abc"));
    }
}
