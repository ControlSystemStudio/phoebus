.. _property_type:

Property Type
=============

There are several different type of widget properties, such as Boolean,
Double, Color, Font and so on. The method ``widget.setPropertyValue(prop_id, value)``
only accepts certain value types corresponding to the property type.
Here is the list of the acceptable value types for each type of property.
Property Value Type is the type of the object returned from ``widget.getPropertyValue(prop_id)``.

.. list-table::
   :widths: 30 70
   :header-rows: 1
   :align: left

   * - Property Type
     - Example Property
   * - `Boolean Property`_
     - Enabled, Visible
   * - `Integer Property`_
     - Height, Width, X, Y
   * - `Double Property`_
     - Meter.Level HIHI, Meter.Maximum
   * - `Combo Property`_
     - Border Style
   * - `String Property`_
     - Name, PV Name, Text
   * - `Color Property`_
     - Background Color, Foreground Color
   * - `Font Property`_
     - Font
   * - `File Path Property`_
     - Image.Image File, Linking Container.OPI File
   * - `PointList Property`_
     - Polyline.Points, Polygon.Points
   * - `Macros Property`_
     - Macros
   * - `ColorMap Property`_
     - IntensityGraph.Color Map

**Actions Property** and **Script Property** are not writeable as they are only loaded once during
the initialization of widget.

Boolean Property
****************

**Examples:**

.. code-block:: javascript

    widget.setPropertyValue("enable", false);
    widget.setPropertyValue("visible", true);

Integer Property
****************

**Examples:**

.. code-block:: javascript

    widget.setPropertyValue("x", 10);

Double Property
***************

**Examples:**

.. code-block:: javascript

    widget.setPropertyValue("fill_level", 35.6);

Combo Property
**************

**Examples:**

.. code-block:: javascript

    //set border style to line style
    widget.setPropertyValue("border_style", 1);

String Property
***************

**Examples:**

.. code-block:: javascript

    widget.setPropertyValue("text", "Hello, World!");

Color Property
**************

**Examples:**

.. code-block:: javascript

    importPackage(Packages.org.csstudio.opibuilder.scriptUtil);
    var ORANGE = ColorFontUtil.getColorFromRGB(255,255,0);
    widget.setPropertyValue("foreground_color",ORANGE);
    widget.setPropertyValue("background_color", "Major"); //"Major" is a color macro

Font Property
*************

**Examples:**

.. code-block:: javascript

    importPackage(Packages.org.csstudio.opibuilder.scriptUtil);
    var bigFont = ColorFontUtil.getFont("Times New Roman", 20, 1);
    widget.setPropertyValue("font", bigFont);

File Path Property
******************

**Examples:**

.. code-block:: javascript

    //load image from relative path
    widget.getWidgetModel().setPropertyValue("image_file", "../pictures/fish.gif");
    //load image from url
    widget.getWidgetModel().setPropertyValue("image_file", "http://neutrons.ornl.gov/images/sns_aerial.jpg");
    //load image from absolute workspace path
    widget.getWidgetModel().setPropertyValue("image_file", "/BOY Examples/widgets/DynamicSymbols/Scared.jpg");
    //load image from local file system
    widget.getWidgetModel().setPropertyValue("image_file", "C:\\Users\\5hz\\Pictures\\me.gif");

PointList Property
******************

**Examples:**

.. code-block:: javascript

    importPackage(Packages.org.csstudio.opibuilder.scriptUtil);
    var jsArray = new Array(20,260,34,56,320,230);
    //set the points for a polygon/polyline widget
    widget.setPropertyValue("points", DataUtil.toJavaIntArray(jsArray));

Macros Property
***************

**Examples:**

.. code-block:: javascript

    importPackage(Packages.org.csstudio.opibuilder.scriptUtil);
    var macroInput = DataUtil.createMacrosInput(true);
    macroInput.put("pv", PVUtil.getString(pvs[0]));
    widget.setPropertyValue("macros", macroInput);

ColorMap Property
*****************

**Examples:**

.. code-block:: javascript

    importPackage(Packages.org.csstudio.opibuilder.scriptUtil);
    var value = PVUtil.getString(pvs[0]);
    widget.setPropertyValue("color_map", value);