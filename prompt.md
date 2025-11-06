# Project Requirement Proposal (PRP)

You are a senior Java backend engineer working on a Spring Boot multi-module Maven project using the ports-and-adapters architecture.

Use the following information to implement a new feature.

---

## Goal
Feature Goal: Implement a new import functionality that allows the system to create a Point of Sale (POS) entry directly from an existing OpenStreetMap (OSM) node, identified by its node ID.

Deliverable:
- A new POST endpoint `/api/pos/import/osm/{nodeId}` in the `api` submodule.
- Extend the `PosService` interface with a new method `importFromOsmNode(Long nodeId)` implemented in the `application` module.
- Update the `README.md` with an example API call and expected response.

Success Definition:
- The system successfully imports an OSM node (e.g. ID 5589879349 for “Rada Coffee & Rösterei”).
- Data is fetched from OSM, mapped to a `PointOfSale` domain entity, and persisted.
- The endpoint returns the created POS data as JSON.
- All related tests (including `PosSystemTests`) pass.

---

## User Persona
Target User: Developers or system integrations using the REST API.

Use Case:
A developer or automated integration service imports a new POS location from OpenStreetMap into the system by providing a node ID.

User Journey:
1. API client sends `POST /api/pos/import/osm/{nodeId}` (e.g. `/api/pos/import/osm/5589879349`).
2. The system retrieves OSM node data (via the OpenStreetMap API in XML or JSON format).
3. Data is mapped to internal `PointOfSale` domain model.
4. POS entity is persisted in the database.
5. The API returns a JSON response with the created POS details.

Pain Points Addressed:
- Removes manual data entry for POS.
- Reduces inconsistencies between OSM and internal data.
- Improves developer efficiency.

---

## Why
Business Value:
Automates POS data import from OSM, improving data accuracy and reducing manual work.

Integration with Existing Features:
- Extends existing POS management functionality.
- Integrates with `PosService`, `domain`, and `data` adapters.
- Respects project’s ports-and-adapters modular architecture.

Problems Solved:
- Eliminates manual creation of POS records.
- Ensures standardization with public OSM data.

---

## What
User-visible Behavior and Technical Requirements:
- Endpoint: `POST /api/pos/import/osm/{nodeId}`
- On success: returns HTTP 201 Created with the new POS JSON object.
- On failure (invalid ID, parse error, network issue): returns 400 / 404 / 500.
- The import logic must:
    - Fetch OSM data (XML/JSON)
    - Parse name, address, lat/lon, and tags
    - Map fields to domain entity `PointOfSale`
    - Save via repository or persistence adapter
- Apply standard logging and error handling.
- Follow project modular boundaries:
    - **api** → REST controller
    - **application** → service logic
    - **domain** → business model and ports
    - **data** → persistence implementation

---

## Success Criteria
- `/api/pos/import/osm/{nodeId}` endpoint operational and documented.
- Unit + integration tests for success and failure cases.
- Example import of OSM node 5589879349 (Rada Coffee & Rösterei) persists valid data.
- `PosSystemTests` remain green.

---

## Documentation & References
MUST READ:
- `README.md` at project root (setup + API examples).

Architecture:
- Java Spring Boot multi-module Maven project
- Ports-and-adapters structure with:
    - `api` — Controller adapter
    - `application` — Application services, tests
    - `data` — Persistence adapter
    - `domain` — Business logic, entities, ports

---

# Implementation Instructions
Generate all necessary classes, interfaces, and configurations to implement this feature according to the description above.
Ensure test coverage for success and error scenarios.