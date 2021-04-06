.. _access_widget:

Access Widget in Script
#######################

**The widget object**

The widget to which a script is attached can be accessed in the script via **widget** object. It is the
controller (or EditPart in GEF) of the widget. The widget object provides methods to get or set any of its properties,
store external objects or provide special methods for a particular widget.

**The display object**

The widget controller of the display is accessible in all scripts as a **display** object. To get the controller of any
widget in the display, you can call its method ``getWidget(name)``. For example:

.. code-block:: javascript

    display.getWidget("myLabel").setPropertyValue("x", 20); //set x position of the widget myLabel to 20.

**Common methods to all widgets**

- `getPropertyValue(prop_id)`_

- `setPropertyValue(prop_id, value)`_

- `setPropertyValue(prop_id, value, forceFire)`_

- `getPVByName(pvName)`_

- `setVar(varName, varValue)`_

- `getVar(name)`_

- `getMacroValue(macroName)`_

- `executeAction(index)`_

- `setEnabled(enable)`_

- `setVisible(visible)`_

- `setX(x)`_

- `setY(y)`_

- `setWidth(width)`_

- `setHeight(height)`_

**Common methods to all container widgets**

Container widgets includes Display, Group, Embedded Display and Tabs.

- `getChild(name)`_

- `getWidget(name)`_

- `addChild(model)`_

- `addChildToRight(model)`_

- `addChildToBottom(model)`_

- `removeChildByName(name)`_

- `removeChild(widget)`_

- `removeChild(index)`_

- `removeAllChildren()`_

- `performAutosize()`_

- `getValue( )`_

- `setValue(value )`_

**Common methods to all PV widgets**

Any widget that has PV Name property is PV widget. For example, Text Update, Combo Box, XY Graph etc.

- `getPV()`_

- `getPV(pvPropId)`_

- `getValue()`_

- `setValue(value)`_

- `setValueInUIThread(value)`_

**Special methods to each widget**

Some widget may have special methods. See the document for each widget.

getPropertyValue(prop_id)
*************************

``public java.lang.Object getPropertyValue(java.lang.String prop_id)``

Get property value of the widget.

**Parameters:**

``prop_id`` - the property id. In most cases, it is the lower case of the property name.

**Returns:**

 The property value.

**Example:**

.. code-block:: javascript

    var x = widget.getPropertyValue("x"); //get x position of the widget

setPropertyValue(prop_id, value)
********************************

``public void setPropertyValue(java.lang.String prop_id, java.lang.Object value)``

Set the property value of the widget.

**Parameters:**

``prop_id`` - the property id. In most cases, it is the lower case of the property name.

``value`` - the value. It must be the allowed input type corresponding to the property type. See :ref:`property_type`.

**Example:**

.. code-block:: javascript

    widget.setPropertyValue("x", 20); //set x position of the widget to 20.

setPropertyValue(prop_id, value, forceFire)
*******************************************

``public void setPropertyValue(java.lang.String prop_id, java.lang.Object value, boolean forceFire)``

Set the property value of the widget.

**Parameters:**

``prop_id`` - the property id.

``value`` - the value.

``forceFire`` - If ``true``, the property will be set again even if the new value is same as old value. If ``false`` and the new value is same as the old value, it will be ignored.

**Example:**

.. code-block:: javascript

    widget.setPropertyValue("opi_file", widget.getPropertyValue("opi_file"), true); //reload OPI in linking container.

getPVByName(pvName)
*******************

``public org.csstudio.utility.pv.PV getPVByName(java.lang.String pvName)``

Get PV attached to this widget by pv name. It includes the PVs in Rules and Scripts.

**Parameters:**

``pvName`` - name of the PV.

**Returns:**

A PV object, or null if no such PV exists.

setVar(varName, varValue)
*************************

``public void setVar(java.lang.String varName, java.lang.Object varValue)``

