package de.seuhd.campuscoffee.api.dtos;

import de.seuhd.campuscoffee.domain.model.CampusType;
import de.seuhd.campuscoffee.domain.model.PosType;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO record for POS metadata.
 */
@Builder(toBuilder = true)
public record PosDto(
        @Nullable Long id, // id is null when creating a new task
        @Nullable LocalDateTime createdAt, // is null when using DTO to create a new POS
        @Nullable LocalDateTime updatedAt, // is set when creating or updating a POS
        @NonNull String name,
        @NonNull String description,
        @NonNull PosType type,
        @NonNull CampusType campus,
        @Nullable String street,
        @Nullable String houseNumber,
        @Nullable Integer postalCode,
        @Nullable String city,
        @Nullable List<String> missingFields // optional list of missing fields when imported partially
) {}
