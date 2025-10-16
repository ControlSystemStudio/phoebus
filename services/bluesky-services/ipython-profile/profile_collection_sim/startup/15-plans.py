# flake8: noqa
print(f"Loading file {__file__!r}")

from typing import Any, Dict, List, Optional

from bluesky.plans import (
    adaptive_scan,
    count,
    fly,
    grid_scan,
    inner_product_scan,
    list_grid_scan,
    list_scan,
    log_scan,
    ramp_plan,
    rel_adaptive_scan,
    rel_grid_scan,
    rel_list_grid_scan,
    rel_list_scan,
    rel_log_scan,
    rel_scan,
    rel_spiral,
    rel_spiral_fermat,
    rel_spiral_square,
    relative_inner_product_scan,
    scan,
    scan_nd,
    spiral,
    spiral_fermat,
    spiral_square,
    tune_centroid,
    tweak,
    x2x_scan,
)


def marked_up_count(
    detectors: List[Any], num: int = 1, delay: Optional[float] = None, md: Optional[Dict[str, Any]] = None
):
    return (yield from count(detectors, num=num, delay=delay, md=md))
