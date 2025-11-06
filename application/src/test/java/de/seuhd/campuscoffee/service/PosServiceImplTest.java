package de.seuhd.campuscoffee.service;

import de.seuhd.campuscoffee.domain.exceptions.OsmNodeMissingFieldsException;
import de.seuhd.campuscoffee.domain.model.ImportResult;
import de.seuhd.campuscoffee.domain.model.OsmNode;
import de.seuhd.campuscoffee.domain.model.Pos;
import de.seuhd.campuscoffee.domain.ports.OsmDataService;
import de.seuhd.campuscoffee.domain.ports.PosDataService;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PosServiceImplTest {

    @Test
    void importFromOsmNode_happyPath() {
        // Arrange: create stub OsmDataService that returns a full OsmNode
        OsmDataService osmStub = nodeId -> OsmNode.builder()
                .nodeId(nodeId)
                .lat(49.0)
                .lon(8.0)
                .name("Rada Coffee & Rösterei")
                .tags(Map.of(
                        "name", "Rada Coffee & Rösterei",
                        "addr:street", "Untere Straße",
                        "addr:housenumber", "21",
                        "addr:postcode", "69117",
                        "addr:city", "Heidelberg"
                ))
                .build();

        // PosDataService stub just returns the pos with a fake id (simulate DB)
        PosDataService posDataStub = new PosDataService() {
            @Override
            public void clear() { }

            @Override
            public java.util.List<Pos> getAll() { return java.util.List.of(); }

            @Override
            public Pos getById(Long id) { throw new RuntimeException("Not expected"); }

            @Override
            public Pos upsert(Pos pos) {
                return pos.toBuilder().id(42L).build();
            }
        };

        PosServiceImpl service = new PosServiceImpl(posDataStub, osmStub);

        // Act
        ImportResult result = service.importFromOsmNode(5589879349L);
        Pos created = result.pos();

        // Assert
        assertThat(created).isNotNull();
        assertThat(created.id()).isEqualTo(42L);
        assertThat(created.name()).isEqualTo("Rada Coffee & Rösterei");
        assertThat(created.street()).isEqualTo("Untere Straße");
        assertThat(created.houseNumber()).isEqualTo("21");
        assertThat(created.postalCode()).isEqualTo(69117);
        assertThat(created.city()).isEqualTo("Heidelberg");
        assertThat(result.missingFields()).isNull();
    }

    @Test
    void importFromOsmNode_missingFields() {
        OsmDataService osmStub = nodeId -> OsmNode.builder()
                .nodeId(nodeId)
                .lat(49.0)
                .lon(8.0)
                .name(null)
                .tags(new HashMap<>())
                .build();

        PosDataService posDataStub = new PosDataService() {
            @Override
            public void clear() { }

            @Override
            public java.util.List<Pos> getAll() { return java.util.List.of(); }

            @Override
            public Pos getById(Long id) { throw new RuntimeException("Not expected"); }

            @Override
            public Pos upsert(Pos pos) { return pos; }
        };

        PosServiceImpl service = new PosServiceImpl(posDataStub, osmStub);

        assertThrows(OsmNodeMissingFieldsException.class, () -> service.importFromOsmNode(123L));
    }

    @Test
    void importFromOsmNode_allowPartialTrue_returnsMissingFields() {
        OsmDataService osmStub = nodeId -> OsmNode.builder()
                .nodeId(nodeId)
                .lat(49.0)
                .lon(8.0)
                .name(null)
                .tags(new HashMap<>())
                .build();

        PosDataService posDataStub = new PosDataService() {
            @Override
            public void clear() { }

            @Override
            public java.util.List<Pos> getAll() { return java.util.List.of(); }

            @Override
            public Pos getById(Long id) { throw new RuntimeException("Not expected"); }

            @Override
            public Pos upsert(Pos pos) { return pos.toBuilder().id(1L).build(); }
        };

        PosServiceImpl service = new PosServiceImpl(posDataStub, osmStub);

        ImportResult result = service.importFromOsmNode(123L, true);
        assertThat(result).isNotNull();
        assertThat(result.pos().id()).isEqualTo(1L);
        assertThat(result.missingFields()).isNotNull();
        assertThat(result.missingFields()).contains("name", "addr:street");
    }
}
