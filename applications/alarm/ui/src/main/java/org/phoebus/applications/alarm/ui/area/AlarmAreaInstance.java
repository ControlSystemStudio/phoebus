package org.phoebus.applications.alarm.ui.area;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.util.logging.Level;

import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

import javafx.scene.Node;
import javafx.scene.control.Label;

public class AlarmAreaInstance implements AppInstance
{
	/** Singleton instance maintained by {@link AlarmAreaApplication} */
    static AlarmAreaInstance INSTANCE = null;

    private final AlarmAreaApplication app;

    private AlarmClient client;
    private final DockItem tab;

    public AlarmAreaInstance(final AlarmAreaApplication app)
    {
        this.app = app;
        tab = new DockItem(this, create());
        tab.addCloseCheck(() ->
        {
            dispose();
            return true;
        });
        tab.addClosedNotification(() -> INSTANCE = null);
        DockPane.getActiveDockPane().addTab(tab);
    }

    @Override
    public AppDescriptor getAppDescriptor()
    {
        return app;
    }

    void raise()
    {
        tab.select();
    }

    private Node create()
    {
        try
        {
            client = new AlarmClient(AlarmSystem.server, AlarmSystem.config_name);
            final AlarmAreaView area_view = new AlarmAreaView(client);
            client.start();
            return area_view;
        }
        catch (final Exception ex)
        {
            logger.log(Level.WARNING, "Cannot create alarm area", ex);
            return new Label("Cannot create alarm area");
        }
    }

    private void dispose()
    {
        if (client != null)
        {
            client.shutdown();
            client = null;
        }
    }

}
