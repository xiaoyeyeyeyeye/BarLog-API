package com.alcohol.places;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Google Places API (New) 原始 place 节点 + 解析辅助。
 */
@Getter
@RequiredArgsConstructor
public class GooglePlace {

    private final JsonNode node;

    public String resourceId() {
        return text(node, "id");
    }

    public String placeId() {
        String id = resourceId();
        if (id != null && id.startsWith("places/")) {
            return id.substring("places/".length());
        }
        return id;
    }

    public String displayName() {
        JsonNode name = node.get("displayName");
        return name != null ? text(name, "text") : null;
    }

    public String formattedAddress() {
        return text(node, "formattedAddress");
    }

    public Double latitude() {
        JsonNode loc = node.get("location");
        return loc != null && loc.has("latitude") ? loc.get("latitude").asDouble() : null;
    }

    public Double longitude() {
        JsonNode loc = node.get("location");
        return loc != null && loc.has("longitude") ? loc.get("longitude").asDouble() : null;
    }

    public Double rating() {
        return node.has("rating") ? node.get("rating").asDouble() : null;
    }

    public Integer userRatingCount() {
        return node.has("userRatingCount") ? node.get("userRatingCount").asInt() : null;
    }

    public String primaryType() {
        return text(node, "primaryType");
    }

    public List<String> types() {
        JsonNode types = node.get("types");
        if (types == null || !types.isArray()) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>();
        types.forEach(t -> list.add(t.asText()));
        return list;
    }

    public String locality() {
        return componentText("locality", "postal_town", "administrative_area_level_1");
    }

    public String sublocality() {
        return componentText("sublocality", "sublocality_level_1", "neighborhood");
    }

    public String openingHoursSummary() {
        JsonNode hours = node.get("regularOpeningHours");
        if (hours == null) {
            return null;
        }
        JsonNode descriptions = hours.get("weekdayDescriptions");
        if (descriptions != null && descriptions.isArray() && descriptions.size() > 0) {
            return descriptions.get(0).asText();
        }
        return null;
    }

    public String fullOpeningHours() {
        JsonNode hours = node.get("regularOpeningHours");
        if (hours == null) {
            return null;
        }
        JsonNode descriptions = hours.get("weekdayDescriptions");
        if (descriptions == null || !descriptions.isArray()) {
            return openingHoursSummary();
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode line : descriptions) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(line.asText());
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    public String websiteUri() {
        return text(node, "websiteUri");
    }

    public String googleMapsUri() {
        return text(node, "googleMapsUri");
    }

    public String shortDescription() {
        JsonNode summary = node.get("editorialSummary");
        if (summary != null && summary.has("text")) {
            return summary.get("text").asText();
        }
        if (hasText(primaryType())) {
            return primaryType().replace('_', ' ');
        }
        return null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String componentText(String... types) {
        JsonNode components = node.get("addressComponents");
        if (components == null || !components.isArray()) {
            return null;
        }
        for (String type : types) {
            for (JsonNode component : components) {
                JsonNode typeNodes = component.get("types");
                if (typeNodes == null || !typeNodes.isArray()) {
                    continue;
                }
                for (JsonNode t : typeNodes) {
                    if (type.equals(t.asText())) {
                        String longText = text(component, "longText");
                        if (longText != null) {
                            return longText;
                        }
                        return text(component, "shortText");
                    }
                }
            }
        }
        return null;
    }

    private static String text(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asText();
    }
}
