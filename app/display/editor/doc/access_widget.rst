.. _access_widget:

Access Widget in Script
#######################

**The widget object**

The widget to which a script is attached can be accessed in the script via **widget** object. It is the
controller of the widget. The widget object provides methods to get or set any of its properties,
store external objects or provide special methods for a particular widget.

**Access to widgets in a display**

A script may obtain a list of all widgets in a display like so:

.. code-block:: python

    widget.getDisplayModel().getChildren()

To find a specific widget the script may then loop over the list and use `getName()` to match on the widget
name property.

**Widget class documentation**

Please consult the Java Doc for details on the Widget class and subclasses:

.. raw:: html

   <a href="html/generated/org/csstudio/display/builder/model/Widget.html">Java Doc for Widget class</a><br>
   <a href="html/generated/org/csstudio/display/builder/model/widgets/package-summary.html">Java Doc for Widget subclasses</a>
