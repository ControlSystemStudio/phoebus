Display Builder
===============

The Display Builder development started in the Eclipse-based version of CS-Studio
as an update of CS-Studio 'BOY', i.e. the `org.csstudio.opibuilder.*` code in 
https://github.com/ControlSystemStudio/cs-studio

It aims for similar functionality and "look",
including the ability to read most existing *.opi files,
while adding these improvements:

 * Model loads in background threads.
   Opening a new display will avoid user interface freeze
   when the display startup is delayed because of embedded displays
   or slow display file access over http.
 * Separation of Model, Representation, Runtime and Editor
   to facilitate long term maintainability.
 * Model without reference to details of the Representation (SWT/GEF, color, dimensions, ..)
   to allow each to be developed and optimized in parallel.
 * Representation could be SWT, AWT, .., JavaFX, favoring the latter
   because it currently promises best performance and long term
   Java support.
 * Runtime handles PV updates and scripts in background threads,
   again lessening the likelihood of user interface freezeups.


Getting Started
---------------

Check the Display Builder examples in the Phoebus-based CS-Studio:

1. Open the menu `Applications`, `Display`, `Examples`, invoke `Install Example Displays`.
2. Select a location where you want to install the examples.
3. Browse the examples. You can right-click a running example and select `Open In Editor` to see how it is configured.
4. Use the Menu `Applications`, `Display`, `New Display` to create your own displays.


Development Details
-------------------

#### Model: `org.csstudio.display.builder.model`

Describes a DisplayModel as a hierarchy of Widgets which each have Properties.
Widget Properties have well defined types. Access to properties is thread-safe.
Listeners can react to widget property changes.
Widgets and their properties can persist to and load from XML files,
using the file system (read, write) or "http:.." URLs (read).
The `src/main/resources/examples/` directory holds the example displays.

Widget categories as well as property categories combined with a well defined order of widget properties
allow editors to present them in a consistent way.

The Model reads existing *.opi files, adapting them to the current model
and writing them in the new format.

#### Representation: `org.csstudio.display.builder.representation`

Represents Widgets in a UI toolkit, i.e. makes them visible on the screen.
In the initial Eclipse-based development, this was implemented for SWT and JavaFX
to demonstrate that different toolkits can be supported,
but SWT implementation was limited because emphasis is on JavaFX,
and the Phoebus version includes the JavaFX implementation.

The representation of a widget needs to add listeners to model properties of interest.
On change, it can prepare the UI update, which is then scheduled via `ToolkitRepresentation.scheduleUpdate()`
to occur on the UI thread in a throttled manner.

#### Runtime: `org.csstudio.display.builder.runtime`

Connects widgets to PVs, executes Jython and JavaScript in background threads.
Throttled updates on user interface thread.
Resolves embedded displays relative to parent.

The base `WidgetRuntime` handles the following:

 * If widget has `pv_name` and `pv_value` properties, a primary PV is created for
   the `pv_name`. The `pv_value` is updated with each `VType` received from that PV.
   Representation then needs to reflect the current `pv_value`.
   
 * Widget can 'write' to the primary PV.
    
 * Each script in the "scripts" property is parsed, its PVs are created,
   and the script is triggered when any of its input PVs change.
   The script can then update widget properties.
   Similarly, "rules" are converted into scripts and then executed.

Most new widgets can simply use the base `WidgetRuntime`,
they do not necessarily need to register their own runtime.
Widgets that rely on several PVs or other runtime functionality,
however, can register their own runtime via SPI.

#### Editor: `org.csstudio.display.builder.editor`

Interactive display editor with Palette, Property Panel, Widget Tree,
copy/paste,
move/resize via tracker, snap-to-grid, snap-to-other-widgets,
align, distribute,
editor for points of polyline/polygon.



Components of a Widget
----------------------

#### Graphical Widgets

A basic graphical widget can be added by implementing a Model and a Representation.

For example, the `EllipseWidget` model provides a `WidgetDescriptor`,
and its `defineProperties()` methods adds the desired properties
to the `BaseWidget`.
For convenience when directly accessing the widget from the representation
or scripts, the `EllipseWidget` also implements methods to access the added
properties, but that is not strictly necessary since one can always access
all properties via  `Widget.getProperty(..)`.

