# flake8: noqa
print(f"Loading file {__file__!r}")

import time as ttime
import typing

import bluesky
import bluesky.plan_stubs as bps
import bluesky.preprocessors as bpp
import ophyd
from ophyd import Component as Cpt  # Keep 'Device' imported, used in unit tests
from ophyd import Device

from bluesky_queueserver.manager.annotation_decorator import parameter_annotation_decorator

# Some useless devices for unit tests.
custom_test_device = ophyd.Device(name="custom_test_device")
custom_test_signal = ophyd.Signal(name="custom_test_signal")
custom_test_flyer = ophyd.sim.MockFlyer("custom_test_flyer", ophyd.sim.det, ophyd.sim.motor, 1, 5, 20)


@parameter_annotation_decorator(
    {
        "description": "Move motors into positions; then count dets.",
        "parameters": {
            "motors": {
                "description": "List of motors to be moved into specified positions before the measurement",
                "annotation": "typing.List[Motors]",
                "devices": {"Motors": ("motor1", "motor2")},
            },
            "detectors": {
                "description": "Detectors to use for measurement.",
                "annotation": "typing.List[Detectors]",
                "devices": {"Detectors": ("det1", "det2", "det3")},
                "default": ["det1", "det2"],
            },
            "positions": {
                "description": "Motor positions. The number of positions must be equal "
                "to the number of the motors.",
                "annotation": "typing.List[float]",
                "min": -10,
                "max": 10,
                "step": 0.01,
            },
        },
    }
)
def move_then_count(
    motors: typing.List[ophyd.device.Device],
    detectors: typing.Optional[typing.List[ophyd.device.Device]] = None,
    positions: typing.Optional[typing.List[float]] = None,
) -> typing.Generator[bluesky.utils.Msg, None, None]:
    if not isinstance(motors, (list, tuple)):
        raise TypeError(f"Parameter 'motors' should be a list or a tuple: type(motors) = {type(motors)}")
    if not isinstance(detectors, (list, tuple)):
        raise TypeError(f"Parameter 'detectors' should be a list or a tuple: type(detectors) = {type(detectors)}")
    if not isinstance(positions, (list, tuple)):
        raise TypeError(f"Parameter 'positions' should be a list or a tuple: type(positions) = {type(positions)}")
    if len(motors) != len(positions):
        raise TypeError(
            f"The lists of 'motors' and 'positions' should have the same number of elements: "
            f"len(motors) = {len(motors)}, len(positions) = {len(positions)}"
        )

    mv_args = [val for tup in zip(motors, positions) for val in tup]
    yield from bps.mv(*mv_args)
    yield from count(detectors)


@bpp.set_run_key_decorator("run_2")
@bpp.run_decorator(md={})
def _sim_plan_inner(npts: int, delay: float = 1.0):
    for j in range(npts):
        yield from bps.mov(motor1, j * 0.1 + 1, motor2, j * 0.2 - 2)
        yield from bps.trigger_and_read([motor1, motor2, det2])
        yield from bps.sleep(delay)


@bpp.set_run_key_decorator("run_1")
@bpp.run_decorator(md={})
@parameter_annotation_decorator(
    {
        "description": "Simulated multi-run plan: two nested runs. "
        "The plan is included for testing purposes only.",
        "parameters": {
            "npts": {
                "description": "The number of measurements in the outer run. "
                "Inner run will contain 'npts+1' measurements.",
            },
            "delay": {
                "description": "Delay between measurements.",
            },
        },
    }
)
def sim_multirun_plan_nested(npts: int, delay: float = 1.0):
    for j in range(int(npts / 2)):
        yield from bps.mov(motor, j * 0.2)
        yield from bps.trigger_and_read([motor, det])
        yield from bps.sleep(delay)

    yield from _sim_plan_inner(npts + 1, delay)

    for j in range(int(npts / 2), npts):
        yield from bps.mov(motor, j * 0.2)
        yield from bps.trigger_and_read([motor, det])
        yield from bps.sleep(delay)


# =====================================================================================
#                Functions for testing 'function_execute' API.
#
#        NOTE: those functions are used in unit tests. Changing the functions
#                     may cause those tests to fail.


def function_sleep(time):
    """
    Sleep for a given number of seconds.
    """
    print("******** Starting execution of the function 'function_sleep' **************")
    print(f"*******************   Waiting for {time} seconds **************************")
    ttime.sleep(time)
    print("******** Finished execution of the function 'function_sleep' **************")

    return {"success": True, "time": time}


