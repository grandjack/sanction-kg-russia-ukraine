# -*- coding: utf-8 -*-
"""Generate synthetic demo data for sanctions KG experiments.

Creates realistic sanctions network data including:
- ~200 entities across 10 types
- Relations across 7 types with domain/range constraints
- Temporal event sequences for Hawkes process
- Train/val/test document splits for GSR-ER
- Sanctioned/unsanctioned labels for HGT-RAM evaluation

All data is synthetic but follows realistic sanctions network topology:
- Power-law degree distribution
- Community structure (oligarch networks, state entities, shell companies)
- Temporal clustering of sanctions events
"""

from __future__ import annotations

import json
import os
import random
from datetime import datetime, timedelta

import numpy as np
import yaml

# ============================================================
# Constants
# ============================================================

ENTITY_TYPES = [
    "Person", "Organization", "Company", "LegalEntity",
    "Vessel", "Airplane", "Sanction", "Security",
    "CryptoWallet", "Address"
]

RELATION_TYPES = [
    "PLAYS_SIGNIFICANT_ROLE_IN", "SANCTIONED_BY", "FAMILY_RELATIONSHIP",
    "ACTS_FOR", "LEADER_OF", "OWNS", "LOCATED_AT"
]

# Domain/Range constraints (Section 3.3, Table 3.2)
RELATION_CONSTRAINTS = {
    "PLAYS_SIGNIFICANT_ROLE_IN": {
        "domain": ["Person"],
        "range": ["Organization", "Company", "LegalEntity"]
    },
    "SANCTIONED_BY": {
        "domain": ["Person", "Organization", "Company", "LegalEntity",
                    "Vessel", "Airplane", "CryptoWallet"],
        "range": ["Sanction"]
    },
    "FAMILY_RELATIONSHIP": {
        "domain": ["Person"],
        "range": ["Person"]
    },
    "ACTS_FOR": {
        "domain": ["Person", "Organization", "Company"],
        "range": ["Person", "Organization", "Company", "LegalEntity"]
    },
    "LEADER_OF": {
        "domain": ["Person"],
        "range": ["Organization", "Company", "LegalEntity"]
    },
    "OWNS": {
        "domain": ["Person", "Organization", "Company", "LegalEntity"],
        "range": ["Company", "Vessel", "Airplane", "CryptoWallet", "Address"]
    },
    "LOCATED_AT": {
        "domain": ["Person", "Organization", "Company", "LegalEntity",
                    "Vessel", "Airplane"],
        "range": ["Address"]
    }
}

# Realistic name pools
RUSSIAN_FIRST = [
    "Aleksandr", "Dmitry", "Igor", "Oleg", "Viktor", "Sergei", "Andrei",
    "Nikolai", "Boris", "Vladimir", "Yuri", "Mikhail", "Alexei", "Pavel",
    "Roman", "Konstantin", "Arkady", "Gennady", "Leonid", "Anatoly",
    "Ivan", "Pyotr", "Maxim", "Timur", "Ruslan", "Denis", "Kirill"
]

RUSSIAN_LAST = [
    "Ivanov", "Petrov", "Kuznetsov", "Sokolov", "Volkov", "Smirnov",
    "Popov", "Morozov", "Novikov", "Kozlov", "Lebedev", "Semenov",
    "Egorov", "Pavlov", "Orlov", "Fedorov", "Belov", "Zakharov",
    "Grigoryev", "Makarov", "Nikitin", "Romanov", "Baranov", "Klimov"
]

ORG_NAMES = [
    "Rosoboronexport", "Gazprom", "Rosneft", "Sberbank", "VTB Bank",
    "Rostec", "Transneft", "Russian Direct Investment Fund",
    "Sovcomflot", "United Aircraft Corporation", "Kalashnikov Concern",
    "Almaz-Antey", "Russian Railways", "Novatek", "Lukoil",
    "Surgutneftegaz", "Tactical Missiles Corporation"
]

