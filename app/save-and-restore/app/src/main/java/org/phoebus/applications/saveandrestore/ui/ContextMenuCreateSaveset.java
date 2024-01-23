/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package org.phoebus.applications.saveandrestore.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.ui.ImageRepository;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;
import org.phoebus.applications.saveandrestore.ui.configuration.ConfigurationFromSelectionController;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.nls.NLS;
import org.phoebus.framework.selection.Selection;
import org.phoebus.security.store.SecureStore;
import org.phoebus.security.tokens.AuthenticationScope;
import org.phoebus.security.tokens.ScopedAuthenticationToken;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.ContextMenuEntry;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  Provide a context menu item for creating or adding to a saveset
 *  from the selection of {@link ProcessVariable}s.
 *
 *  @author Genie Jhang {@literal <changj@frib.msu.edu>}
 */
@SuppressWarnings("nls")
public class ContextMenuCreateSaveset implements ContextMenuEntry
{
    private static final Logger LOGGER = Logger.getLogger(SaveAndRestoreService.class.getName());

    private static final Class<?> supportedTypes = ProcessVariable.class;

    private static final DateTimeFormatter savesetTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private SaveAndRestoreService saveAndRestoreService = null;

    @Override
    public String getName()
    {
        return "Create/add to a Configuration";
    }

    @Override
    public Image getIcon()
    {
        return ImageRepository.CONFIGURATION;
    }

    @Override
    public Class<?> getSupportedType()
    {
        return supportedTypes;
    }

    /**
     * @return <code>true</code> only if user has been authenticated by the save-n-restore service.
     */
    @Override
    public boolean isEnabled(){
        try {
            SecureStore secureStore = new SecureStore();
            ScopedAuthenticationToken token =
                    secureStore.getScopedAuthenticationToken(AuthenticationScope.SAVE_AND_RESTORE);
            return token != null;
        } catch (Exception e) {
            Logger.getLogger(ContextMenuCreateSaveset.class.getName()).log(Level.WARNING, "Unable to retrieve authentication token for " +
                    AuthenticationScope.SAVE_AND_RESTORE.getName() + " scope", e);
            return false;
        }
    }

    @Override
    public void call(final Selection selection)
    {



        saveAndRestoreService = SaveAndRestoreService.getInstance();

        checkRootNode();

        final List<ProcessVariable> pvs = selection.getSelections();

        try {
            FXMLLoader loader = new FXMLLoader();
            ResourceBundle resourceBundle = NLS.getMessages(Messages.class);
            loader.setResources(resourceBundle);
            loader.setLocation(SaveAndRestoreApplication.class.getResource("ui/configuration/ConfigurationFromSelection.fxml"));
            Parent root = loader.load();
            Stage dialog = new Stage();
            dialog.setTitle(getName());
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.getIcons().add(ImageCache.getImage(ImageCache.class, "/icons/logo.png"));
            dialog.setScene(new Scene(root));

            final ConfigurationFromSelectionController controller = loader.getController();
            controller.setSelection(pvs);
            dialog.show();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Cannot load ConfigurationFromSelection.fxml file!", e);
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
                LOGGER.log(Level.SEVERE, alertMessage, e);
            }
        }
    }
}