The `EllipseRepresentation` representation creates the actual JavaFX scene elements for
the widget. It registers listeners to the model, and updates the JavaFX scene elements
when the model changes.
Note that the representation does not directly update the elements in the model property listener.
The property model listeners are typically invoked in background threads.
The representation reacts by maybe pre-computing a color or other detail that it needs
to update the JavaFX scene elements, then sets a flag to note what needs to be updated,
and schedules an update on the toolkit's UI thread. Eventually, `updateChanges()` is
called on the UI thread, where the JavaFX elements are updated.

Graphical widgets don't directly use PVs, but the base widget does support rules
and scripts, so properties of a graphical widget could still change in response to PV updates.

#### Monitor Widgets

Widgets based on the `PVWidget` include a primary `pv_name` and `pv_value` property.
The default `WidgetRuntime` connects to the PV specified by the `pv_name`
and updates the `pv_value` with the received values.
The widget representation simply needs to listen to `pv_value` changes
in the same way as it would listen to any other property changes.
For an example, refer to the `TextUpdate` widget and its `TextUpdateRepresentation`.

Widgets that use multiple PVs need to implement their own runtime
to connect to these added PV names, typically updating PV value properties
similar to the default `WidgetRuntime`, and having their representation
listen to these changes. For an example, refer to the `XYPlotWidget`.

#### Control Widgets

Widgets that write to PVs typically react to user input.
For an example, refer to the `TextEntryRepresentation`.
When it receives user input from the JavaFX node,
we want to write a value to the PV.
That PV is maintained by the `WidgetRuntime`.
The representation, however, cannot directly access the runtime.
It is decoupled, because in edit mode there would in fact not be any runtime.
The representation sends an event via
`ToolkitRepresentation.fireWrite(Widget widget, Object value)`.
In runtime mode, the `WidgetRuntime` subscribes to these events
and writes to the PV. 

### SPI
The model, representation and (optional) runtime of a widget
are registered via SPI, using the `WidgetsService`, `WidgetRepresentationsService` and `WidgetRuntimesService`.
This allows site-specific modules to add their own widgets to the Display Builder.


Compatibility with BOY
----------------------

The Display Builder reads existing BOY `*.opi` files.

#### Widget Mappings

Most widgets and their properties aim to be compatible with their BOY counterpart.

In some cases, widget types are mapped.
BOY had a plain rectangle and a rounded rectangle widget, which has been turned
into a rectangle widget with a corner radius property.
BOY had an LED widget, which would either indicate a binary state, which could
be based on a bit in a number or zero vs. non-zero numeric value. That same LED
widget could also reflect one of N states, using very different configuration
parameters. In the Display Builder, there a separate (binary) LED and Multi-State LED widgets.
Rectangle and LED widgets are automatically mapped from `*.opi` files based on their configuration.

The BOY XYGraph had many different modes of operation,
some of which depend on the type of PV (scalar vs. waveform).
The Display Builder offers the XYPlot for plotting X and Y waveforms (with optional error waveform),
and the Data Browser plot for showing scalar PVs over time (with access to history).
Since the PV type is not known when loading a display file,
the Display Builder cannot automatically convert all XYGraphs from `*.opi` files.
It will default to the XYPlot, requiring manual conversion to a Data Browser widget.
In addition, the support for cursors and overlays is different between BOY
and the Display Builder, requiring manual conversion.

#### Groups

The BOY grouping container clipped contained widgets,
which often resulted in displays that included several widgets
which the user could never see because they had accidentally been
moved outside of the group border, yet remained inside the group.

The Display Builder will show such widgets from imported `*.opi` files.
To get the same end result, such 'orphaned' widgets need to be deleted
in the `*.opi` file. In the BOY editor, this is somewhat hard because
you cannot see them. So you need to use the Outline view to select and then
delete them.

#### Embedded Displays

In BOY, displays embedded in "Linking Containers" were merged into the main display.
They were loaded one by one, delaying the load of the complete display
until every embedded display was fetched.

In the Display Builder, embedded displays are treated as a black box.
They are loaded in parallel while the main display is already shown.
The content of each embedded display widget is then represented as soon as it resolves.

The content of an embedded display file can change.
The main display can thus not assume anything about the content of the embedded display.
Likewise, an embedded display can be included by arbitrary parent displays.
Embedded displays are therefore implemented as a sandbox.
Scripts within the main display cannot touch widgets in embedded displays
and vice versa.


#### Alarm Indication, Border

In BOY, the border reduced the usable widget area, causing the widget proper
to grow respectively shrink with border visibility.
Alarm-sensitive borders were only represented via a color.

