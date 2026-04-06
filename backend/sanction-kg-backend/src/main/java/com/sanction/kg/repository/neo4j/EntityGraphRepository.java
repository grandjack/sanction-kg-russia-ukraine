package com.sanction.kg.repository.neo4j;

import com.sanction.kg.entity.graph.EntityNode;
import com.sanction.kg.entity.graph.SanctionRelation;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class EntityGraphRepository {

    private static final Logger log = LoggerFactory.getLogger(EntityGraphRepository.class);

    private final Driver driver;

    public EntityGraphRepository(Driver driver) {
        this.driver = driver;
    }

    /**
     * Find entity by entity_id.
     */
    public EntityNode findByEntityId(String entityId) {
        try (Session session = driver.session()) {
            Result result = session.run(
                    "MATCH (e:Entity {entity_id: $entityId}) RETURN e, labels(e) AS lbls",
                    Values.parameters("entityId", entityId));
            if (result.hasNext()) {
                Record record = result.next();
                return mapNodeToEntity(record.get("e").asNode(), record.get("lbls").asList(Value::asString));
            }
            return null;
        }
    }

    /**
     * Search entities by keyword (matches name or aliases), with type filter and pagination.
     */
    public List<EntityNode> searchEntities(String keyword, String entityType, int skip, int limit) {
        try (Session session = driver.session()) {
            StringBuilder cypher = new StringBuilder();
            Map<String, Object> params = new HashMap<>();

            cypher.append("MATCH (e:Entity) WHERE ");

            List<String> conditions = new ArrayList<>();
            if (keyword != null && !keyword.trim().isEmpty()) {
                conditions.add("(toLower(e.name) CONTAINS toLower($keyword) OR toLower(e.aliases) CONTAINS toLower($keyword) OR e.entity_id = $keyword)");
                params.put("keyword", keyword);
            }
            if (entityType != null && !entityType.trim().isEmpty()) {
                conditions.add("e.entity_type = $entityType");
                params.put("entityType", entityType);
            }

            if (conditions.isEmpty()) {
                cypher = new StringBuilder("MATCH (e:Entity) ");
            } else {
                cypher.append(String.join(" AND ", conditions)).append(" ");
            }

            cypher.append("RETURN e, labels(e) AS lbls ORDER BY e.risk_score DESC SKIP $skip LIMIT $limit");
            params.put("skip", skip);
            params.put("limit", limit);

            Result result = session.run(cypher.toString(), params);
            List<EntityNode> entities = new ArrayList<>();
            while (result.hasNext()) {
                Record record = result.next();
                entities.add(mapNodeToEntity(record.get("e").asNode(), record.get("lbls").asList(Value::asString)));
            }
            return entities;
        }
    }

    /**
     * Count entities matching search criteria.
     */
    public long countEntities(String keyword, String entityType) {
        try (Session session = driver.session()) {
            StringBuilder cypher = new StringBuilder();
            Map<String, Object> params = new HashMap<>();

            cypher.append("MATCH (e:Entity) WHERE ");

            List<String> conditions = new ArrayList<>();
            if (keyword != null && !keyword.trim().isEmpty()) {
                conditions.add("(toLower(e.name) CONTAINS toLower($keyword) OR toLower(e.aliases) CONTAINS toLower($keyword) OR e.entity_id = $keyword)");
                params.put("keyword", keyword);
            }
            if (entityType != null && !entityType.trim().isEmpty()) {
                conditions.add("e.entity_type = $entityType");
                params.put("entityType", entityType);
            }

            if (conditions.isEmpty()) {
                cypher = new StringBuilder("MATCH (e:Entity) ");
            } else {
                cypher.append(String.join(" AND ", conditions)).append(" ");
            }

            cypher.append("RETURN count(e) AS cnt");

            Result result = session.run(cypher.toString(), params);
            if (result.hasNext()) {
                return result.next().get("cnt").asLong();
            }
            return 0;
        }
    }

    /**
     * Get relations for an entity up to a given depth.
     */
    public Map<String, Object> getEntityRelations(String entityId, int depth) {
        try (Session session = driver.session()) {
            String cypher = "MATCH path = (e:Entity {entity_id: $entityId})-[r*1.." + depth + "]-(other:Entity) " +
                    "UNWIND relationships(path) AS rel " +
                    "WITH DISTINCT startNode(rel) AS s, rel, endNode(rel) AS t " +
                    "RETURN s.entity_id AS sourceId, s.name AS sourceName, s.entity_type AS sourceType, " +
                    "type(rel) AS relType, rel.weight AS weight, rel.first_seen_at AS firstSeenAt, " +
                    "rel.last_seen_at AS lastSeenAt, rel.evidence_ids AS evidenceIds, " +
                    "t.entity_id AS targetId, t.name AS targetName, t.entity_type AS targetType";

            Result result = session.run(cypher, Values.parameters("entityId", entityId));

            Set<String> seenNodes = new HashSet<>();
            List<Map<String, Object>> nodes = new ArrayList<>();
            List<Map<String, Object>> edges = new ArrayList<>();

            while (result.hasNext()) {
                Record record = result.next();
                String sourceId = record.get("sourceId").asString();
                String targetId = record.get("targetId").asString();

                if (!seenNodes.contains(sourceId)) {
                    seenNodes.add(sourceId);
                    Map<String, Object> node = new HashMap<>();
                    node.put("id", sourceId);
                    node.put("name", record.get("sourceName").asString());
                    node.put("type", record.get("sourceType").asString());
                    nodes.add(node);
                }
                if (!seenNodes.contains(targetId)) {
                    seenNodes.add(targetId);
                    Map<String, Object> node = new HashMap<>();
                    node.put("id", targetId);
                    node.put("name", record.get("targetName").asString());
                    node.put("type", record.get("targetType").asString());
                    nodes.add(node);
                }

                Map<String, Object> edge = new HashMap<>();
                edge.put("source", sourceId);
                edge.put("target", targetId);
                edge.put("type", record.get("relType").asString());
                edge.put("weight", record.get("weight").isNull() ? 0.0 : record.get("weight").asDouble());
                edge.put("firstSeenAt", record.get("firstSeenAt").isNull() ? null : record.get("firstSeenAt").asString());
                edge.put("lastSeenAt", record.get("lastSeenAt").isNull() ? null : record.get("lastSeenAt").asString());
                edges.add(edge);
            }

            Map<String, Object> result2 = new HashMap<>();
            result2.put("nodes", nodes);
            result2.put("edges", edges);
            return result2;
        }
    }

    /**
     * Get k-hop subgraph centered on an entity.
     */
    public Map<String, Object> getSubgraph(String entityId, int depth) {
        try (Session session = driver.session()) {
            String cypher = "MATCH path = (e:Entity {entity_id: $entityId})-[*1.." + depth + "]-(other:Entity) " +
                    "WITH nodes(path) AS ns, relationships(path) AS rs " +
                    "UNWIND ns AS n " +
                    "WITH DISTINCT n, rs " +
                    "UNWIND rs AS r " +
                    "WITH DISTINCT n, r " +
                    "RETURN collect(DISTINCT {id: n.entity_id, name: n.name, type: n.entity_type, " +
                    "riskScore: n.risk_score, sanctionStatus: n.sanction_status, communityId: n.community_id}) AS nodes, " +
                    "collect(DISTINCT {source: startNode(r).entity_id, target: endNode(r).entity_id, " +
                    "type: type(r), weight: r.weight, firstSeenAt: r.first_seen_at, lastSeenAt: r.last_seen_at}) AS edges";

            Result result = session.run(cypher, Values.parameters("entityId", entityId));

            List<Map<String, Object>> nodesList = new ArrayList<>();
            List<Map<String, Object>> edgesList = new ArrayList<>();

            if (result.hasNext()) {
                Record record = result.next();
                List<Object> rawNodes = record.get("nodes").asList();
                List<Object> rawEdges = record.get("edges").asList();

                Set<String> seenNodeIds = new HashSet<>();
                for (Object obj : rawNodes) {
                    if (obj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> nodeMap = (Map<String, Object>) obj;
                        String nodeId = nodeMap.get("id") != null ? nodeMap.get("id").toString() : null;
                        if (nodeId != null && !seenNodeIds.contains(nodeId)) {
                            seenNodeIds.add(nodeId);
                            nodesList.add(nodeMap);
                        }
                    }
                }

                Set<String> seenEdgeKeys = new HashSet<>();
                for (Object obj : rawEdges) {
                    if (obj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> edgeMap = (Map<String, Object>) obj;
                        String edgeKey = edgeMap.get("source") + "->" + edgeMap.get("target") + ":" + edgeMap.get("type");
                        if (!seenEdgeKeys.contains(edgeKey)) {
                            seenEdgeKeys.add(edgeKey);
                            edgesList.add(edgeMap);
                        }
                    }
                }
            }

            // Also add the center node if not already included
            if (nodesList.isEmpty()) {
                EntityNode center = findByEntityId(entityId);
                if (center != null) {
                    Map<String, Object> centerMap = new HashMap<>();
                    centerMap.put("id", center.getEntityId());
                    centerMap.put("name", center.getName());
                    centerMap.put("type", center.getEntityType());
                    centerMap.put("riskScore", center.getRiskScore());
                    centerMap.put("sanctionStatus", center.getSanctionStatus());
                    centerMap.put("communityId", center.getCommunityId());
                    nodesList.add(centerMap);
                }
            }

            Map<String, Object> graphResult = new HashMap<>();
            graphResult.put("nodes", nodesList);
            graphResult.put("edges", edgesList);
            return graphResult;
        }
    }

    /**
     * Find shortest paths between two entities.
     */
    public Map<String, Object> findPaths(String sourceId, String targetId, int maxDepth) {
        try (Session session = driver.session()) {
            String cypher = "MATCH path = shortestPath((s:Entity {entity_id: $sourceId})-[*1.." + maxDepth + "]-(t:Entity {entity_id: $targetId})) " +
                    "UNWIND nodes(path) AS n " +
                    "UNWIND relationships(path) AS r " +
                    "RETURN collect(DISTINCT {id: n.entity_id, name: n.name, type: n.entity_type, " +
                    "riskScore: n.risk_score, sanctionStatus: n.sanction_status}) AS nodes, " +
                    "collect(DISTINCT {source: startNode(r).entity_id, target: endNode(r).entity_id, " +
                    "type: type(r), weight: r.weight}) AS edges";

            Result result = session.run(cypher, Values.parameters("sourceId", sourceId, "targetId", targetId));

            List<Map<String, Object>> nodesList = new ArrayList<>();
            List<Map<String, Object>> edgesList = new ArrayList<>();

            if (result.hasNext()) {
                Record record = result.next();
                List<Object> rawNodes = record.get("nodes").asList();
                List<Object> rawEdges = record.get("edges").asList();
                for (Object obj : rawNodes) {
                    if (obj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> m = (Map<String, Object>) obj;
                        nodesList.add(m);
                    }
                }
                for (Object obj : rawEdges) {
                    if (obj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> m = (Map<String, Object>) obj;
                        edgesList.add(m);
                    }
                }
            }

            Map<String, Object> pathResult = new HashMap<>();
            pathResult.put("nodes", nodesList);
            pathResult.put("edges", edgesList);
            return pathResult;
        }
    }

    /**
     * Count all entity nodes.
     */
    public long countAllEntities() {
        try (Session session = driver.session()) {
            Result result = session.run("MATCH (e:Entity) RETURN count(e) AS cnt");
            if (result.hasNext()) {
                return result.next().get("cnt").asLong();
            }
            return 0;
        }
    }

    /**
     * Count all relationships.
     */
    public long countAllRelations() {
        try (Session session = driver.session()) {
            Result result = session.run("MATCH ()-[r]->() RETURN count(r) AS cnt");
            if (result.hasNext()) {
                return result.next().get("cnt").asLong();
            }
            return 0;
        }
    }

    /**
     * Count entities grouped by type.
     */
    public Map<String, Long> countEntitiesByType() {
        try (Session session = driver.session()) {
            Result result = session.run("MATCH (e:Entity) RETURN e.entity_type AS type, count(e) AS cnt ORDER BY cnt DESC");
            Map<String, Long> counts = new LinkedHashMap<>();
            while (result.hasNext()) {
                Record record = result.next();
                String type = record.get("type").isNull() ? "Unknown" : record.get("type").asString();
                counts.put(type, record.get("cnt").asLong());
            }
            return counts;
        }
    }

    /**
     * Count entities grouped by sanction status.
     */
    public Map<String, Long> countEntitiesByStatus() {
        try (Session session = driver.session()) {
            Result result = session.run("MATCH (e:Entity) RETURN e.sanction_status AS status, count(e) AS cnt ORDER BY cnt DESC");
            Map<String, Long> counts = new LinkedHashMap<>();
            while (result.hasNext()) {
                Record record = result.next();
                String status = record.get("status").isNull() ? "Unknown" : record.get("status").asString();
                counts.put(status, record.get("cnt").asLong());
            }
            return counts;
        }
    }

    /**
     * Get entities ranked by risk score (top N).
     */
    public List<EntityNode> getTopRiskyEntities(int topN) {
        try (Session session = driver.session()) {
            Result result = session.run(
                    "MATCH (e:Entity) WHERE e.risk_score IS NOT NULL " +
                            "RETURN e, labels(e) AS lbls ORDER BY e.risk_score DESC LIMIT $limit",
                    Values.parameters("limit", topN));
            List<EntityNode> entities = new ArrayList<>();
            while (result.hasNext()) {
                Record record = result.next();
                entities.add(mapNodeToEntity(record.get("e").asNode(), record.get("lbls").asList(Value::asString)));
            }
            return entities;
        }
    }

    /**
     * Verify Neo4j connectivity. Returns true if connected.
     */
    public boolean isConnected() {
        try (Session session = driver.session()) {
            session.run("RETURN 1");
            return true;
        } catch (Exception e) {
            log.error("Neo4j connectivity check failed: {}", e.getMessage());
            return false;
        }
    }

    private EntityNode mapNodeToEntity(Node node, List<String> labels) {
        return EntityNode.builder()
                .entityId(getStringProperty(node, "entity_id"))
                .name(getStringProperty(node, "name"))
                .aliases(getStringProperty(node, "aliases"))
                .entityType(getStringProperty(node, "entity_type"))
                .sanctionStatus(getStringProperty(node, "sanction_status"))
                .riskScore(getDoubleProperty(node, "risk_score"))
                .communityId(getIntProperty(node, "community_id"))
                .pagerankScore(getDoubleProperty(node, "pagerank_score"))
                .hawkesIntensity(getDoubleProperty(node, "hawkes_intensity"))
                .createdVersion(getIntProperty(node, "created_version"))
                .updatedVersion(getIntProperty(node, "updated_version"))
                .labels(labels != null ? labels : Collections.<String>emptyList())
                .build();
    }

    private String getStringProperty(Node node, String key) {
        if (node.containsKey(key) && !node.get(key).isNull()) {
            return node.get(key).asString();
        }
        return null;
    }

    private Double getDoubleProperty(Node node, String key) {
        if (node.containsKey(key) && !node.get(key).isNull()) {
            return node.get(key).asDouble();
        }
        return null;
    }

    private Integer getIntProperty(Node node, String key) {
        if (node.containsKey(key) && !node.get(key).isNull()) {
            return node.get(key).asInt();
        }
        return null;
    }
}
