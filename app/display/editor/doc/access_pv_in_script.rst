.. _access_pv_in_script:

Access PV in Script
###################

**The pvs object**

The input PVs for a script can be accessed in script via ``pvs`` object. The order of the input PVs in the
configuration list is preserved in ``pvs``. ``pvs[0]`` is the first input pv. If you have N input PVs, ``pvs[N-1]`` is the last input PV.

A button-type widget configured to execute script(s) should instead
reference pvs using ``widget.getPV()`` or ``widget.getPVByName(my_pv_name)``.

You can read/write PV or get its timestamp or severity via the utility APIs provided in ``PVUtil``.

**Examples**:

**Get double value from PV:**

.. code-block:: python

    from org.csstudio.display.builder.runtime.script import PVUtil
    value = PVUtil.getDouble(pvs[0]);

**Write PV Value**

Several method argument types are supported, e.g. Double, Double[], Integer, String. If writing a PV is forbidden by
PV security, an exception will be thrown and shown in console.

.. code-block:: python

    pvs[0].write(0);

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