Set variable value. If the variable does not exist, it will be added to this widget.

**Parameters:**

``varName`` - name of the variable.

``varValue`` - value of the variable, which can be any type.

getVar(name)
************

``public java.lang.Object getVar(java.lang.String name)``

Get the value of a Variable.

**Returns:**

the variable value or null if no such variable has been set.

**Example:**

This is an example which uses these two methods to determine if a dialog has been shown previously.

.. code-block:: javascript

    importPackage(Packages.org.eclipse.jface.dialogs);
    importPackage(Packages.org.csstudio.platform.data);
    importPackage(Packages.java.lang);

    var flagName = "popped";

    if(widget.getVar(flagName) == null){
        widget.setVar(flagName, false);
    }

    var b = widget.getVar(flagName);

    if(ValueUtil.getDouble(pvs[0].getValue()) > 80){
        if( b == false){
            widget.setVar(flagName, true);
            MessageDialog.openWarning(
                null, "Warning", "The temperature you set is too high!");
        }
    }
    else if (b == true){
        widget.setVar(flagName, false);
    }

getMacroValue(macroName)
************************

``public java.lang.String getMacroValue(java.lang.String macroName)``

Get macro value from the widget.

**Parameters:**

``macroName`` - The name of the macro.

**Returns:**

the value of the macro. null if no such macro exist.

**Example:**

.. code-block:: javascript

    var macroValue = widget.getMacroValue("m1"); //get the macro value of "m1"

executeAction(index)
********************

``public void executeAction(int index)``

Run a widget action which is attached to the widget.

**Parameters:**

``index`` - the index of the action in the actions list.

setEnabled(enable)
******************

``public void setEnabled(boolean enable)``

Set this widget to be enabled.

**Parameters:**

``enable`` - ``true`` if the widget should be enabled.

setVisible(visible)
*******************

``public void setVisible(boolean visible)``

Set this widget's visibility.

**Parameters:**

``enable`` - ``true`` if the widget should be visible.

setX(x)
*******

``public void setX(java.lang.Number x)``

Set X position of the widget

**Parameters:**

``x`` - x position in pixel which is relative to its parent.

setY(y)
*******

``public void setY(java.lang.Number y)``

Set Y position of the widget

**Parameters:**

``y`` - y position in pixel which is relative to its parent.

setWidth(width)
***************

``public void setWidth(java.lang.Number width)``

Set widget's width

**Parameters:**

``width`` - width in pixels.

setHeight(height)
*****************

``public void setHeight(java.lang.Number height)``

Set widget's height

**Parameters:**

``height`` - height in pixels.

getChild(name)
**************

``public AbstractBaseEditPart getChild(java.lang.String name)``

Get the direct child of this container by name. Depreciated: Use getWidget(name) instead.

**Parameters:**

``name`` - the name of the child widget

**Returns:**

the widget controller of the child, or null if the widget doesn't exist.

**Example:**

.. code-block:: javascript

    var child = widget.getChild("gauge_2"); //get the gauge widget whose name is gauge_2
    child.setPropertyValue("enable", false); //child is a widget

getWidget(name)
***************

``public AbstractBaseEditPart getWidget(java.lang.String name)``

Get a widget which is a descendant of this container by name.

**Parameters:**

``name`` - the name of the widget which is a descendant of the container

**Returns:**

The widget controller of the widget. Throws exception if the widget doesn't exist.

**Example:**

.. code-block:: javascript

    var gauge2 = widget.getWidget("gauge_2"); //get the gauge widget whose name is gauge_2
    gauge2.setPropertyValue("enable", false); //child is a widget

addChild(model)
***************

``public void addChild(org.csstudio.opibuilder.model.AbstractWidgetModel widgetModel)``

Add a child widget to the container.

**Parameters:**

``widgetModel`` - model of the widget to be added.

addChildToRight(model)
**********************

``public void addChildToRight(org.csstudio.opibuilder.model.AbstractWidgetModel widgetModel)``

