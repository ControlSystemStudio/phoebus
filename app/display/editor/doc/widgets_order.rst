============
Widget Order
============

The order of widgets, that is, the order in which they are listed in the display
file, matters in a few different ways.

1. Widgets are drawn in this order on the display.
   If widgets overlap, those listed later will appear on top of widgets
   that are drawn earlier.

2. When using the keyboard to navigate between widgets in an running display,
   the "TAB" key will navigate to the next widget in this order.
   
3. The editor "Widget Tree" that can sometimes be
   useful to locate widgets will list them in this order.

By default, the widget order is based on how widgets were added to the display.
New widgets are simply added to the end of the display.

There are several options to change the widget order:

* Use the context menu or editor toolbar to move selected widgets "backwards" or "forward",
  which moves them one level in the order, or "back" respectively "front" to move them to the
  start or end of the order.
  
* Use the "Widget Tree" to drag/drop widgets witin the order

* Select either the display background or a group widget, then invoke the "Sort Widgets" option
  from the context menu. This will sort the widgets of the display or of a group by position,
  ordering them left to right and top to bottom.
  If widgets share the same position, they are ordered by name.
  This can be a quick way to establish a useful "TAB" order, but it may not result in the desired order
  for overlapping widgets, which requires either manual adjustment or setting widget names that aid in the sort.
