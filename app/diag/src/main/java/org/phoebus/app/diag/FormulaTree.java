package org.phoebus.app.diag;

import javafx.fxml.FXMLLoader;
import org.phoebus.app.diag.ui.FormulaTreeController;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An instance of the FormulaTree application to display available formulas.
 */
public class FormulaTree implements AppInstance {
    private final FormulaTreeApp app;
    private FormulaTreeController controller;
    private DockItem tab;

    FormulaTree(final FormulaTreeApp app) {
        this.app = app;
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(this.getClass().getResource("ui/FormulaTree.fxml"));
            tab = new DockItem(this, loader.load());
            controller = loader.getController();
            DockPane.getActiveDockPane().addTab(tab);
        } catch (IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.WARNING, "Cannot load UI", e);
        }
    }

    @Override
    public AppDescriptor getAppDescriptor() {
        return app;
    }

}
