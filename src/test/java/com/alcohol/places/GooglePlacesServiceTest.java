package com.alcohol.places;

import com.alcohol.compat.vo.FrontendBarVO;
import com.alcohol.config.GooglePlacesProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class GooglePlacesServiceTest {

    @Mock
    private GooglePlacesClient client;

    @Mock
    private PlacesUsageLimiter usageLimiter;

    private GooglePlacesProperties properties;
    private GooglePlacesService service;

    @BeforeEach
    void setUp() {
        properties = new GooglePlacesProperties();
        properties.setEnabled(true);
        properties.setApiKey("test-key");
        properties.setDefaultCity("Singapore");
        properties.setSearchRadiusM(3000);
        properties.setCacheTtlSeconds(0);
        properties.setDetailCacheTtlSeconds(0);
        lenient().when(usageLimiter.tryAcquire(anyString())).thenReturn(true);
        service = new GooglePlacesService(properties, client, new PlacesToBarMapper(), usageLimiter);
    }

    @Test
    void searchNearbyUsesClientWhenLatLngPresent() {
        when(client.searchNearby(1.29, 103.85, 3000)).thenReturn(List.of(samplePlace()));

        List<FrontendBarVO> bars = service.searchNearby(1.29, 103.85, "Singapore");

        assertEquals(1, bars.size());
        assertEquals("gp_ChIJTestBar", bars.get(0).getId());
        verify(client).searchNearby(1.29, 103.85, 3000);
        verify(client, never()).searchText(anyString());
    }

    @Test
    void searchNearbyUsesTextWhenNoLatLng() {
        when(client.searchText("bars in Singapore")).thenReturn(List.of(samplePlace()));

        List<FrontendBarVO> bars = service.searchNearby(null, null, "Singapore");

        assertEquals(1, bars.size());
        verify(client).searchText("bars in Singapore");
    }

    @Test
    void returnsEmptyWhenNotConfigured() {
        properties.setEnabled(false);
        assertTrue(service.searchNearby(1.29, 103.85, "Singapore").isEmpty());
    }

    private GooglePlace samplePlace() {
        try {
            var node = new com.fasterxml.jackson.databind.ObjectMapper().readTree("""
                    {
                      "id": "places/ChIJTestBar",
                      "displayName": { "text": "Atlas Bar" },
                      "formattedAddress": "Singapore",
                      "location": { "latitude": 1.3001, "longitude": 103.8590 },
                      "rating": 4.6,
                      "types": ["bar"]
                    }
                    """);
            return new GooglePlace(node);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
