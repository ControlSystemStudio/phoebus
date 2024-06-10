package org.phoebus.applications.uxanalytics.monitor;

import org.csstudio.display.builder.model.properties.ActionInfo;
import org.phoebus.ui.docking.DockItemWithInput;

import java.io.IOException;

@FunctionalInterface
public interface BackendConnection {
    public Boolean connect(String hostOrRegion, Integer port, String usernameOrAccessKey, String passwordOrSecretKey);
    public default String getProtocol(){return "";}
    public default String getDefaultHost(){return "localhost";}
    public default String getDefaultPort(){return "";}
    public default String getDefaultUsername(){return "";}
    public default Integer tearDown(){return -1;}
    public default void handleClick(DockItemWithInput who, Integer x, Integer y){}
    public default void handleWrite(DockItemWithInput who, ActionInfo info){}
    public default void handleOpenDisplay(DockItemWithInput who, ActionInfo info){}
}
