# -*- coding: utf-8 -*-
"""Evaluation metrics for GSR-ER and HGT-RAM experiments.

Provides:
- Precision, Recall, F1 (for entity/relation extraction)
- P@K, NDCG@K (for risk ranking evaluation)
"""

import numpy as np


# ============================================================
# Section 3.5: GSR-ER Evaluation Metrics
# ============================================================

def precision_recall_f1(tp, fp, fn):
    # type: (int, int, int) -> tuple
    """Compute precision, recall, and F1 score.

    Args:
        tp: True positives.
        fp: False positives.
        fn: False negatives.

    Returns:
        Tuple of (precision, recall, f1).
    """
    precision = tp / (tp + fp) if (tp + fp) > 0 else 0.0
    recall = tp / (tp + fn) if (tp + fn) > 0 else 0.0
    f1 = 2 * precision * recall / (precision + recall) if (precision + recall) > 0 else 0.0
    return precision, recall, f1


def entity_f1(pred_entities, gold_entities):
    # type: (list, list) -> tuple
    """Compute entity-level F1 score using exact span matching.

    Args:
        pred_entities: List of predicted entity tuples (type, start, end, text).
        gold_entities: List of gold entity tuples (type, start, end, text).

    Returns:
        Tuple of (precision, recall, f1).
    """
    pred_set = set(tuple(e) for e in pred_entities)
    gold_set = set(tuple(e) for e in gold_entities)
    tp = len(pred_set & gold_set)
    fp = len(pred_set - gold_set)
    fn = len(gold_set - pred_set)
    return precision_recall_f1(tp, fp, fn)


def relation_f1(pred_relations, gold_relations):
    # type: (list, list) -> tuple
    """Compute relation-level F1 score using exact match.

    A relation tuple: (subject_text, relation_type, object_text).

    Args:
        pred_relations: List of predicted relation tuples.
        gold_relations: List of gold relation tuples.

    Returns:
        Tuple of (precision, recall, f1).
    """
    pred_set = set(tuple(r) for r in pred_relations)
    gold_set = set(tuple(r) for r in gold_relations)
    tp = len(pred_set & gold_set)
    fp = len(pred_set - gold_set)
    fn = len(gold_set - pred_set)
    return precision_recall_f1(tp, fp, fn)


# ============================================================
# Section 4.5: HGT-RAM Ranking Metrics
# ============================================================

def precision_at_k(ranked_list, positives, k):
    # type: (list, set, int) -> float
    """Compute Precision@K for risk ranking.

    Args:
        ranked_list: Ordered list of entity IDs (highest risk first).
        positives: Set of truly high-risk entity IDs.
        k: Cutoff position.

    Returns:
        P@K value.
    """
    if k <= 0 or len(ranked_list) == 0:
        return 0.0
    top_k = ranked_list[:k]
    hits = sum(1 for eid in top_k if eid in positives)
    return hits / k


def ndcg_at_k(ranked_list, relevance_map, k):
    # type: (list, dict, int) -> float
    """Compute NDCG@K (Normalized Discounted Cumulative Gain).

    Equation (4.14): NDCG@K = DCG@K / IDCG@K

    Args:
        ranked_list: Ordered list of entity IDs (highest risk first).
        relevance_map: Dict mapping entity_id -> relevance score (e.g. 0, 1, 2).
        k: Cutoff position.

    Returns:
        NDCG@K value.
    """
    if k <= 0:
        return 0.0

    # DCG@K
    dcg = 0.0
    for i, eid in enumerate(ranked_list[:k]):
        rel = relevance_map.get(eid, 0)
        dcg += (2 ** rel - 1) / np.log2(i + 2)  # i+2 because rank starts at 1

    # IDCG@K: sort relevance values in descending order
    ideal_rels = sorted(relevance_map.values(), reverse=True)[:k]
    idcg = 0.0
    for i, rel in enumerate(ideal_rels):
        idcg += (2 ** rel - 1) / np.log2(i + 2)

    if idcg == 0.0:
        return 0.0
    return dcg / idcg


def evaluate_ranking(ranked_list, positives, ks=None):
    # type: (list, set, list) -> dict
    """Compute full ranking evaluation metrics.

    Args:
        ranked_list: Ordered list of entity IDs (highest risk first).
        positives: Set of truly high-risk entity IDs.
        ks: List of K values for P@K. Default [10, 20, 50].

    Returns:
        Dict with P@K and NDCG@K values.
    """
    if ks is None:
        ks = [10, 20, 50]
    relevance_map = {eid: 1 for eid in positives}
    results = {}
    for k in ks:
        results["P@{}".format(k)] = precision_at_k(ranked_list, positives, k)
        results["NDCG@{}".format(k)] = ndcg_at_k(ranked_list, relevance_map, k)
    return results
