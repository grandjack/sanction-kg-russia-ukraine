// Neo4j initialization script for Sanction Knowledge Graph
// Run this against a fresh Neo4j 4.4+ instance

// ----------------------------
// Constraints (uniqueness)
// ----------------------------
CREATE CONSTRAINT entity_id_unique IF NOT EXISTS ON (e:Entity) ASSERT e.entity_id IS UNIQUE;
CREATE CONSTRAINT person_id_unique IF NOT EXISTS ON (p:Person) ASSERT p.entity_id IS UNIQUE;
CREATE CONSTRAINT organization_id_unique IF NOT EXISTS ON (o:Organization) ASSERT o.entity_id IS UNIQUE;
CREATE CONSTRAINT company_id_unique IF NOT EXISTS ON (c:Company) ASSERT c.entity_id IS UNIQUE;
CREATE CONSTRAINT legal_entity_id_unique IF NOT EXISTS ON (l:LegalEntity) ASSERT l.entity_id IS UNIQUE;
CREATE CONSTRAINT vessel_id_unique IF NOT EXISTS ON (v:Vessel) ASSERT v.entity_id IS UNIQUE;
CREATE CONSTRAINT airplane_id_unique IF NOT EXISTS ON (a:Airplane) ASSERT a.entity_id IS UNIQUE;
CREATE CONSTRAINT sanction_id_unique IF NOT EXISTS ON (s:Sanction) ASSERT s.entity_id IS UNIQUE;
CREATE CONSTRAINT security_id_unique IF NOT EXISTS ON (s:Security) ASSERT s.entity_id IS UNIQUE;
CREATE CONSTRAINT crypto_wallet_id_unique IF NOT EXISTS ON (c:CryptoWallet) ASSERT c.entity_id IS UNIQUE;
CREATE CONSTRAINT address_id_unique IF NOT EXISTS ON (a:Address) ASSERT a.entity_id IS UNIQUE;

// ----------------------------
// Indexes for search performance
// ----------------------------
CREATE INDEX entity_name_idx IF NOT EXISTS FOR (e:Entity) ON (e.name);
CREATE INDEX entity_type_idx IF NOT EXISTS FOR (e:Entity) ON (e.entity_type);
CREATE INDEX entity_status_idx IF NOT EXISTS FOR (e:Entity) ON (e.sanction_status);
CREATE INDEX entity_risk_idx IF NOT EXISTS FOR (e:Entity) ON (e.risk_score);
CREATE INDEX entity_community_idx IF NOT EXISTS FOR (e:Entity) ON (e.community_id);

CREATE INDEX person_name_idx IF NOT EXISTS FOR (p:Person) ON (p.name);
CREATE INDEX organization_name_idx IF NOT EXISTS FOR (o:Organization) ON (o.name);
CREATE INDEX company_name_idx IF NOT EXISTS FOR (c:Company) ON (c.name);
CREATE INDEX vessel_name_idx IF NOT EXISTS FOR (v:Vessel) ON (v.name);

// ----------------------------
// Sample data (for thesis demonstration)
// ----------------------------

// Sanctioned entities
CREATE (p1:Person:Entity {
    entity_id: 'PER-001',
    name: 'Vladimir Putin',
    aliases: 'V. Putin, Владимир Путин',
    entity_type: 'Person',
    sanction_status: 'ACTIVE',
    risk_score: 1.0,
    community_id: 1,
    pagerank_score: 0.95,
    hawkes_intensity: 0.8,
    created_version: 1,
    updated_version: 1
});

CREATE (o1:Organization:Entity {
    entity_id: 'ORG-001',
    name: 'Central Bank of Russia',
    aliases: 'CBR, Bank of Russia, Центральный банк Российской Федерации',
    entity_type: 'Organization',
    sanction_status: 'ACTIVE',
    risk_score: 0.95,
    community_id: 1,
    pagerank_score: 0.88,
    hawkes_intensity: 0.75,
    created_version: 1,
    updated_version: 1
});

CREATE (c1:Company:Entity {
    entity_id: 'COM-001',
    name: 'Gazprom',
    aliases: 'PAO Gazprom, ПАО Газпром',
    entity_type: 'Company',
    sanction_status: 'ACTIVE',
    risk_score: 0.85,
    community_id: 1,
    pagerank_score: 0.82,
    hawkes_intensity: 0.65,
    created_version: 1,
    updated_version: 1
});

CREATE (c2:Company:Entity {
    entity_id: 'COM-002',
    name: 'Rosneft',
    aliases: 'PAO NK Rosneft, ПАО НК Роснефть',
    entity_type: 'Company',
    sanction_status: 'ACTIVE',
    risk_score: 0.82,
    community_id: 1,
    pagerank_score: 0.78,
    hawkes_intensity: 0.6,
    created_version: 1,
    updated_version: 1
});

