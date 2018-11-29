package org.phoebus.applications.filebrowser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.framework.nls.NLS;
import org.phoebus.framework.persistence.Memento;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;

@SuppressWarnings("nls")
public class FileBrowser implements AppInstance
{
    public static final Logger logger = Logger.getLogger(FileBrowser.class.getPackageName());

    private static final String DIRECTORY = "directory";

    private final AppDescriptor app;

    private FileBrowserController controller;

    FileBrowser(final AppDescriptor app, final File directory)
    {
        this.app = app;

        final FXMLLoader fxmlLoader;

        Node content;
        try
        {
            final URL fxml = getClass().getResource("FileBrowser.fxml");
            final InputStream iStream = NLS.getMessages(FileBrowser.class);
            final ResourceBundle bundle = new PropertyResourceBundle(iStream);
            fxmlLoader = new FXMLLoader(fxml, bundle);
            content = (Node) fxmlLoader.load();
            controller = fxmlLoader.getController();
        }
        catch (IOException ex)
        {
            logger.log(Level.WARNING, "Cannot load UI", ex);
            content = new Label("Cannot load UI");
        }

        final DockItem tab = new DockItem(this, content);
        DockPane.getActiveDockPane().addTab(tab);

        if (controller != null  &&  directory != null)
            controller.setRoot(directory);

        tab.addClosedNotification(controller::shutdown);
    }

    @Override
    public AppDescriptor getAppDescriptor()
    {
        return app;
    }


    @Override
    public void restore(final Memento memento)
    {
        memento.getString(DIRECTORY).ifPresent(dir -> controller.setRoot(new File(dir)));
    }

    @Override
    public void save(final Memento memento)
    {
        if (controller != null)
            memento.setString(DIRECTORY, controller.getRoot().toString());
    }
}