COMPANY_NAMES = [
    "Nordstream AG", "RusChemAlliance", "Baltic Shipping LLC",
    "EuroAsia Trading GmbH", "Black Sea Logistics Ltd",
    "Crimean Bridge Construction Co", "Volga Holdings SA",
    "Ural Mining Corp", "Siberian Resources BV",
    "Caspian Maritime Services", "Donbass Industrial Group",
    "Neva Financial Services", "Kaliningrad Freight Co",
    "Arctic LNG Transport", "Kremlin Properties Ltd",
    "Moskva Capital Partners", "Ural Defense Systems",
    "Vostok Electronics Co", "Zvezda Shipbuilding",
    "Continental Trading FZE", "Gulf Bridge Services DMCC"
]

VESSEL_NAMES = [
    "NS Leader", "Akademik Chersky", "Fortuna", "SCF Primorye",
    "Baltic Explorer", "NS Arctic", "Yenisei River", "Ob River",
    "SCF Ural", "Crimean Bridge", "Black Sea Pioneer",
    "Admiral Kuznetsov", "Pacific Voyager", "Northern Route"
]

AIRPLANE_NAMES = [
    "RA-96017", "RA-82042", "RA-76503", "RA-85684",
    "RA-64010", "RA-73710", "VP-BPB", "VP-CLR"
]

SANCTION_PROGRAMS = [
    "OFAC-SDN-Russia", "OFAC-SDN-Ukraine", "EU-Sanctions-Russia",
    "UK-OFSI-Russia", "UN-SC-2022", "OFAC-SSI", "EU-Sanctions-Belarus"
]

CITIES = [
    "Moscow", "St. Petersburg", "Crimea", "Donetsk", "Luhansk",
    "Sevastopol", "Kaliningrad", "Novosibirsk", "Dubai",
    "Nicosia", "London", "Zurich", "Hong Kong", "Singapore",
    "Istanbul", "Minsk", "Tbilisi", "Astana"
]

# Time range: 2022-02-24 to 2025-12-31
T0 = datetime(2022, 2, 24)
T1 = datetime(2025, 12, 31)
T_DAYS = (T1 - T0).days


def _random_date():
    # type: () -> str
    delta = timedelta(days=random.randint(0, T_DAYS))
    return (T0 + delta).strftime("%Y-%m-%d")


def _random_date_dt():
    # type: () -> datetime
    delta = timedelta(days=random.randint(0, T_DAYS))
    return T0 + delta