In the Display Builder, the alarm borders are drawn around the widget,
not affecting the widget size. The alarm states are indicated via color
and line type. Even color blind users can thus distinguish the alarm state,
eliminating the need for alternate alarm indications.

While the alarm-based border can be disabled, the fundamental disconnected state
of a PV is always indicated via the respective border to assert that users
will always be aware of missing data.

#### Macros

Similar to BOY, macros can be in the format `$(macro_name)` as well as `${macro_name}`.

In contrast to EDM and BOY, macros are simply defined and possibly re-defined in the following order:

  1. Environment Variables
  2. System Properties
  3. Widget Property
  4. Preferences
  5. OpenDisplayAction
  6. EmbeddedWidget
  7. DisplayModel
  8. GroupWidget

BOY did not fall back to environment variables or system properties.

While BOY limits macros to string-based properties, more properties now support macros.
For example, the numeric 'x' position can be specified as $(POS).
If the macro does not expand to a valid specification, for example if the macro POS has the value 'eight'
instead of '8', the default value for that property will be used, and a warning is logged.

For displays that are meant as templates, to be invoked with macros,
standalone testing is possible by using the syntax `$(macro_name=default_value)`.
When such displays are invoked with macros, their values are replaced.
If they are invoked without macros, the default value is used.

BOY resp. EDM had options to _not_ inherit parent macros as well as to _not_ replace
the values of existing macros. The new implementation will always inherit all parent macros
and replace them in the order just described.
This simplifies the behavior of macros, since discussions with the implementor of EDM found
no good reason to duplicate the more complicated previous behavior.
As a technical detail, the BOY *.opi XML format treated `"include_parent_macros"`,
the option to inherit parent macros, just like the name of an ordinary macro.
This macro name is now ignored. 

Properties that support macros are based on `MacroizedWidgetProperty`.
They distinguish between the original value specification,
which is a text that may contain macros like `"$(INSTANCE_NUMBER)"`,
and the current value, which evaluates the current macro settings and may be an integer like `8`.


#### Fonts

Since available fonts differ between installations of Windows, Linux, Mac OS X,
the Display Builder defaults to the "Liberation" fonts,
which are included.

Even when the same true-type-fonts were available, the legacy CS-Studio displays rendered
fonts differently across operating systems because it failed to distinguish between
pixels on the screen and font size points.
Font sizes were specified in "points", a unit equal to 1/72 of an inch when printed on paper.
While operator displays use "pixels" for widget locations, sizes, line width etc.,
font specifications like "height 12" were in points.
For SWT as used in the legacy  CS-Studio displays, the on-screen size of fonts depends
on the resolution and size of the display.
For existing *.opi files, the desired font sizes are therefore unknown unless one can measure
them on the OS and hardware where the display was originally executed. 

Using JavaFX, fonts so far appear to be mapped 1 pixel per 1 "point" on Linux, Windows and Mac OS X.
To verify, execute `org.csstudio.display.builder.representation.javafx.JFXFontCalibration`.

Goal for the Display Builder is some level of compatibility with existing *.opi displays
that were created on Linux, and high levels of similarity across operating systems for
newly created displays.


#### Rules

Rules are highly compatible between BOY and the Display Builder.

Internally, however, BOY translated rules into JavaScript, while
the Display Builder translates into Jython.
Rules with boolean expressions like `pv0 > 5  && pv1 < 2`
are translated into `pv0 > 5 and pv1 < 2`,
but expressions that invoked JavaScript methods will need to
be modified into the corresponding Jython code.


#### Scripts

Scripts are generally not portable, since the underlying widget model API is
completely different.

The legacy helper classes from `org.csstudio.opibuilder.scriptUtil` are replaced with similar classes.
For example, references to `org.csstudio.opibuilder.scriptUtil.PVUtil` need to be updated to
`org.csstudio.display.builder.runtime.script.PVUtil`.

__Jython__

Basic Jython scripts similar to this one will work without changes because of compatibility classes:
```
from org.csstudio.opibuilder.scriptUtil import PVUtil
widget.setPropertyValue("text", PVUtil.getString(pvs[0]))
```

For compatibility, classes with the original package name are included.
When accessed the first time, an error is logged:

`Script accessed deprecated org.csstudio.opibuilder.scriptUtil.PVUtil, update to org.csstudio.display.builder.runtime.script.PVUtil`.

Such Jython scripts should be updated to
```
from org.csstudio.display.builder.runtime.script import PVUtil
widget.setPropertyValue("text", PVUtil.getString(pvs[0]))
```

