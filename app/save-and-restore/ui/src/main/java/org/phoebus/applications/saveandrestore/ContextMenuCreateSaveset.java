/**
 * Copyright (C) 2020 Facility for Rare Isotope Beams
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact Information: Facility for Rare Isotope Beam,
 *                      Michigan State University,
 *                      East Lansing, MI 48824-1321
 *                      http://frib.msu.edu
 */
package org.phoebus.applications.saveandrestore;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.service.SaveAndRestoreService;
import org.phoebus.applications.saveandrestore.ui.saveset.SaveSetFromSelectionController;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.selection.Selection;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.ContextMenuEntry;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;

/**
 *  Provide a context menu item for creating or adding to a saveset
 *  from the selection of {@link ProcessVariable}s.
 *
 *  @author Genie Jhang <changj@frib.msu.edu>
 */
@SuppressWarnings("nls")
public class ContextMenuCreateSaveset implements ContextMenuEntry
{
    private static final Logger LOGGER = Logger.getLogger(SaveAndRestoreService.class.getName());

    private static final Class<?> supportedTypes = ProcessVariable.class;

    private static final Image icon = ImageCache.getImage(ImageCache.class, "/icons/save-and-restore/saveset.png");

    private static final DateTimeFormatter savesetTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private SaveAndRestoreService saveAndRestoreService = null;

    @Override
    public String getName()
    {
        return "Create/add to a Saveset";
    }

    @Override
    public Image getIcon()
    {
        return icon;
    }

    @Override
    public Class<?> getSupportedType()
    {
        return supportedTypes;
    }

    @Override
    public void call(final Selection selection) throws Exception
    {
        try {
            saveAndRestoreService = (SaveAndRestoreService) ApplicationContextProvider.getApplicationContext().getAutowireCapableBeanFactory().getBean("saveAndRestoreService");
        } catch (Exception e) {
            try {
                ApplicationService.createInstance(SaveAndRestoreApplication.NAME);
                saveAndRestoreService = (SaveAndRestoreService) ApplicationContextProvider.getApplicationContext().getAutowireCapableBeanFactory().getBean("saveAndRestoreService");
            } catch (Exception ee) {
                LOGGER.severe("Cannot open SaveAndRestore application nor create its service instance!");

                ee.printStackTrace();
            }
        }

        checkRootNode();

        final List<ProcessVariable> pvs = selection.getSelections();

        try {
            SpringFxmlLoader springFxmlLoader = new SpringFxmlLoader();
            Stage dialog = new Stage();
            dialog.setTitle(getName());
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setScene(new Scene((Parent) springFxmlLoader.load("ui/saveset/SaveSetFromSelection.fxml")));

            final SaveSetFromSelectionController controller = springFxmlLoader.getLoader().getController();
            controller.setSelection(pvs);
            dialog.show();
        } catch (Exception e) {
            LOGGER.severe("Cannot load SaveSetFromSelection.fxml file!");

            e.printStackTrace();
        }
    }

    /**
     * When ROOT node is completely empty, create a new folder with the current timestamp.
     */
    private void checkRootNode() {
        Node rootNode = saveAndRestoreService.getRootNode();

        if (saveAndRestoreService.getChildNodes(rootNode).isEmpty()) {
            Node newFolderBuild = Node.builder()
                    .nodeType(NodeType.FOLDER)
                    .name(savesetTimeFormat.format(Instant.now()) + " (Auto-created)")
                    .build();

            try {
                saveAndRestoreService.createNode(rootNode.getUniqueId(), newFolderBuild);
            } catch (Exception e) {
                String alertMessage = "Cannot create a new folder under root node: " + rootNode.getName() + "(" + rootNode.getUniqueId() + ")";
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText(alertMessage);
                alert.show();

                LOGGER.severe(alertMessage);

                e.printStackTrace();
            }
        }
    }
}
