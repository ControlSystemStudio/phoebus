=================
Widget Properties
=================

Each widget has a set of properties which describe the widgets visual and functional behaviours.

Common Widget Properties
========================

The following most basic properties are available on all widgets:
 - "x", "y", "width", "height" that define its position within the display.
 - "name" that can be used to identify a widget.
 - "tooltip", used to provide relevant information when mouse pointer hovers over the widget. See below for additional information.

While not available on all widgets the following properties are associated with almost all widgets
 - "foreground_color"
 - "font" the font associated with the text of the widget

 - "pv_name" the name of a datasource channel which is to be monitored and the values from the channel are used to update the state of the widget

Tool Tip Property
-----------------

Every widget supports the tool tip property, but the default value set in the display editor differs among widgets. For PV aware widgets the default value
is ``$(pv_name)\n$(pv_value)``. In case a PV is not defined by the user for such a widget, the macros ``$(pv_name)`` and ``$(pv_value)`` will not expand to
anything useful. In such cases user should consider to change the tool tip property value, or set it to an empty value.

An empty tool tip property will suppress rendering of a widget tool tip, even if the value for the tool tip text is
set by a rule.