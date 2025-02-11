/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.csstudio.display.builder.model;

import org.csstudio.display.builder.model.spi.ActionEditor;
import org.csstudio.display.builder.model.spi.ActionInfo;

import java.util.Optional;
import java.util.ServiceLoader;

public class ActionEditorFactory {

    public static ActionEditor getActionEditor(Widget widget, ActionInfo actionInfo){
        ServiceLoader<ActionEditor> actionInfos = ServiceLoader.load(ActionEditor.class);
        Optional<ServiceLoader.Provider<ActionEditor>> optional =
                actionInfos.stream().filter(provider -> provider.get().matchesAction(actionInfo.getType())).findFirst();
        if(optional.isEmpty()){
            throw new RuntimeException("Unable to locate editor for action info " + actionInfo.getType());
        }

        ActionEditor actionEditor = optional.get().get();
        actionEditor.configure(widget, actionInfo);
        return actionEditor;
    }
}
