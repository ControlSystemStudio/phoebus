package org.csstudio.scan.ui.editor;

import java.net.URI;

import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;

import javafx.stage.FileChooser.ExtensionFilter;

/** Application instance for Scan Editor
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanEditorInstance  implements AppInstance
{
    private static final ExtensionFilter[] file_extensions = new ExtensionFilter[]
    {
        new ExtensionFilter("All", "*.*"),
        new ExtensionFilter("Scan", "*.scn"),
    };

    private final ScanEditorApplication app;
    private final DockItemWithInput tab;
    private final ScanEditor editor = new ScanEditor();

    ScanEditorInstance(final ScanEditorApplication app)
    {
        this.app = app;

        URI input = null;
        tab = new DockItemWithInput(this, editor, input , file_extensions, this::doSave);

        DockPane.getActiveDockPane().addTab(tab);
    }

    @Override
    public AppDescriptor getAppDescriptor()
    {
        return app;
    }

    void doSave(final JobMonitor monitor) throws Exception
    {
        // TODO See DisplayEditorInstance
    }
}
