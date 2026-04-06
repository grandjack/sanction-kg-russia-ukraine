package com.sanction.kg.repository.neo4j;

import com.sanction.kg.entity.graph.SanctionRelation;
import org.neo4j.driver.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class RelationGraphRepository {

    private static final Logger log = LoggerFactory.getLogger(RelationGraphRepository.class);

    private final Driver driver;

    public RelationGraphRepository(Driver driver) {
        this.driver = driver;
    }

    /**
     * Get all direct relationships for an entity (both incoming and outgoing).
     */
    public List<SanctionRelation> findRelationsByEntityId(String entityId) {
        try (Session session = driver.session()) {
            String cypher = "MATCH (e:Entity {entity_id: $entityId})-[r]-(other:Entity) " +
                    "RETURN startNode(r).entity_id AS sourceId, endNode(r).entity_id AS targetId, " +
                    "type(r) AS relType, r.weight AS weight, r.first_seen_at AS firstSeenAt, " +
                    "r.last_seen_at AS lastSeenAt, r.evidence_ids AS evidenceIds, " +
                    "r.created_version AS createdVersion";

            Result result = session.run(cypher, Values.parameters("entityId", entityId));

            List<SanctionRelation> relations = new ArrayList<>();
            while (result.hasNext()) {
                Record record = result.next();
                relations.add(SanctionRelation.builder()
                        .sourceId(record.get("sourceId").asString())
                        .targetId(record.get("targetId").asString())
                        .type(record.get("relType").asString())
                        .weight(record.get("weight").isNull() ? null : record.get("weight").asDouble())
                        .firstSeenAt(record.get("firstSeenAt").isNull() ? null : record.get("firstSeenAt").asString())
                        .lastSeenAt(record.get("lastSeenAt").isNull() ? null : record.get("lastSeenAt").asString())
                        .evidenceIds(record.get("evidenceIds").isNull() ? null : record.get("evidenceIds").asString())
                        .createdVersion(record.get("createdVersion").isNull() ? null : record.get("createdVersion").asInt())
                        .build());
            }
            return relations;
        }
    }

    /**
     * Get relationship details between two specific entities.
     */
    public List<SanctionRelation> findRelationsBetween(String sourceId, String targetId) {
        try (Session session = driver.session()) {
            String cypher = "MATCH (s:Entity {entity_id: $sourceId})-[r]-(t:Entity {entity_id: $targetId}) " +
                    "RETURN startNode(r).entity_id AS sid, endNode(r).entity_id AS tid, " +
                    "type(r) AS relType, r.weight AS weight, r.first_seen_at AS firstSeenAt, " +
                    "r.last_seen_at AS lastSeenAt, r.evidence_ids AS evidenceIds, " +
                    "r.created_version AS createdVersion";

            Result result = session.run(cypher, Values.parameters("sourceId", sourceId, "targetId", targetId));

            List<SanctionRelation> relations = new ArrayList<>();
            while (result.hasNext()) {
                Record record = result.next();
                relations.add(SanctionRelation.builder()
                        .sourceId(record.get("sid").asString())
                        .targetId(record.get("tid").asString())
                        .type(record.get("relType").asString())
                        .weight(record.get("weight").isNull() ? null : record.get("weight").asDouble())
                        .firstSeenAt(record.get("firstSeenAt").isNull() ? null : record.get("firstSeenAt").asString())
                        .lastSeenAt(record.get("lastSeenAt").isNull() ? null : record.get("lastSeenAt").asString())
                        .evidenceIds(record.get("evidenceIds").isNull() ? null : record.get("evidenceIds").asString())
                        .createdVersion(record.get("createdVersion").isNull() ? null : record.get("createdVersion").asInt())
                        .build());
            }
            return relations;
        }
    }

    /**
     * Count all relationships in the graph.
     */
    public long countAll() {
        try (Session session = driver.session()) {
            Result result = session.run("MATCH ()-[r]->() RETURN count(r) AS cnt");
            if (result.hasNext()) {
                return result.next().get("cnt").asLong();
            }
            return 0;
        }
    }
}
