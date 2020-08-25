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
import javafx.scene.control.TreeTableColumn;

@SuppressWarnings("nls")
public class FileBrowser implements AppInstance
{
    /** Logger for all file browser code */
    public static final Logger logger = Logger.getLogger(FileBrowser.class.getPackageName());

    /** Memento tags */
    private static final String DIRECTORY = "directory",
                                SHOW_COLUMN = "show_col",
                                WIDTH = "col_width";

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
            final ResourceBundle bundle = NLS.getMessages(FileBrowser.class);
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
        int i = 0;
        for (TreeTableColumn<?,?> col : controller.getView().getColumns())
        {
            if (! memento.getBoolean(SHOW_COLUMN+i).orElse(true))
                col.setVisible(false);
            memento.getNumber(WIDTH+i).ifPresent(width -> col.setPrefWidth(width.doubleValue()));
            ++i;
        }
    }

    @Override
    public void save(final Memento memento)
    {
        if (controller == null)
            return;
        memento.setString(DIRECTORY, controller.getRoot().toString());
        int i = 0;
        for (TreeTableColumn<?,?> col : controller.getView().getColumns())
        {
            if (! col.isVisible())
                memento.setBoolean(SHOW_COLUMN+i, false);
            memento.setNumber(WIDTH+i, col.getWidth());
            ++i;
        }
    }
}
