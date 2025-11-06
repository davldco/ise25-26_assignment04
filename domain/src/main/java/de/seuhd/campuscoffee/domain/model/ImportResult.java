package de.seuhd.campuscoffee.domain.model;

import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Simple result container for imports from external sources.
 */
public record ImportResult(
        @NonNull Pos pos,
        @Nullable List<String> missingFields
) {}

