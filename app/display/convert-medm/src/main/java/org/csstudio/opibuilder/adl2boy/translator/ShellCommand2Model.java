package org.csstudio.opibuilder.adl2boy.translator;

import java.util.ArrayList;
import java.util.List;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.model.properties.ActionInfos;
import org.csstudio.display.builder.model.properties.ExecuteCommandActionInfo;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.widgets.ActionButtonWidget;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.widgetParts.CommandItem;
import org.csstudio.utility.adlparser.fileParser.widgets.ShellCommand;

public class ShellCommand2Model extends AbstractADL2Model<ActionButtonWidget> {

    public ShellCommand2Model(ADLWidget adlWidget, WidgetColor[] colorMap,
            Widget parentModel) throws Exception {
        super(adlWidget, colorMap, parentModel);
    }

    @Override
    public void processWidget(ADLWidget adlWidget) throws Exception {
        ShellCommand commandWidget = new ShellCommand(adlWidget);
        setADLObjectProps(commandWidget, widgetModel);
        setWidgetColors(commandWidget);

        final List<ActionInfo> actions = new ArrayList<>();
        for (CommandItem cmd : commandWidget.getCommandItems())
            if (! cmd.getCommandName().replaceAll("\"", "").isEmpty())
            {
                final String command = cmd.getCommandName() + " " + cmd.getArgs();
                actions.add(new ExecuteCommandActionInfo(cmd.getLabel(), command));
            }

        widgetModel.propText().setValue(commandWidget.getLabel());
        widgetModel.propActions().setValue(new ActionInfos(actions));
    }

    @Override
    public void makeModel(ADLWidget adlWidget,
            Widget parentModel) {
        widgetModel = new ActionButtonWidget();
        ChildrenProperty.getChildren(parentModel).addChild(widgetModel);
    }
}