def generate_entities(rng):
    # type: (random.Random) -> list
    """Generate ~200 entities with realistic type distribution.

    Type distribution mimics real sanctions lists:
    - Person: ~35%, Organization: ~12%, Company: ~18%, LegalEntity: ~5%
    - Vessel: ~8%, Airplane: ~4%, Sanction: ~4%, Security: ~2%
    - CryptoWallet: ~4%, Address: ~8%
    """
    entities = []
    eid = 0

    # Persons (~70)
    used_names = set()
    for _ in range(70):
        while True:
            first = rng.choice(RUSSIAN_FIRST)
            last = rng.choice(RUSSIAN_LAST)
            name = "{} {}".format(first, last)
            if name not in used_names:
                used_names.add(name)
                break
        entities.append({
            "entity_id": "ENT-{:04d}".format(eid),
            "entity_type": "Person",
            "name": name,
            "aliases": ["{}, {}".format(last, first)],
            "sanctioned": rng.random() < 0.35,
            "created_at": _random_date()
        })
        eid += 1

    # Organizations (~24)
    for name in ORG_NAMES[:17]:
        entities.append({
            "entity_id": "ENT-{:04d}".format(eid),
            "entity_type": "Organization",
            "name": name,
            "aliases": [],
            "sanctioned": rng.random() < 0.6,
            "created_at": _random_date()
        })
        eid += 1
    for i in range(7):
        entities.append({
            "entity_id": "ENT-{:04d}".format(eid),
            "entity_type": "Organization",
            "name": "State Agency {}".format(i + 1),
            "aliases": [],
            "sanctioned": rng.random() < 0.4,
            "created_at": _random_date()
        })
        eid += 1

    # Companies (~36)
    for name in COMPANY_NAMES:
        entities.append({
            "entity_id": "ENT-{:04d}".format(eid),
            "entity_type": "Company",
            "name": name,
            "aliases": [],
            "sanctioned": rng.random() < 0.3,
            "created_at": _random_date()
        })
        eid += 1

    # LegalEntity (~10)
    for i in range(10):
        entities.append({
            "entity_id": "ENT-{:04d}".format(eid),
            "entity_type": "LegalEntity",
            "name": "Legal Entity {} Trust".format(i + 1),
            "aliases": [],
            "sanctioned": rng.random() < 0.2,
            "created_at": _random_date()
        })
        eid += 1

    # Vessels (~14)
    for name in VESSEL_NAMES:
        entities.append({
            "entity_id": "ENT-{:04d}".format(eid),
            "entity_type": "Vessel",
            "name": name,
            "aliases": [],
            "sanctioned": rng.random() < 0.5,
            "created_at": _random_date()
        })
        eid += 1

    # Airplanes (~8)
    for name in AIRPLANE_NAMES:
        entities.append({
            "entity_id": "ENT-{:04d}".format(eid),
            "entity_type": "Airplane",
            "name": name,
            "aliases": [],
            "sanctioned": rng.random() < 0.4,
            "created_at": _random_date()
        })
        eid += 1

    # Sanction Programs (~7)
    for name in SANCTION_PROGRAMS:
        entities.append({
            "entity_id": "ENT-{:04d}".format(eid),
            "entity_type": "Sanction",
            "name": name,
            "aliases": [],
            "sanctioned": False,
            "created_at": _random_date()
        })
        eid += 1

    # Security (~4)
    for i in range(4):
        entities.append({
            "entity_id": "ENT-{:04d}".format(eid),
            "entity_type": "Security",
            "name": "Security Instrument {}".format(i + 1),
            "aliases": [],
            "sanctioned": False,
            "created_at": _random_date()
        })
        eid += 1

    # CryptoWallets (~8)
    for i in range(8):
        addr = "0x" + "".join(rng.choices("0123456789abcdef", k=40))
        entities.append({
            "entity_id": "ENT-{:04d}".format(eid),
            "entity_type": "CryptoWallet",
            "name": addr,
            "aliases": [],
            "sanctioned": rng.random() < 0.5,
            "created_at": _random_date()
        })
        eid += 1

    # Addresses (~18)
    for city in CITIES:
        entities.append({
            "entity_id": "ENT-{:04d}".format(eid),
            "entity_type": "Address",
            "name": "{}, {}".format(rng.randint(1, 200), city),
            "aliases": [city],
            "sanctioned": False,
            "created_at": _random_date()
        })
        eid += 1

    return entities


