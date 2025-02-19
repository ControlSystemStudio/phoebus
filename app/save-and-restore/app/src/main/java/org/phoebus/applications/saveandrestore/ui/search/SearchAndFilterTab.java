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

package org.phoebus.applications.saveandrestore.ui.search;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.image.ImageView;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.model.search.Filter;
import org.phoebus.applications.saveandrestore.ui.NodeChangedListener;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreController;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreTab;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.nls.NLS;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.ImageCache;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SearchAndFilterTab extends SaveAndRestoreTab implements NodeChangedListener {
    public static final String SEARCH_AND_FILTER_TAB_ID = "SearchAndFilterTab";

    private SearchAndFilterViewController searchAndFilterViewController;
    private final SaveAndRestoreService saveAndRestoreService;

    public SearchAndFilterTab(SaveAndRestoreController saveAndRestoreController) {

        setId(SEARCH_AND_FILTER_TAB_ID);

        saveAndRestoreService = SaveAndRestoreService.getInstance();

        final ResourceBundle bundle = NLS.getMessages(SaveAndRestoreApplication.class);

        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(SearchAndFilterTab.class.getResource("SearchAndFilterView.fxml"));
        loader.setResources(bundle);
        loader.setControllerFactory(clazz -> {
            try {
                if (clazz.isAssignableFrom(SearchAndFilterViewController.class)) {
                    return clazz.getConstructor(SaveAndRestoreController.class)
                            .newInstance(saveAndRestoreController);
                }
                else if(clazz.isAssignableFrom(SearchResultTableViewController.class)){
                    return clazz.getConstructor()
                            .newInstance();
                }
            } catch (Exception e) {
                Logger.getLogger(SearchAndFilterTab.class.getName()).log(Level.SEVERE, "Failed to instantiate SearchAndFilterViewController", e);
            }
            return null;
        });

        try {
            Node node = loader.load();
            controller = loader.getController();
            setContent(node);
            searchAndFilterViewController = loader.getController();
            setOnCloseRequest(event -> searchAndFilterViewController.handleSaveAndFilterTabClosed());
        } catch (IOException e) {
            Logger.getLogger(SearchAndFilterTab.class.getName())
                    .log(Level.SEVERE, "Unable to load search tab content fxml", e);
            return;
        }

        setText(Messages.search);
        setGraphic(new ImageView(ImageCache.getImage(ImageCache.class, "/icons/sar-search_18x18.png")));

        setOnCloseRequest(event -> SaveAndRestoreService.getInstance().removeNodeChangeListener(this));

        saveAndRestoreService.addNodeChangeListener(this);
    }


    @Override
    public void nodeChanged(org.phoebus.applications.saveandrestore.model.Node updatedNode) {
        searchAndFilterViewController.nodeChanged(updatedNode);
    }

    /**
     * Shows a {@link Filter} in the view. If the filter identified through the specified (unique) id does not
     * exist, an error message is show.
     *
     * @param filterId Unique, case-sensitive name of a persisted {@link Filter}.
     */
    public void showFilter(String filterId) {
        JobManager.schedule("Show Filter", monitor -> {
            List<Filter> allFilters;
            try {
                allFilters = saveAndRestoreService.getAllFilters();
            } catch (Exception e) {
                Platform.runLater(() -> ExceptionDetailsErrorDialog.openError(Messages.failedGetFilters, e));
                return;
            }
            Optional<Filter> filterOptional = allFilters.stream().filter(f -> f.getName().equalsIgnoreCase(filterId)).findFirst();
            if (!filterOptional.isPresent()) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setContentText(MessageFormat.format(Messages.filterNotFound, filterId));
                    alert.show();
                });
            } else {
                Filter filter = filterOptional.get();
                Platform.runLater(() ->
                    searchAndFilterViewController.setFilter(filter));
            }
        });
    }
}
