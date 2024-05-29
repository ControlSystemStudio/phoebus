package org.csstudio.display.builder.runtime.app;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.representation.ToolkitListener;

import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.csstudio.display.builder.runtime.WidgetRuntime.logger;

public class DoubleEventExample implements ToolkitListener {

    @Override
    public void handleWrite(Widget widget, Object value) {
        System.out.println("Got a write event");
    }
}
