/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */
package org.csstudio.display.actions;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.ActionInfos;
import org.csstudio.display.builder.model.spi.ActionInfo;
import org.phoebus.framework.macros.Macros;
import org.phoebus.ui.javafx.ApplicationWrapper;
import org.csstudio.display.builder.representation.javafx.actionsdialog.ActionsDialog;

import javafx.stage.Stage;

/** Demo of {@link ActionsDialog}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ActionsDialogDemo extends ApplicationWrapper
{
    public static void main(final String[] args)
    {
        launch(ActionsDialogDemo.class, args);
    }

    @Override
    public void start(final Stage stage)
    {

        final Widget widget = new Widget("demo");
        ServiceLoader<ActionInfo> actionInfoList = ServiceLoader.load(ActionInfo.class);
        final ActionInfos actionInfos =
                new ActionInfos(actionInfoList.stream().map(p -> p.get()).collect(Collectors.toList()));
        final ActionsDialog dialog = new ActionsDialog(widget, actionInfos, null);
        final Optional<ActionInfos> result = dialog.showAndWait();
        if (result.isPresent())
        {
            if (result.get().isExecutedAsOne())
                System.out.println("Execute all commands as one:");

            System.out.println("Execute " + result.get().getActions());
        }
        else
            System.out.println("Cancelled");
    }
}
