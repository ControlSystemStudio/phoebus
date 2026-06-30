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

File Property
-------------

Some widget use a File property to identify a resource, local or remote. If such a reference is a local file, then
user is advised to use relative a path as an absolute
path may not be portable between hosts, in particular not between Windows and Linux/Mac hosts.

File paths may be specified using both forward slash (/) or backslash (\\) as path separator as both will work interchangeably
between Windows and Linux/Mac.

Web Browser Widget
==================

The Web Browser widget embeds a web view in a display. In addition to the common properties it provides
the following widget specific properties:

 - "url" the address that is loaded when the display starts.
 - "show_toolbar" whether to show a navigation toolbar (back, forward, reload and an address bar) above the
   page. When turned off, only the page itself is shown.
 - "resize_with_window", described below.

Resize with Window
------------------

By default every widget, including the Web Browser, keeps the size it was given in the editor, and the display
scrolls if the window is smaller than the display. Enabling "resize_with_window" makes the browser instead grow
and shrink to fill the runtime window or tab. This is useful for displays built around a single Web Browser that
should fill the screen.

The property only applies when the Web Browser is a top-level widget, that is a direct child of the display
itself. When the widget is placed inside a Group, a Tab or an embedded display the property has no effect. The
default is off, so existing displays are unaffected.