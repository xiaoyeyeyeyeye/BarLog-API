package com.alcohol.places;

import com.alcohol.config.GooglePlacesProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

class GooglePlacesClientTest {

    private WireMockServer wireMock;
    private GooglePlacesClient client;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(0);
        wireMock.start();

        GooglePlacesProperties properties = new GooglePlacesProperties();
        properties.setApiKey("test-key");
        properties.setMaxResultCount(5);
        properties.setBaseUrl("http://localhost:" + wireMock.port() + "/v1");

        RestTemplate restTemplate = new RestTemplateBuilder()
                .rootUri(properties.getBaseUrl())
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(2))
                .build();
        client = new GooglePlacesClient(restTemplate, properties);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void searchTextReturnsPlaces() {
        wireMock.stubFor(post(urlEqualTo("/v1/places:searchText"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "places": [{
                                    "id": "places/ChIJTestBar",
                                    "displayName": { "text": "Atlas Bar" },
                                    "formattedAddress": "Parkview Square, Singapore",
                                    "location": { "latitude": 1.3001, "longitude": 103.8590 },
                                    "rating": 4.6,
                                    "userRatingCount": 1200,
                                    "primaryType": "bar",
                                    "types": ["bar", "night_club"]
                                  }]
                                }
                                """)));

        var places = client.searchText("bars in Singapore");
        assertEquals(1, places.size());
        assertEquals("ChIJTestBar", places.get(0).placeId());
        assertEquals("Atlas Bar", places.get(0).displayName());
        assertEquals(4.6, places.get(0).rating());
    }

    @Test
    void getPlaceDetailsReturnsPlace() {
        wireMock.stubFor(get(urlEqualTo("/v1/places/ChIJDetailBar"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id": "places/ChIJDetailBar",
                                  "displayName": { "text": "Native Bar" },
                                  "formattedAddress": "32 Seah St, Singapore",
                                  "location": { "latitude": 1.298, "longitude": 103.855 },
                                  "rating": 4.4,
                                  "regularOpeningHours": {
                                    "weekdayDescriptions": ["Monday: 5:00 PM – 1:00 AM"]
                                  }
                                }
                                """)));

        GooglePlace place = client.getPlaceDetails("ChIJDetailBar");
        assertNotNull(place);
        assertEquals("Native Bar", place.displayName());
        assertEquals("Monday: 5:00 PM – 1:00 AM", place.openingHoursSummary());
    }
}