Add a child widget to the right of the container.

**Parameters:**

``widgetModel`` - model of the widget to be added.

addChildToBottom(model)
***********************

``public void addChildToBottom(org.csstudio.opibuilder.model.AbstractWidgetModel widgetModel)``

Add a child widget to the bottom of the container.

**Parameters:**

``widgetModel`` - model of the widget to be added.

removeChildByName(name)
***********************

removeChildByName
public void removeChildByName(java.lang.String widgetName)
Remove a child widget by its name.
Parameters:
widgetName - name of the widget.
Throws:
java.lang.RuntimeException - if the widget name does not exist.

removeChild(widget)
*******************

``public void removeChild(AbstractBaseEditPart child)``

Remove a child widget.

**Parameters:**

``child`` - the child widget.

removeChild(index)
******************

``public void removeChild(int index)``

Remove the child at index.

**Parameters:**

``index`` - index of the child.

removeAllChildren()
*******************

``public void removeAllChildren()``

Remove all children.

performAutosize()
*****************

``public void performAutosize()``

Automatically set the container size according to its children's geography size.

getValue( )
***********

``public java.lang.Object getValue()``

By default, it returns an Object Array of its children's value. If ``setValue(Object)`` was called with a non Object[] input value, it will return the value of its first child.

**Overrides:**

``getValue``  in class  AbstractBaseEditPart

**Returns:**

The value of the widget.

setValue(value )
****************

``public void setValue(java.lang.Object value)``

If input value is instance of Object[] and its length is equal or larger than children size, it will write each element of value to each child according children's order. Otherwise, it will write the input value as an whole Object to every child.

**Overrides:**

``setValue``  in class  AbstractBaseEditPart

**Parameters:**

``value`` - the value to be set. It must be the compatible type for the widget. For example, a boolean widget only accept boolean or number.

getPV()
*******

``public PV getPV()``

Get the PV corresponding to the PV Name property. It is same as calling getPV("pv_name") .

**Returns:**

The PV corresponding to the PV Name property, or null if PV Name is not configured for this widget.

**Example:**

.. code-block:: javascript

    importPackage(Packages.org.csstudio.opibuilder.scriptUtil);
    var pv = widget.getPV();
    var value = PVUtil.getDouble(pv); //Get its double value

getPV(pvPropId)
***************

``public PV getPV(java.lang.String pvPropId)``

Some widgets may have more than one PV attached to it. This method can help to get the pv by its property id.

**Parameters:**

``pvPropId`` - the PV property id. For example, pv_name , trace_0_y_pv for XY Graph.

**Returns:**

The corresponding pv for the pvPropId, or null if the pv doesn't exist.

getValue()
**********

``public Object getValue()``

The value that is being displayed on the widget. It is not the value of the attached PV even though they are equals in most cases if there is PV configured for the widget. The value type is specified by the widget, for example, boolean for boolean widget, double for meter and gauge, String for combo.

**Returns:**

The value of the widget.

setValue(value)
***************

``public void setValue(Object value)``

Set the value of the widget. This only takes effect on the visual presentation of the widget and will not write the value to the PV attached to this widget. Since setting value to a widget usually results in figure repaint, this method should be called in UI thread. To call it in non-UI thread, see `setValueInUIThread(value)`_.

**Parameters:**

``value`` - the value to be set. It must be compatible with the widget. For example, a boolean widget only accept boolean or number.

**Throws:**

java.lang.RuntimeException - if the type of the value is not an acceptable type.

setValueInUIThread(value)
*************************

``public void setValueInUIThread(Object value)``

Call setValue(value) in UI Thread. This method can be called in non-UI thread.

**Parameters:**

``value`` - the value to be set. It must be compatible with the widget. For example, a boolean widget only accept boolean or number.

**Throws:**

java.lang.RuntimeException - if the type of the value is not an acceptable type.