CREATE (p2:Person:Entity {
    entity_id: 'PER-002',
    name: 'Igor Sechin',
    aliases: 'I. Sechin, Игорь Сечин',
    entity_type: 'Person',
    sanction_status: 'ACTIVE',
    risk_score: 0.9,
    community_id: 1,
    pagerank_score: 0.85,
    hawkes_intensity: 0.7,
    created_version: 1,
    updated_version: 1
});

CREATE (s1:Sanction:Entity {
    entity_id: 'SAN-001',
    name: 'OFAC SDN Program - Russia/Ukraine',
    aliases: 'OFAC Russia, EO 14024',
    entity_type: 'Sanction',
    sanction_status: 'ACTIVE',
    risk_score: 1.0,
    community_id: 0,
    pagerank_score: 1.0,
    hawkes_intensity: 0.9,
    created_version: 1,
    updated_version: 1
});

CREATE (v1:Vessel:Entity {
    entity_id: 'VES-001',
    name: 'SCF Primorye',
    aliases: 'Primorye',
    entity_type: 'Vessel',
    sanction_status: 'ACTIVE',
    risk_score: 0.7,
    community_id: 2,
    pagerank_score: 0.45,
    hawkes_intensity: 0.3,
    created_version: 1,
    updated_version: 1
});

CREATE (o2:Organization:Entity {
    entity_id: 'ORG-002',
    name: 'Sovcomflot',
    aliases: 'SCF Group, PAO Sovcomflot',
    entity_type: 'Organization',
    sanction_status: 'ACTIVE',
    risk_score: 0.75,
    community_id: 2,
    pagerank_score: 0.6,
    hawkes_intensity: 0.45,
    created_version: 1,
    updated_version: 1
});

// ----------------------------
// Relationships
// ----------------------------

// Sanctions relationships
MATCH (p:Person {entity_id: 'PER-001'}), (s:Sanction {entity_id: 'SAN-001'})
CREATE (p)-[:SANCTIONED_BY {weight: 1.0, first_seen_at: '2022-02-25', last_seen_at: '2026-04-01', evidence_ids: 'EV-001,EV-002', created_version: 1}]->(s);

MATCH (o:Organization {entity_id: 'ORG-001'}), (s:Sanction {entity_id: 'SAN-001'})
CREATE (o)-[:SANCTIONED_BY {weight: 1.0, first_seen_at: '2022-02-28', last_seen_at: '2026-04-01', evidence_ids: 'EV-003', created_version: 1}]->(s);

MATCH (c:Company {entity_id: 'COM-001'}), (s:Sanction {entity_id: 'SAN-001'})
CREATE (c)-[:SANCTIONED_BY {weight: 0.9, first_seen_at: '2022-03-01', last_seen_at: '2026-04-01', evidence_ids: 'EV-004', created_version: 1}]->(s);

MATCH (c:Company {entity_id: 'COM-002'}), (s:Sanction {entity_id: 'SAN-001'})
CREATE (c)-[:SANCTIONED_BY {weight: 0.9, first_seen_at: '2022-02-28', last_seen_at: '2026-04-01', evidence_ids: 'EV-005', created_version: 1}]->(s);

// Ownership and leadership
MATCH (p:Person {entity_id: 'PER-002'}), (c:Company {entity_id: 'COM-002'})
CREATE (p)-[:LEADER_OF {weight: 0.95, first_seen_at: '2004-07-01', last_seen_at: '2026-04-01', evidence_ids: 'EV-006', created_version: 1}]->(c);

MATCH (p:Person {entity_id: 'PER-001'}), (o:Organization {entity_id: 'ORG-001'})
CREATE (p)-[:PLAYS_SIGNIFICANT_ROLE_IN {weight: 0.99, first_seen_at: '2000-05-07', last_seen_at: '2026-04-01', evidence_ids: 'EV-007', created_version: 1}]->(o);

// Company relationships
MATCH (o:Organization {entity_id: 'ORG-002'}), (v:Vessel {entity_id: 'VES-001'})
CREATE (o)-[:OWNS {weight: 0.85, first_seen_at: '2018-01-01', last_seen_at: '2026-04-01', evidence_ids: 'EV-008', created_version: 1}]->(v);

MATCH (c:Company {entity_id: 'COM-001'}), (c2:Company {entity_id: 'COM-002'})
CREATE (c)-[:ACTS_FOR {weight: 0.4, first_seen_at: '2015-01-01', last_seen_at: '2026-04-01', evidence_ids: 'EV-009', created_version: 1}]->(c2);
