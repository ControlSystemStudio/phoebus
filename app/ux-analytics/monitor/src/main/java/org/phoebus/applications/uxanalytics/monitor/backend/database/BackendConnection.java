package org.phoebus.applications.uxanalytics.monitor.backend.database;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.runtime.app.DisplayInfo;
import org.phoebus.applications.uxanalytics.monitor.backend.image.ImageClient;
import org.phoebus.applications.uxanalytics.monitor.util.ResourceOpenSources;
import org.phoebus.applications.uxanalytics.monitor.representation.ActiveTab;

@FunctionalInterface
public interface BackendConnection {
    public Boolean connect(String hostOrRegion, Integer port, String usernameOrAccessKey, String passwordOrSecretKey);
    public default String getProtocol(){return "";}
    public default String getHost(){return "localhost";}
    public default String getPort(){return "";}
    public default String getDefaultUsername(){return "";}
    public default Integer tearDown(){return -1;}
    public default void setImageClient(ImageClient imageClient){}
    public default void handleClick(ActiveTab who, Widget widget, Integer x, Integer y){}
    public default void handleClick(ActiveTab who, Integer x, Integer y){this.handleClick(who, null, x, y);}
    public default void handleAction(ActiveTab who, Widget widget, ActionInfo info){}
    public default void handlePVWrite(ActiveTab who, Widget widget, String PVName, String value){};
    public default void handleDisplayOpen(DisplayInfo target, DisplayInfo src, ResourceOpenSources how){};
}
