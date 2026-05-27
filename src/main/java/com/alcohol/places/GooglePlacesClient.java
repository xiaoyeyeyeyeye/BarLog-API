package com.alcohol.places;

import com.alcohol.config.GooglePlacesProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Google Places API (New) HTTP 客户端（Nearby / Text / Details）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GooglePlacesClient {

    static final String SEARCH_FIELD_MASK = String.join(",",
            "places.id",
            "places.displayName",
            "places.formattedAddress",
            "places.location",
            "places.rating",
            "places.userRatingCount",
            "places.types",
            "places.primaryType",
            "places.addressComponents");

    static final String DETAIL_FIELD_MASK = String.join(",",
            "id",
            "displayName",
            "formattedAddress",
            "location",
            "rating",
            "userRatingCount",
            "types",
            "primaryType",
            "addressComponents",
            "regularOpeningHours",
            "websiteUri",
            "googleMapsUri");

    private final RestTemplate googlePlacesRestTemplate;
    private final GooglePlacesProperties properties;

    public List<GooglePlace> searchNearby(double lat, double lng, int radiusM) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("includedTypes", List.of("bar", "night_club"));
        body.put("maxResultCount", properties.getMaxResultCount());
        body.put("locationRestriction", Map.of(
                "circle", Map.of(
                        "center", Map.of("latitude", lat, "longitude", lng),
                        "radius", (double) radiusM)));

        JsonNode response = post("/places:searchNearby", SEARCH_FIELD_MASK, body);
        return parsePlacesArray(response);
    }

    public List<GooglePlace> searchText(String textQuery) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("textQuery", textQuery);
        body.put("maxResultCount", properties.getMaxResultCount());

        JsonNode response = post("/places:searchText", SEARCH_FIELD_MASK, body);
        return parsePlacesArray(response);
    }

    public GooglePlace getPlaceDetails(String placeId) {
        String resourceName = placeId.startsWith("places/") ? placeId : "places/" + placeId;
        try {
            JsonNode node = googlePlacesRestTemplate.exchange(
                    "/" + resourceName,
                    HttpMethod.GET,
                    new HttpEntity<>(headers(DETAIL_FIELD_MASK)),
                    JsonNode.class).getBody();
            return node != null ? new GooglePlace(node) : null;
        } catch (RestClientException e) {
            log.warn("Google Place Details failed for {}: {}", placeId, e.getMessage());
            return null;
        }
    }

    private JsonNode post(String path, String fieldMask, Map<String, Object> body) {
        try {
            return googlePlacesRestTemplate.exchange(
                    path,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers(fieldMask)),
                    JsonNode.class).getBody();
        } catch (RestClientException e) {
            log.warn("Google Places request failed {}: {}", path, e.getMessage());
            return null;
        }
    }

    private HttpHeaders headers(String fieldMask) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Goog-Api-Key", properties.getApiKey());
        headers.set("X-Goog-FieldMask", fieldMask);
        return headers;
    }

    private List<GooglePlace> parsePlacesArray(JsonNode response) {
        List<GooglePlace> places = new ArrayList<>();
        if (response == null) {
            return places;
        }
        JsonNode array = response.get("places");
        if (array == null || !array.isArray()) {
            return places;
        }
        array.forEach(node -> places.add(new GooglePlace(node)));
        return places;
    }
}