_fifo_buffer = []


def push_buffer_element(element):
    """
    Push an element to a FIFO buffer.
    """
    print("******** Executing the function 'push_buffer_element' **************")
    _fifo_buffer.append(element)


def pop_buffer_element():
    """
    Pop an element from FIFO buffer. Raises exception if the buffer is empty
    (function call fails, traceback should be returned).
    """
    print("******** Executing the function 'pop_buffer_element' **************")
    return _fifo_buffer.pop(0)


def clear_buffer():
    """
    The function used for testing of 'function_execute' API.
    """
    print("******** Executing the function 'clear_buffer' **************")
    return _fifo_buffer.clear()


# ===========================================================================================
#     Simulated devices with subdevices. The devices are used in unit tests. Do not delete.
#     If class names are changed, search and change the names in unit tests.
#     Formatting of imported classes is inconsistent (e.g. 'Device', 'ophyd.Device' and
#     'ophyd.sim.SynAxis') is inconsistent on purpose to check if all possible versions work.


class SimStage(Device):
    x = Cpt(ophyd.sim.SynAxis, name="y", labels={"motors"})
    y = Cpt(ophyd.sim.SynAxis, name="y", labels={"motors"})
    z = Cpt(ophyd.sim.SynAxis, name="z", labels={"motors"})

    def set(self, x, y, z):
        """Makes the device Movable"""
        self.x.set(x)
        self.y.set(y)
        self.z.set(z)


class SimDetectors(Device):
    """
    The detectors are controlled by simulated 'motor1' and 'motor2'
    defined on the global scale.
    """

    det_A = Cpt(
        ophyd.sim.SynGauss,
        name="det_A",
        motor=motor1,
        motor_field="motor1",
        center=0,
        Imax=5,
        sigma=0.5,
        labels={"detectors"},
    )
    det_B = Cpt(
        ophyd.sim.SynGauss,
        name="det_B",
        motor=motor2,
        motor_field="motor2",
        center=0,
        Imax=5,
        sigma=0.5,
        labels={"detectors"},
    )


class SimBundle(ophyd.Device):
    mtrs = Cpt(SimStage, name="mtrs")
    dets = Cpt(SimDetectors, name="dets")


sim_bundle_A = SimBundle(name="sim_bundle_A")
sim_bundle_B = SimBundle(name="sim_bundle_B")  # Used for tests


@parameter_annotation_decorator(
    {
        "parameters": {
            "detectors": {  # Annotation for the parameter 'detectors'
                "annotation": "typing.List[DetList]",
                "devices": {"DetList": ["det", "det1", "det2", "det3", ":+^sim_bundle:?.*$"]},
            }
        }
    }
)
def count_bundle_test(detectors, num=1, delay=None, *, per_shot=None, md=None):
    yield from count(detectors, num=num, delay=delay, per_shot=per_shot, md=md)


# =======================================================================================
#                Plans for testing visualization of Progress Bars

from bluesky.utils import ProgressBar


class StatusPlaceholder:
    "Just enough to make ProgressBar happy. We will update manually."

    def __init__(self):
        self.done = False

    def watch(self, _): ...


def plan_test_progress_bars(n_progress_bars: int = 1):
    """
    Test visualization of progress bars.

    Parameters
    ----------
    n_progress_bars: int
        The number of progress bars to display.
    """
    # Display at least one progress bar
    n_progress_bars = max(n_progress_bars, 1)

    # where the status object computes the fraction
    st_list = [StatusPlaceholder() for _ in range(n_progress_bars)]
    pbar = ProgressBar(st_list)

    v_min = 0
    v_max = 1

    n_pts = 10
    step = (v_max - v_min) / n_pts

    print(f"TESTING {n_progress_bars} PROGRESS BARS ...\n")

    for n in range(n_pts):
        yield from bps.sleep(0.5)
        v = v_min + (n + 1) * step
        for n_pb in range(n_progress_bars):
            pbar.update(n_pb, name=f"TEST{n_pb + 1}", current=v, initial=v_min, target=v_max, fraction=n / n_pts)

    for st in st_list:
        st.done = True
    for n_pb in range(n_progress_bars):
        pbar.update(n_pb, name=f"TEST{n_pb + 1}", current=1, initial=0, target=1, fraction=1)

    s = "\n" * n_progress_bars
    print(f"{s}\nTEST COMPLETED ...")


# =======================================================================================
