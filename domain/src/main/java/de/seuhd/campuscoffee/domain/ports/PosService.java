package de.seuhd.campuscoffee.domain.ports;


import de.seuhd.campuscoffee.domain.exceptions.DuplicatePosNameException;
import de.seuhd.campuscoffee.domain.exceptions.OsmNodeMissingFieldsException;
import de.seuhd.campuscoffee.domain.exceptions.OsmNodeNotFoundException;
import de.seuhd.campuscoffee.domain.exceptions.PosNotFoundException;
import de.seuhd.campuscoffee.domain.model.ImportResult;
import de.seuhd.campuscoffee.domain.model.Pos;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Service interface for POS (Point of Sale) operations.
 * This interface defines the core business logic operations for managing Points of Sale.
 * This is a port in the hexagonal architecture pattern, implemented by the domain layer
 * and consumed by the API layer. It encapsulates business rules and orchestrates
 * data operations through the {@link PosDataService} port.
 */
public interface PosService {
    /**
     * Clears all POS data.
     * This operation removes all Points of Sale from the system.
     * Warning: This is a destructive operation typically used only for testing
     * or administrative purposes. Use with caution in production environments.
     */
    void clear();

    /**
     * Retrieves all Points of Sale in the system.
     *
     * @return a list of all POS entities; never null, but may be empty if no POSs exist
     */
    @NonNull List<Pos> getAll();

    /**
     * Retrieves a specific Point of Sale by its unique identifier.
     *
     * @param id the unique identifier of the POS to retrieve; must not be null
     * @return the POS entity with the specified ID; never null
     * @throws PosNotFoundException if no POS exists with the given ID
     */
    @NonNull Pos getById(@NonNull Long id) throws PosNotFoundException;

    /**
     * Creates a new POS or updates an existing one.
     * This method performs an "upsert" operation:
     * <ul>
     *   <li>If the POS has no ID (null), a new POS is created</li>
     *   <li>If the POS has an ID, and it exists, the existing POS is updated</li>
     * </ul>
     * <p>
     * Business rules enforced:
     * <ul>
     *   <li>POS names must be unique (enforced by database constraint)</li>
     *   <li>All required fields must be present and valid</li>
     *   <li>Timestamps (createdAt, updatedAt) are managed by the {@link PosDataService}.</li>
     * </ul>
     *
     * @param pos the POS entity to create or update; must not be null
     * @return the persisted POS entity with populated ID and timestamps; never null
     * @throws PosNotFoundException if attempting to update a POS that does not exist
     * @throws DuplicatePosNameException if a POS with the same name already exists
     */
    @NonNull Pos upsert(@NonNull Pos pos) throws PosNotFoundException, DuplicatePosNameException;

    /**
     * Imports a Point of Sale from an OpenStreetMap node and returns an ImportResult which
     * contains the created/updated Pos and a list of missing fields (nullable) when a partial import was performed.
     */
    @NonNull ImportResult importFromOsmNode(@NonNull Long nodeId) throws OsmNodeNotFoundException, OsmNodeMissingFieldsException, DuplicatePosNameException;

    /**
     * Variant of {@link #importFromOsmNode(Long)} that allows partial imports when required fields are missing.
     * If {@code allowPartial} is true, the method will create a POS with available fields and return it, and
     * the created POS may have nulls for missing address fields. Implementations should record which fields
     * were missing (logging or metadata) so clients can later complete the data.
     */
    @NonNull ImportResult importFromOsmNode(@NonNull Long nodeId, boolean allowPartial) throws OsmNodeNotFoundException, DuplicatePosNameException;
}