def generate_relations(entities, rng):
    # type: (list, random.Random) -> list
    """Generate relations respecting domain/range constraints.

    Creates ~400 relations with realistic network structure:
    - SANCTIONED_BY links from sanctioned entities to sanction programs
    - OWNS / LEADER_OF / ACTS_FOR creating corporate hierarchies
    - FAMILY_RELATIONSHIP among persons
    - LOCATED_AT for geographic grounding
    """
    # Index entities by type
    by_type = {}  # type: dict
    for e in entities:
        t = e["entity_type"]
        if t not in by_type:
            by_type[t] = []
        by_type[t].append(e)

    eid_map = {e["entity_id"]: e for e in entities}
    relations = []
    rid = 0

    def _add_rel(subj_id, rel_type, obj_id, weight=None):
        nonlocal rid
        if weight is None:
            edge_weights = {
                "OWNS": 0.9, "LEADER_OF": 0.8, "ACTS_FOR": 0.85,
                "FAMILY_RELATIONSHIP": 0.6, "SANCTIONED_BY": 1.0,
                "PLAYS_SIGNIFICANT_ROLE_IN": 0.88, "LOCATED_AT": 0.3
            }
            weight = edge_weights.get(rel_type, 0.5)
        relations.append({
            "relation_id": "REL-{:04d}".format(rid),
            "subject_id": subj_id,
            "relation_type": rel_type,
            "object_id": obj_id,
            "weight": weight,
            "confidence": round(rng.uniform(0.7, 1.0), 3),
            "first_seen_at": _random_date(),
            "last_seen_at": _random_date(),
            "evidence_ids": ["EV-{:05d}".format(rng.randint(0, 9999))]
        })
        rid += 1

    # 1. SANCTIONED_BY: sanctioned entities -> sanction programs
    sanction_programs = by_type.get("Sanction", [])
    for e in entities:
        if e["sanctioned"] and e["entity_type"] != "Sanction":
            constraint = RELATION_CONSTRAINTS["SANCTIONED_BY"]
            if e["entity_type"] in constraint["domain"]:
                prog = rng.choice(sanction_programs)
                _add_rel(e["entity_id"], "SANCTIONED_BY", prog["entity_id"])
                # Some entities sanctioned by multiple programs
                if rng.random() < 0.3:
                    prog2 = rng.choice(sanction_programs)
                    if prog2["entity_id"] != prog["entity_id"]:
                        _add_rel(e["entity_id"], "SANCTIONED_BY", prog2["entity_id"])

    # 2. LEADER_OF: Persons -> Organizations/Companies
    persons = by_type.get("Person", [])
    orgs_companies = by_type.get("Organization", []) + by_type.get("Company", [])
    for org in orgs_companies:
        leader = rng.choice(persons)
        _add_rel(leader["entity_id"], "LEADER_OF", org["entity_id"])

    # 3. OWNS: corporate ownership chains
    owners = (by_type.get("Person", []) + by_type.get("Organization", []) +
              by_type.get("Company", []))
    owned = (by_type.get("Company", []) + by_type.get("Vessel", []) +
             by_type.get("Airplane", []) + by_type.get("CryptoWallet", []))
    for target in owned:
        owner = rng.choice(owners)
        if owner["entity_id"] != target["entity_id"]:
            _add_rel(owner["entity_id"], "OWNS", target["entity_id"])
    # Extra ownership links for shell company chains
    companies = by_type.get("Company", [])
    for _ in range(15):
        if len(companies) >= 2:
            c1, c2 = rng.sample(companies, 2)
            _add_rel(c1["entity_id"], "OWNS", c2["entity_id"])

    # 4. ACTS_FOR: agency relationships
    for _ in range(25):
        actor_pool = persons + by_type.get("Organization", []) + companies
        target_pool = (persons + by_type.get("Organization", []) +
                       companies + by_type.get("LegalEntity", []))
        actor = rng.choice(actor_pool)
        target = rng.choice(target_pool)
        if actor["entity_id"] != target["entity_id"]:
            _add_rel(actor["entity_id"], "ACTS_FOR", target["entity_id"])

    # 5. PLAYS_SIGNIFICANT_ROLE_IN
    org_targets = (by_type.get("Organization", []) + companies +
                   by_type.get("LegalEntity", []))
    for _ in range(20):
        person = rng.choice(persons)
        org = rng.choice(org_targets)
        _add_rel(person["entity_id"], "PLAYS_SIGNIFICANT_ROLE_IN",
                 org["entity_id"])

    # 6. FAMILY_RELATIONSHIP
    for _ in range(15):
        if len(persons) >= 2:
            p1, p2 = rng.sample(persons, 2)
            _add_rel(p1["entity_id"], "FAMILY_RELATIONSHIP", p2["entity_id"])

    # 7. LOCATED_AT
    addresses = by_type.get("Address", [])
    locatable = (persons + by_type.get("Organization", []) + companies +
                 by_type.get("LegalEntity", []) + by_type.get("Vessel", []) +
                 by_type.get("Airplane", []))
    for e in locatable:
        if rng.random() < 0.4:
            addr = rng.choice(addresses)
            _add_rel(e["entity_id"], "LOCATED_AT", addr["entity_id"])

    return relations


def generate_temporal_events(entities, rng):
    # type: (list, random.Random) -> dict
    """Generate temporal event sequences for Hawkes process (Section 4.3).

    Events are clustered in time to simulate sanctions waves:
    - Wave 1: Feb-Apr 2022 (initial sanctions post-invasion)
    - Wave 2: Jun-Aug 2022 (expanded sanctions)
    - Wave 3: Feb-Mar 2023 (anniversary sanctions)
    - Wave 4: Feb 2024 (two-year anniversary)
    - Sporadic events throughout

    Returns:
        Dict mapping entity_id -> list of event timestamps (days since T0).
    """
    # Sanctions wave centers (days since T0)
    waves = [
        (10, 15),    # Wave 1: early March 2022, std ~15 days
        (130, 20),   # Wave 2: July 2022
        (370, 15),   # Wave 3: March 2023
        (730, 15),   # Wave 4: Feb 2024
        (1000, 20),  # Wave 5: late 2024
    ]

    events = {}  # type: dict
    for e in entities:
        if e["entity_type"] in ("Sanction", "Security", "Address"):
            continue

        n_events = 0
        if e["sanctioned"]:
            n_events = rng.randint(3, 12)
        else:
            n_events = rng.randint(0, 4)

        timestamps = []
        for _ in range(n_events):
            # Pick a wave with probability proportional to intensity
            wave_center, wave_std = rng.choice(waves)
            t = rng.gauss(wave_center, wave_std)
            t = max(0, min(T_DAYS, t))
            timestamps.append(round(t, 2))

        # Add some sporadic events
        n_sporadic = rng.randint(0, 2)
        for _ in range(n_sporadic):
            timestamps.append(round(rng.uniform(0, T_DAYS), 2))

        timestamps.sort()
        if timestamps:
            events[e["entity_id"]] = timestamps

    return events