__Python__

In addition to Jython, the Display Builder supports real C-Python scripts.
They are invoked via Py4J, and a helper library is provided that allows
writing Jython as well as Python script in a common way.
Check online help for details.

__Java Script__

JavaScript execution is based on the Nashorn JS engine included since Java 8,
while the legacy tool used the Rhino engine.

Nashorn requires changes to Rhino scripts because 'importPackage' is no longer supported.
Instead of `importPackage`, use the fully qualified name.

Example:

```
importPackage(Packages.org.csstudio.opibuilder.scriptUtil);
widget.setPropertyValue("text", PVUtil.getString(pvs[0]));
```

needs to change into the following, including use of the new package name:

```
PVUtil = org.csstudio.display.builder.runtime.script.PVUtil;
widget.setPropertyValue("text", PVUtil.getString(pvs[0]));
```




Performance: JavaFX vs. SWT
---------------------------

CPU loads were measured with JVisualVM.
Results differ a lot between computers and operating systems.
What matters in the following is the comparison of two tests on the same computer.

`RepresentationDemoJavaFX` vs. `RepresentationDemoSWT`:

 200 Group widgets, each containing
 - Label widget,
 - Textupdate with 10Hz 'ramp' PV,
 - Rectangle with 10Hz 'noise' PV triggering Jython to change rectangle width.

Windows:
Both use the same amount of CPU (~14 %).
JFX version is fluid, time spent in UpdateThrottle waiting for UI is "0 ms".
SWT version shows jumpy rectangle updates, UpdateThrottle waits 140..200 ms for UI updates, profiler shows time spent in `org.eclipse.swt.internal.win32.OS.SetWindowPos`.

Older Ubunty Linux:
JFX version uses 11% CPU and is fluid
SWT version uses 25% CPU and can only update every 4 seconds because stuck in `_gtk_widget_size_allocate`.

Redhat 6:
JFX version uses 8% CPU, "1 ms" in UI updates.
SWT version uses 13% CPU, "10..16 ms" in UI updates.
Both appear fluid.


__-> JavaFX tends to be faster than SWT, especially on some Linux__


`RepresentationDemoJavaFX` vs. `RepresentationDemoJavaFXinSWT`

Windows: 
Updates appear the same,  UpdateThrottle waiting for UI is "0 ms",
but JavaFX in SWT FXCanvas uses about twice the CPU.
`javafx.embed.swt.FXCanvas.paintControl` shows up in profile.

Linux:
Works on RedHat 6, JDK 1.8.0_25, 64 bit.
Time in UI updates just "1 ms", but pure JavaFX has 8% CPU while FXCanvas version uses 20% CPU.
On Ubunty crashes in FXCanvas creation, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=469126

__-> JFX under Eclipse (in SWT FXCanvas) cannot get full performance of standalone JavaFX.__


Performance Comparison to CSS
-----------------------------

Both `RuntimeDemoJavaFX` and CSS can execute
`org.csstudio.display.builder.runtime.test/examples/legacy.opi`.

Linux: RuntimeDemo 10% CPU, CSS 20% CPU.


JavaFX Development Notes
------------------------

The default style sheet for JavaFX applications is modena.css, which is found in the JavaFX runtime JAR file, jfxrt.jar.
This style sheet defines styles for the root node and the UI controls.
To view this file, go to /jre/lib/ext directory of the Java Development Kit (JDK)
and extract the style sheet from the JAR file:

    jar xf jfxrt.jar com/sun/javafx/scene/control/skin/modena/modena.css

To debug the Scene Graph:
* Download Scenic View 8 from http://fxexperience.com/scenic-view
* Unpack
* Add ScenicView.jar to the build path of org.csstudio.display.builder.representation.javafx
* In JFXStageRepresentation:configureStage(), add
   ScenicView.show(scene)


What base class to use for all widget representations?

* Node (Currently used)
Most basic option, allows for any JFX item:
Canvas, Shape, Control, ...

* Region
Allows for Border (alarm sensitive border, ..).
Many widgets are based on a Region to support the alarm sensitive border.

* Control
Has Border, Context Menu, Tool Tip



How to draw custom widgets in background?

JFX Canvas offers good API. Canvas can be prepared off UI thread,
but turns out that drawing operations are simply buffered
to be executed on UI thread once canvas becomes visible,
loading the UI thread.

JFX WritableImage has very limited API.

Best option seems to use AWT to draw buffered image in background thread,
then show that in JFX Canvas.

