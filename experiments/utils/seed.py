# -*- coding: utf-8 -*-
"""Random seed utilities for reproducible experiments.

Fixes all random seeds across Python stdlib, NumPy, and PyTorch
to ensure deterministic behaviour in thesis experiments.
"""

import os
import random

import numpy as np
import torch


def fix_all_seeds(seed=42):
    # type: (int) -> None
    """Fix random seeds for reproducibility across all libraries.

    Args:
        seed: Integer seed value. Default 42.
    """
    random.seed(seed)
    np.random.seed(seed)
    os.environ["PYTHONHASHSEED"] = str(seed)

    torch.manual_seed(seed)
    if torch.cuda.is_available():
        torch.cuda.manual_seed(seed)
        torch.cuda.manual_seed_all(seed)
        torch.backends.cudnn.deterministic = True
        torch.backends.cudnn.benchmark = False
