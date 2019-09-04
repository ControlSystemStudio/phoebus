Display Builder Converters
==========================

Converting BOY ``*.opi`` Displays
---------------------------------

The display builder can read existing BOY ``*.opi`` displays.
It will automatically convert the widgets and their properties
from the legacy file format, so you can simply open exiting ``*.opi`` displays
in the display builder runtime.

Since there can be subtle differences in the look and behavior of the
widgets, you may want to manually adjust a few displays.
Do this by opening the existing ``*.opi`` in the Display Builder editor,
adjust as necessary, and then save the file with the new ``*.bob`` file extension.
When the display builder runtime opens a ``*.opi`` display,
it will check for the existence of a ``*.bob`` version and open the latter.
There is thus no need to delete the older ``*.opi`` files,
especially as you transition between tools.
Most ``*.opi`` files will work "as is", without changes, in both BOY
and Display Builder.
You only need to create slightly adjusted ``*.bob`` file versions as necessary,
and the Display Builder runtime will open these instead of the ``*.opi`` files.

Manual adjustments will be necessary for displays that use plots or scripts.
The Display Builder offers a different set of plots,
so legacy displays need to be updated.
Since the underlying Java API of the two tools is dramatically different,
any scripts beyond this will need to be rewritten::

    value = PVUtil.getDouble(pvs[0])
    widget.setPropertyValue("x", value + 100)

See ``script_util/portable.py`` in the examples for hints.


If you prefer to bulk-convert existing ``*.opi`` files into the new file format,
you can do that via this command line invocation::

    Usage: phoebus -main org.csstudio.display.builder.model.Converter [-help] [-output /path/to/folder] <files>

    Converts BOY *.opi files to Display Builder *.bob format

    -output /path/to/folder   - Folder into which converted files are written
    <files>                   - One or more files to convert


Converting MEDM ``*.adl`` Displays
----------------------------------

When you open an MEDM ``*.adl`` in the Phoebus file browser,
it converts this into a ``*.bob`` file which you can then open
in the Display Builder editor or runtime.

For bulk conversions, you can use this command line invocation::

    Usage: phoebus -main org.csstudio.display.converter.medm.Converter [-help] [-output /path/to/folder] <files>

    Converts MEDM *.adl files to Display Builder *.bob format

    -output /path/to/folder   - Folder into which converted files are written
    <files>                   - One or more files to convert

