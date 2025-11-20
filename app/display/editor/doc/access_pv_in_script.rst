.. _access_pv_in_script:

Access PV in Script
###################

**The pvs object**

The input PVs for a script can be accessed in script via ``pvs`` object. The order of the input PVs in the
configuration list is preserved in ``pvs``. ``pvs[0]`` is the first input pv. If you have N input PVs, ``pvs[N-1]`` is the last input PV.

When using the ``pvs`` object, the display runtime will invoke the script once all PVs connected
and then whenever those marked as a "Trigger" change.
Scripts might also access PVs associated with a widget like this, but the preferred way
is using ``pvs`` for PVs listed as script inputs.

.. code-block:: python

    from org.csstudio.display.builder.runtime.script import ScriptUtil
    pv = ScriptUtil.getPrimaryPV(widget)
    pvs = ScriptUtil.getPVs(widget)
    pv = ScriptUtil.getPVByName(widget, "SomePVName")

You can read/write PV or get its timestamp or severity via the utility APIs provided in ``PVUtil``.

**Examples**:

**Get double value from PV:**

.. code-block:: python

    from org.csstudio.display.builder.runtime.script import PVUtil
    value = PVUtil.getDouble(pvs[0])

**Write PV Value**

While the ``pvs`` are configured as script inputs, they may also be used
as outputs by writing to them.
Several method argument types are supported, e.g. Double, Double[], Integer, String. If writing a PV is forbidden by
PV security, an exception will be thrown and shown in console.

.. code-block:: python

    pvs[0].write(0)

**Get severity of PV**

.. code-block:: python

    from org.csstudio.display.builder.runtime.script import PVUtil, ColorFontUtil

    RED = ColorFontUtil.RED
    ORANGE = ColorFontUtil.getColorFromRGB(255, 255, 0)
    GREEN = ColorFontUtil.getColorFromRGB(0, 255, 0)
    PINK = ColorFontUtil.PINK

    severity = PVUtil.getSeverity(pvs[0])
    color = PINK

    if severity == 0:
    	color = GREEN
    elif severity == 1:
        color = ORANGE
    elif severity == 2:
        color = RED
    else:
        color = PINK

    widget.setPropertyValue("foreground_color",color)