def generate_gsr_er_documents(entities, relations, rng):
    # type: (list, list, random.Random) -> list
    """Generate synthetic annotated documents for GSR-ER evaluation.

    Creates 1200 documents with entity mentions and relation annotations.
    Each document is a short text snippet with annotated spans.
    """
    eid_map = {e["entity_id"]: e for e in entities}
    templates = [
        "The US Treasury Department designated {subj} under the {obj} program "
        "for operating in the defense sector of the Russian Federation.",
        "{subj} has been identified as the leader of {obj}, a Russian "
        "state-owned enterprise involved in energy exports.",
        "According to OFAC records, {subj} is owned by {obj} and has been "
        "involved in sanctions evasion activities.",
        "{subj} acts as an intermediary for {obj} in international trade "
        "transactions designed to circumvent sanctions.",
        "{subj} plays a significant role in {obj}'s operations, including "
        "procurement of dual-use technology.",
        "Intelligence reports indicate that {subj} is a family member of "
        "{obj}, a previously designated individual.",
        "{subj} is registered at the address {obj} in a known sanctions "
        "jurisdiction.",
        "Vessel {subj} was sanctioned under {obj} for transporting crude "
        "oil in violation of price cap restrictions.",
        "{subj}, a shell company owned by {obj}, facilitated the transfer "
        "of restricted goods to sanctioned entities.",
        "Cryptocurrency wallet {subj} linked to {obj} was used to process "
        "transactions worth over $10 million.",
    ]

    documents = []
    for doc_id in range(1200):
        # Pick 1-3 relations to include in the document
        n_rels = rng.randint(1, 3)
        doc_rels = rng.sample(relations, min(n_rels, len(relations)))

        text_parts = []
        doc_entities = []
        doc_relations = []

        for rel in doc_rels:
            subj = eid_map.get(rel["subject_id"])
            obj = eid_map.get(rel["object_id"])
            if subj is None or obj is None:
                continue

            template = rng.choice(templates)
            text = template.format(subj=subj["name"], obj=obj["name"])
            text_parts.append(text)

            # Create entity annotations
            subj_start = text.find(subj["name"])
            if subj_start >= 0:
                doc_entities.append({
                    "type": subj["entity_type"],
                    "start": subj_start,
                    "end": subj_start + len(subj["name"]),
                    "text": subj["name"]
                })
            obj_start = text.find(obj["name"])
            if obj_start >= 0:
                doc_entities.append({
                    "type": obj["entity_type"],
                    "start": obj_start,
                    "end": obj_start + len(obj["name"]),
                    "text": obj["name"]
                })

            doc_relations.append({
                "subject": subj["name"],
                "relation": rel["relation_type"],
                "object": obj["name"]
            })

        full_text = " ".join(text_parts)
        documents.append({
            "doc_id": "DOC-{:04d}".format(doc_id),
            "text": full_text,
            "entities": doc_entities,
            "relations": doc_relations
        })

    return documents


