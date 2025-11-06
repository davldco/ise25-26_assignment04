package de.seuhd.campuscoffee.domain.model;

import lombok.Builder;
import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.Optional;

/**
 * Represents an OpenStreetMap node with relevant Point of Sale information.
 * This is the domain model for OSM data before it is converted to a POS object.
 */
@Builder
public record OsmNode(@NonNull Long nodeId,
                      @NonNull Double lat,
                      @NonNull Double lon,
                      String name,
                      Map<String, String> tags) {

    public Optional<String> tag(String key) {
        if (tags == null) return Optional.empty();
        return Optional.ofNullable(tags.get(key));
    }
}
