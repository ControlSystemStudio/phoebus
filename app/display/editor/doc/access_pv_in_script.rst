.. _access_pv_in_script:

Access PV in Script
###################

**The pvs object**

The input PVs for a script can be accessed in script via ``pvs`` object. The order of the input PVs in the
configuration list is preserved in ``pvs``. ``pvs[0]`` is the first input pv. If you have N input PVs, ``pvs[N-1]`` is the last input PV.

A button-type widget configured to execute script(s) should instead
reference pvs using ``widget.getPV()`` or ``widget.getPVByName(my_pv_name)``.

You can read/write PV or get its timestamp or severity via the utility APIs provided in ``PVUtil``.

**The triggerPV object**

The PV that triggers the execution of the script can be accessed via ``triggerPV`` object. When there are more
than one trigger PV for a script and you need to know this execution is triggered by which PV, you can use this object. For example,

.. code-block:: javascript

    importPackage(Packages.org.csstudio.opibuilder.scriptUtil);
    if(triggerPV == pvs[1]){
        ConsoleUtil.writeInfo("I'm triggered by the second input PV.");
    }

**Examples**:

*Get double value from PV:*

.. code-block:: javascript

    importPackage(Packages.org.csstudio.opibuilder.scriptUtil);
    var value = PVUtil.getDouble(pvs[0]);
    widget.setPropertyValue("start_angle", value);

*Write PV Value*

If writing a PV is forbidden by PV security, an exception will be thrown and shown in console. The method ``PV.setValue(data)`` accepts Double, Double[], Integer, String, maybe more.

.. code-block:: javascript

    importPackage(Packages.org.csstudio.platform.data);
    pvs[0].setValue(0);

*Get severity of PV*

.. code-block:: javascript

    importPackage(Packages.org.csstudio.opibuilder.scriptUtil);

    var RED = ColorFontUtil.RED;
    var ORANGE = ColorFontUtil.getColorFromRGB(255,255,0);
    var GREEN = ColorFontUtil.getColorFromHSB(120.0,1.0,1.0);
    var PINK = ColorFontUtil.PINK;

    var severity = PVUtil.getSeverity(pvs[0]);
    var color;

    switch(severity){
        case 0:  //OK
            color = GREEN;
            break;
        case 1:  //MAJOR
            color = RED;
            break;
        case 2:  //MINOR
            color = ORANGE;
            break;
        case -1:  //INVALID
        default:
            color = PINK;
    }

    widget.setPropertyValue("foreground_color",color);
