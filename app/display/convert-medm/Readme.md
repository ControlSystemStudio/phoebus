MEDM File Converter
===================

Converts MEDM `*.adl` files into Display Builder `*.bob` files.

For usage information, see `Display Builder Converters` section in the Phoebus online help.

This code is based on the CS-Studio ADL file parser by H. Rickens (DESY)
and the BOY file converter by J. Hammonds (ANL).
It performs a direct conversion of MEDM widgets into Display Builder widgets.
For example, an MEDM `rectangle` turns into a Display Builder `Rectangle`,
a `text` into a `Label`, a `text update` into a `Text Update`
and a `text entry` into a `Text Entry`.
Positions, sizes, colors and fonts are translated.

There are limitations when it comes to more complex widgets like plots,
which differ both in look and detailed functionality.
Displays that include these widgets likely require
opening the converted file in the Display Builder Editor
to tweak those properties which do not automatically translate.

Finally, the translation does not update MEDM widgets to more complex Display Builder counterparts.
For example, a rectangle and label which surround a set of widgets will not be replaced with
a `Group` that contains them.
A pair of `oval` widgets which are (almost) on top of each other with conditional visibility,
graphically representing an LED, will not be replaced with an `LED` widget.


