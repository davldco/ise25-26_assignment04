package de.seuhd.campuscoffee.service;

import de.seuhd.campuscoffee.domain.exceptions.DuplicatePosNameException;
import de.seuhd.campuscoffee.domain.exceptions.OsmNodeMissingFieldsException;
import de.seuhd.campuscoffee.domain.exceptions.OsmNodeNotFoundException;
import de.seuhd.campuscoffee.domain.model.CampusType;
import de.seuhd.campuscoffee.domain.model.ImportResult;
import de.seuhd.campuscoffee.domain.model.OsmNode;
import de.seuhd.campuscoffee.domain.model.Pos;
import de.seuhd.campuscoffee.domain.exceptions.PosNotFoundException;
import de.seuhd.campuscoffee.domain.model.PosType;
import de.seuhd.campuscoffee.domain.ports.OsmDataService;
import de.seuhd.campuscoffee.domain.ports.PosDataService;
import de.seuhd.campuscoffee.domain.ports.PosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Application-level implementation of PosService. This class provides the Spring bean
 * and delegates data access to the domain ports (PosDataService, OsmDataService).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PosServiceImpl implements PosService {
    private final PosDataService posDataService;
    private final OsmDataService osmDataService;

    @Override
    public void clear() {
        log.warn("Clearing all POS data");
        posDataService.clear();
    }

    @Override
    public @NonNull List<Pos> getAll() {
        log.debug("Retrieving all POS");
        return posDataService.getAll();
    }

    @Override
    public @NonNull Pos getById(@NonNull Long id) throws PosNotFoundException {
        log.debug("Retrieving POS with ID: {}", id);
        return posDataService.getById(id);
    }

    @Override
    public @NonNull Pos upsert(@NonNull Pos pos) throws PosNotFoundException {
        if (pos.id() == null) {
            // Create new POS
            log.info("Creating new POS: {}", pos.name());
            return performUpsert(pos);
        } else {
            // Update existing POS
            log.info("Updating POS with ID: {}", pos.id());
            // POS ID must be set
            Objects.requireNonNull(pos.id());
            // POS must exist in the database before the update
            posDataService.getById(pos.id());
            return performUpsert(pos);
        }
    }

    @Override
    public @NonNull ImportResult importFromOsmNode(@NonNull Long nodeId) throws OsmNodeNotFoundException {
        return importFromOsmNode(nodeId, false);
    }

    @Override
    public @NonNull ImportResult importFromOsmNode(@NonNull Long nodeId, boolean allowPartial) throws OsmNodeNotFoundException {
        log.info("Importing POS from OpenStreetMap node {} (allowPartial={})...", nodeId, allowPartial);

        // Fetch the OSM node data using the port
        OsmNode osmNode = osmDataService.fetchNode(nodeId);

        // Convert OSM node to POS domain object and upsert it
        var result = convertOsmNodeToPos(osmNode, allowPartial);
        Pos posCandidate = result.pos();
        List<String> missing = result.missingFields();

        Pos savedPos = upsert(posCandidate);
        log.info("Successfully imported POS '{}' from OSM node {}", savedPos.name(), nodeId);

        return new ImportResult(savedPos, missing);
    }

    /**
     * Converts an OSM node to a POS domain object.
     * Returns an ImportResult with the candidate Pos and list of missing fields (nullable/empty list).
     */
    private @NonNull ImportResult convertOsmNodeToPos(@NonNull OsmNode osmNode, boolean allowPartial) {
        // Extract required fields from OSM tags
        var name = osmNode.tag("name");
        var street = osmNode.tag("addr:street");
        var housenumber = osmNode.tag("addr:housenumber");
        var postcode = osmNode.tag("addr:postcode");
        var city = osmNode.tag("addr:city");

        List<String> missing = new ArrayList<>();
        Optional<String> nameVal = name.isEmpty() ? Optional.empty() : Optional.of(name.get());
        Optional<String> streetVal = street.isEmpty() ? Optional.empty() : Optional.of(street.get());
        Optional<String> housenumberVal = housenumber.isEmpty() ? Optional.empty() : Optional.of(housenumber.get());
        Optional<String> postcodeVal = postcode.isEmpty() ? Optional.empty() : Optional.of(postcode.get());
        Optional<String> cityVal = city.isEmpty() ? Optional.empty() : Optional.of(city.get());

        if (nameVal.isEmpty()) missing.add("name");
        if (streetVal.isEmpty()) missing.add("addr:street");
        if (housenumberVal.isEmpty()) missing.add("addr:housenumber");
        if (postcodeVal.isEmpty()) missing.add("addr:postcode");
        if (cityVal.isEmpty()) missing.add("addr:city");

        // Parse postcode to integer if possible
        Integer postalCodeInt = null;
        if (postcodeVal.isPresent()) {
            try {
                postalCodeInt = Integer.valueOf(postcodeVal.get());
            } catch (Exception e) {
                missing.add("addr:postcode (unparseable)");
                postalCodeInt = null;
            }
        }

        if (!missing.isEmpty() && !allowPartial) {
            // If any required field is missing and partial imports are not allowed, throw exception
            throw new OsmNodeMissingFieldsException(osmNode.nodeId());
        }

        // Map to domain Pos. Some fields (type, campus, description) are not available from OSM tags reliably.
        // Use sensible defaults: type = CAFE, campus = ALTSTADT, description = name or fallback
        String finalName = nameVal.orElse("OSM Node " + osmNode.nodeId());
        String finalDescription = nameVal.orElse("Imported from OSM Node " + osmNode.nodeId());

        Pos.PosBuilder builder = Pos.builder()
                .name(finalName)
                .description(finalDescription)
                .type(PosType.CAFE)
                .campus(CampusType.ALTSTADT)
                .street(streetVal.orElse(null))
                .houseNumber(housenumberVal.orElse(null))
                .postalCode(postalCodeInt)
                .city(cityVal.orElse(null));

        Pos pos = builder.build();

        if (!missing.isEmpty()) {
            log.warn("OSM node {} missing fields: {}", osmNode.nodeId(), missing);
        }

        return new ImportResult(pos, missing.isEmpty() ? null : missing);
    }

    /**
     * Performs the actual upsert operation with consistent error handling and logging.
     * Database constraint enforces name uniqueness - data layer will throw DuplicatePosNameException if violated.
     * JPA lifecycle callbacks (@PrePersist/@PreUpdate) set timestamps automatically.
     *
     * @param pos the POS to upsert
     * @return the persisted POS with updated ID and timestamps
     * @throws DuplicatePosNameException if a POS with the same name already exists
     */
    private @NonNull Pos performUpsert(@NonNull Pos pos) throws DuplicatePosNameException {
        try {
            Pos upsertedPos = posDataService.upsert(pos);
            log.info("Successfully upserted POS with ID: {}", upsertedPos.id());
            return upsertedPos;
        } catch (DuplicatePosNameException e) {
            log.error("Error upserting POS '{}': {}", pos.name(), e.getMessage());
            throw e;
        }
    }
}
