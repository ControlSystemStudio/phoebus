# flake8: noqa
print(f"Loading file {__file__!r}")

from bluesky import RunEngine
from bluesky.callbacks.best_effort import BestEffortCallback

from bluesky_queueserver import is_ipython_mode

# Detect if the code is executed in IPython environment and backend uses Qt
ipython_matplotlib = False
try:
    import matplotlib

    if matplotlib.get_backend().startswith("qt"):
        ipython_matplotlib = True
except Exception:
    pass

RE = RunEngine()

bec = BestEffortCallback()
if not is_ipython_mode() or not ipython_matplotlib:
    bec.disable_plots()

RE.subscribe(bec)
