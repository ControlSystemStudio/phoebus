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



Converting EDM ``*.edl`` Displays
---------------------------------

To use the EDM converter, add the following to your settings::

    org.csstudio.display.converter.edm/edm_paths_config=/path/to/edm_paths.txt
    org.csstudio.display.converter.edm/colors_list=/path/to/edm/colors.list

For details, see full description of :ref:`preference_settings`.

Each time you now try to open an ``*.edl`` file,
the converter will create a ``*.bob`` file and then open it.

For bulk conversions, you can use this command line invocation,
which can convert a list of files, including complete directories::

    Usage: -main org.csstudio.display.converter.edm.Converter [options] <files>

    Converts EDM *.edl files to Display Builder *.bob format.

    Options:
    -help                        - Help
    -colors /path/to/colors.list - EDM colors.list file to use
    -paths /path/to/paths.list   - File that lists paths
    -output /path/to/folder      - Folder into which converted files are written
    -force                       - Overwrite existing files instead of stopping
    -depth count                 - Convert just the listed files (1), or also referenced files (2), or more levels down
    
The batch converter can also recursively convert referenced files like
embedded displays or related displays.
Refer to the complete ``-help`` output for details.


Auto-Converter
--------------

When a user opens an ``*.edl`` file, it is automatically converted
into a ``*.bob`` file and the latter then opens.

Furthermore, there is an EDM auto-converter that can automatically look for ``*.edl`` files
and convert them.
This way, a site that plans to slowly transition from EDM to the Display Builder
does not need to bulk-convert all files.
Instead, you start with a top-level display that for example includes links
to displays ``a.bob`` and ``b.bob``.
When the user then tries to open ``a.bob`` and it does not exist,
the auto-converter will search for a file ``a.edl`` and auto-convert it.
The next time around, the ``a.bob`` file exists and opens a little faster.
This way, files are auto-converted on first access, on-demand.

To enable the auto-converter, define a folder where the converted files will be stored via this setting::

    org.csstudio.display.converter.edm/auto_converter_dir=/path/to/AUTOCONVERTED_FILES
    
    # Additional EDM converter settings that you likely need:
    org.csstudio.display.converter.edm/colors_list=/path/to/edm/colors.list
    org.csstudio.display.converter.edm/edm_paths_config=/path/to/my_edm_search_paths.txt
    

With the auto-converter folder defined, each time the display builder
needs to open a file ``*.bob`` that does not exist, it will try to
auto-convert it by locating an ``*.edl`` file of the same base name
along the ``edm_paths_config``. If an EDM file is found, it is converted.
In case the EDM file is found via an http link, it is first downloaded.
On success, the resulting ``*.bob`` file is opened.
When that display then refers to other EDM files,
the same process is repeated.
Converted files are stored in the ``auto_converter_dir``,
they are thus only fetched and converted once.
