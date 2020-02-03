/*************************************************************************\
 * Copyright (c) 2010  UChicago Argonne, LLC
 * This file is distributed subject to a Software License Agreement found
 * in the file LICENSE that is included with this distribution.
/*************************************************************************/

package org.csstudio.opibuilder.adl2boy.translator;

import java.util.ArrayList;
import java.util.List;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.model.properties.ActionInfos;
import org.csstudio.display.builder.model.properties.OpenDisplayActionInfo;
import org.csstudio.display.builder.model.properties.OpenDisplayActionInfo.Target;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.widgets.ActionButtonWidget;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.widgetParts.RelatedDisplayItem;
import org.csstudio.utility.adlparser.fileParser.widgets.RelatedDisplay;
import org.phoebus.framework.macros.Macros;

/**
 * Convert MEDMs related display to BOYs MenuButton
 *
 * @author John Hammonds, Argonne National Laboratory
 *
 */
public class RelatedDisplay2Model extends AbstractADL2Model<ActionButtonWidget> {

    public RelatedDisplay2Model(ADLWidget adlWidget, WidgetColor[] colorMap,
            Widget parentModel) throws Exception {
        super(adlWidget, colorMap, parentModel);
    }

    @Override
    public void makeModel(ADLWidget adlWidget, Widget parentModel){
        widgetModel = new ActionButtonWidget();
        ChildrenProperty.getChildren(parentModel).addChild(widgetModel);
    }

    /**
     * @param adlWidget
     * @throws Exception
     */
    @Override
    public void processWidget(ADLWidget adlWidget) throws Exception {
        RelatedDisplay rdWidget = new RelatedDisplay(adlWidget);
        setADLObjectProps(rdWidget, widgetModel);
        setADLDynamicAttributeProps(rdWidget, widgetModel);
        setWidgetColors(rdWidget);

        final List<ActionInfo> actions = new ArrayList<>();
        final RelatedDisplayItem[] displays = rdWidget.getRelatedDisplayItems();
        if (displays != null  &&  displays.length > 0)
        {
            // For menu, always new tab because menu button doesn't
            // allow user to use 'Ctrl' etc at runtime.
            // Users can always close the new tab, but have no other way
            // to get new tab.
            final Target target = displays.length == 1 ? Target.REPLACE : Target.TAB;
            for (RelatedDisplayItem display : displays)
            {
                final ActionInfo action = createOpenDisplayAction(display, target);
                if (action != null)
                    actions.add(action);
            }

            widgetModel.propActions().setValue(new ActionInfos(actions));
        }

        String label = rdWidget.getLabel();
        if (label != null)
        {
            // leading "-" was used to flag not
            // using the icon. Just don't use
            // the icon and throw this away
            if (label.startsWith("-"))
                label = label.substring(1);
            widgetModel.propText().setValue(label);
        }
    }

    /**
     * @param target
     * @param rdDisplays
     * @param ii
     * @return
     */
    public ActionInfo createOpenDisplayAction(final RelatedDisplayItem rdDisplay, Target target)
    {
        final String file = rdDisplay.getFileName()
                                     .replaceAll("\"", "")
                                     .replace(".adl", ".opi");
        if (file.isEmpty())
            return null;

        String description = file;
        if (rdDisplay.getLabel() != null)
            description = rdDisplay.getLabel().replaceAll("\"", "");

        if  (rdDisplay.getPolicy() != null)
        {    // policy is present
            if (rdDisplay.getPolicy().replaceAll("\"", "").equals("replace display"))
                target = target.REPLACE;
            else
                target = target.TAB;
        }

        return new OpenDisplayActionInfo(description, file, getMacros(rdDisplay), target);
    }

    public Macros getMacros(RelatedDisplayItem rdDisplay)
    {
        if (rdDisplay.getArgs() != null && !rdDisplay.getArgs().isEmpty())
        {
            String args = rdDisplay.getArgs().replaceAll("\"", "");
            return makeMacros(args);
        }
        return new Macros();
    }
}
