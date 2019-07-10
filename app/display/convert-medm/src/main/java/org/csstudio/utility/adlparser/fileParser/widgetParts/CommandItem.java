package org.csstudio.utility.adlparser.fileParser.widgetParts;

import org.csstudio.utility.adlparser.fileParser.ADLResource;
import org.csstudio.utility.adlparser.fileParser.ADLWidget;
import org.csstudio.utility.adlparser.fileParser.FileLine;
import org.csstudio.utility.adlparser.fileParser.WrongADLFormatException;
import org.csstudio.utility.adlparser.internationalization.Messages;

public class CommandItem extends WidgetPart{
    private String label;
    public String getLabel() {
        return label;
    }

    public CommandItem(final ADLWidget display)
    throws WrongADLFormatException {
    super(display);
}

    public void setLabel(String label) {
        this.label = label;
    }

    public String getCommandName() {
        return commandName;
    }

    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }

    public String getArgs() {
        return args;
    }

    public void setArgs(String args) {
        this.args = args;
    }

    private String commandName;
    private String args;

    @Override
    void init() {
        name = String.valueOf("command");
        label = String.valueOf("");
        commandName = String.valueOf("");
        args = String.valueOf("");
    }

    @Override
    void parseWidgetPart(ADLWidget widgetPart) throws WrongADLFormatException {
        for (FileLine fileLine : widgetPart.getBody()) {
            String parameter = fileLine.getLine();
            if (parameter.trim().startsWith("//")) { //$NON-NLS-1$
                continue;
            }
            String head = parameter.split("=")[0]; //$NON-NLS-1$
            String tmp = "";
            try {
                tmp = parameter.substring(head.length() + 1);
            } catch (StringIndexOutOfBoundsException exp) {
                throw new WrongADLFormatException(
                        Messages.RelatedDisplayItem_WrongADLFormatException_Begin + head
                                + Messages.RelatedDisplayItem_WrongADLFormatException_Middle
                                + fileLine + "(" + widgetPart.getObjectNr() + ":" + widgetPart.getType()
                                + ")[" + parameter + "]");
            }
            String row = tmp;
            head = head.trim().toLowerCase();
            if (head.equals("label")) { //$NON-NLS-1$
                label = row;
            } else if (head.equals("name")) { //$NON-NLS-1$
                commandName = row;
            } else if (head.equals("args")) { //$NON-NLS-1$
                  args = row;
            } else {
                throw new WrongADLFormatException(
                        Messages.RelatedDisplayItem_WrongADLFormatException_Begin + head
                                + Messages.RelatedDisplayItem_WrongADLFormatException_Middle
                                + fileLine + "(" + widgetPart.getObjectNr() + ":" + widgetPart.getType()
                                + ")");
            }
        }
    }

    @Override
    public Object[] getChildren() {
        Object[] ret = new Object[3];
        ret[0] = new ADLResource(ADLResource.RD_LABEL, label);
        ret[1] = new ADLResource(ADLResource.RD_NAME, commandName);
        ret[2] = new ADLResource(ADLResource.RD_ARGS, args);

        return ret;
    }

}
