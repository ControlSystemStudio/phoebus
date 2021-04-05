.. _access_pv_in_script:

Access PV in Script
###################

**The ``pvs`` object**

The input PVs for a script can be accessed in script via ``pvs`` object. The order of the input PVs in the
configuration list is preserved in ``pvs``. ``pvs[0]`` is the first input pv. If you have N input PVs, ``pvs[N-1]`` is the last input PV.

You may also able to get the PV attached to a PV widget via ``widget.getPV()```.

In script, you can read/write PV or get its timestamp or severity via the utility APIs provided in ``PVUtil``.

** The ``triggerPV`` object**

The PV that triggers the execution of the script can be accessed via ``triggerPV`` object. When there are more
than one trigger PV for a script and you need to know this execution is triggered by which PV, you can use this object. For example,

.. code-block:: javascript

    importPackage(Packages.org.csstudio.opibuilder.scriptUtil);
    if(triggerPV == pvs[1]){
        ConsoleUtil.writeInfo("I'm triggered by the second input PV.");
    }
