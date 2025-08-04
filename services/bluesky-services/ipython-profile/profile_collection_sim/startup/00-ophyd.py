# flake8: noqa
print(f"Loading file {__file__!r}")

# Import some built-in plans
from bluesky.plan_stubs import mv, mvr, null
from ophyd.sim import hw

# Import ALL simulated Ophyd objects in global namespace (borrowed from ophyd.sim)
globals().update(hw().__dict__)
del hw