def generate_risk_labels(entities, relations):
    # type: (list, list) -> dict
    """Generate ground-truth risk labels for HGT-RAM evaluation.

    High-risk entities (label=1):
    - Directly sanctioned entities
    - Entities owned by sanctioned entities (1-hop risk)

    Medium-risk entities (label=0.5):
    - 2-hop from sanctioned via OWNS/ACTS_FOR/LEADER_OF

    Low-risk (label=0): all others.
    """
    sanctioned_ids = set(
        e["entity_id"] for e in entities if e["sanctioned"]
    )

    # Build adjacency from relations
    # ownership / control edges (high-risk propagation)
    high_risk_rels = {"OWNS", "ACTS_FOR", "LEADER_OF", "SANCTIONED_BY"}
    adj = {}  # type: dict  # entity_id -> set of entity_ids
    for r in relations:
        if r["relation_type"] in high_risk_rels:
            sid = r["subject_id"]
            oid = r["object_id"]
            if sid not in adj:
                adj[sid] = set()
            adj[sid].add(oid)
            if oid not in adj:
                adj[oid] = set()
            adj[oid].add(sid)

    labels = {}
    for e in entities:
        eid = e["entity_id"]
        if eid in sanctioned_ids:
            labels[eid] = 1.0
            continue

        # Check 1-hop
        neighbors = adj.get(eid, set())
        if neighbors & sanctioned_ids:
            labels[eid] = 1.0
            continue

        # Check 2-hop
        found_2hop = False
        for n in neighbors:
            n_neighbors = adj.get(n, set())
            if n_neighbors & sanctioned_ids:
                found_2hop = True
                break
        if found_2hop:
            labels[eid] = 0.5
        else:
            labels[eid] = 0.0

    return labels


def main():
    # type: () -> None
    """Generate all demo data and write to files."""
    seed = 42
    rng = random.Random(seed)
    np.random.seed(seed)

    out_dir = os.path.dirname(os.path.abspath(__file__))

    print("Generating entities...")
    entities = generate_entities(rng)
    print("  Created {} entities".format(len(entities)))

    print("Generating relations...")
    relations = generate_relations(entities, rng)
    print("  Created {} relations".format(len(relations)))

    print("Generating temporal events...")
    events = generate_temporal_events(entities, rng)
    print("  Created event sequences for {} entities".format(len(events)))

    print("Generating GSR-ER documents...")
    documents = generate_gsr_er_documents(entities, relations, rng)
    print("  Created {} documents".format(len(documents)))

    # Split documents: 6:2:2 (Section 3.5)
    n = len(documents)
    n_train = int(n * 0.6)
    n_val = int(n * 0.2)
    train_docs = documents[:n_train]
    val_docs = documents[n_train:n_train + n_val]
    test_docs = documents[n_train + n_val:]

    print("Generating risk labels...")
    risk_labels = generate_risk_labels(entities, relations)
    n_high = sum(1 for v in risk_labels.values() if v == 1.0)
    n_med = sum(1 for v in risk_labels.values() if v == 0.5)
    n_low = sum(1 for v in risk_labels.values() if v == 0.0)
    print("  High-risk: {}, Medium: {}, Low: {}".format(n_high, n_med, n_low))

    # Write output files
    def _write_json(data, filename):
        filepath = os.path.join(out_dir, filename)
        with open(filepath, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
        print("  Wrote {}".format(filepath))

    _write_json(entities, "entities.json")
    _write_json(relations, "relations.json")
    _write_json(events, "temporal_events.json")
    _write_json(train_docs, "gsr_er_train.json")
    _write_json(val_docs, "gsr_er_val.json")
    _write_json(test_docs, "gsr_er_test.json")
    _write_json(risk_labels, "risk_labels.json")

    # Summary stats
    summary = {
        "num_entities": len(entities),
        "num_relations": len(relations),
        "entity_type_counts": {},
        "relation_type_counts": {},
        "num_documents": len(documents),
        "train_docs": len(train_docs),
        "val_docs": len(val_docs),
        "test_docs": len(test_docs),
        "num_sanctioned": sum(1 for e in entities if e["sanctioned"]),
        "num_event_sequences": len(events),
        "risk_label_distribution": {
            "high": n_high, "medium": n_med, "low": n_low
        }
    }
    for e in entities:
        t = e["entity_type"]
        summary["entity_type_counts"][t] = summary["entity_type_counts"].get(t, 0) + 1
    for r in relations:
        t = r["relation_type"]
        summary["relation_type_counts"][t] = summary["relation_type_counts"].get(t, 0) + 1

    _write_json(summary, "data_summary.json")
    print("\nDone! Data summary:")
    print(json.dumps(summary, indent=2))


if __name__ == "__main__":
    main()
