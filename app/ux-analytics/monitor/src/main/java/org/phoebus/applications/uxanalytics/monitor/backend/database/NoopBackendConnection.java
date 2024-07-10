package org.phoebus.applications.uxanalytics.monitor.backend.database;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.phoebus.applications.uxanalytics.monitor.representation.ActiveTab;

import java.util.logging.Level;
import java.util.logging.Logger;

public class NoopBackendConnection implements BackendConnection {

    Logger logger = Logger.getLogger(NoopBackendConnection.class.getName());

    @Override
    public Boolean connect(String hostOrRegion, Integer port, String usernameOrAccessKey, String passwordOrSecretKey) {
        return true;
    }

    @Override
    public void handleClick(ActiveTab who, Integer x, Integer y) {
        logger.log(Level.INFO, "Backend Connection would've handled Click at " + x + "," + y);
    }

    @Override
    public void handleAction(ActiveTab who, Widget widget, ActionInfo info) {
        logger.log(Level.INFO, "Backend Connection would've handled Action" + info.getType() + "from" + who);
    }

    @Override
    public void handlePVWrite(ActiveTab who, Widget widget, String PVName, String value) {
        logger.log(Level.INFO, "Backend Connection would've handled PV Write" + value + "from" + who + "on" + widget);
    }
}
