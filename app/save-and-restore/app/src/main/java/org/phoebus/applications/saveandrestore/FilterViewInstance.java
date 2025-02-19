/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore;

import javafx.fxml.FXMLLoader;
import org.phoebus.applications.saveandrestore.ui.search.SearchResultTableViewController;
import org.phoebus.framework.nls.NLS;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.security.tokens.ScopedAuthenticationToken;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Instance maintaining a list of save-and-restore nodes matching a particular filter. Its UI is a subset of the
 * search and filter view of the {@link SaveAndRestoreApplication} UI.
 */
public class FilterViewInstance implements AppInstance {

    private final AppDescriptor appDescriptor;
    public static FilterViewInstance INSTANCE;
    private DockItem dockItem;
    private final SearchResultTableViewController searchResultTableViewController;

    public FilterViewInstance(AppDescriptor appDescriptor) {
        this.appDescriptor = appDescriptor;

        dockItem = null;

        FXMLLoader loader = new FXMLLoader();
        try {
            ResourceBundle resourceBundle = NLS.getMessages(Messages.class);
            loader.setResources(resourceBundle);
            loader.setLocation(FilterViewApplication.class.getResource("/org/phoebus/applications/saveandrestore/ui/search/SearchResultTableView.fxml"));
            dockItem = new DockItem(this, loader.load());
        } catch (Exception e) {
            Logger.getLogger(SaveAndRestoreApplication.class.getName()).log(Level.SEVERE, "Failed loading fxml", e);
        }

        searchResultTableViewController = loader.getController();
        dockItem.addCloseCheck(() -> {
            INSTANCE = null;
            return CompletableFuture.completedFuture(true);
        });

        DockPane.getActiveDockPane().addTab(dockItem);
    }

    @Override
    public AppDescriptor getAppDescriptor() {
        return appDescriptor;
    }

    public void openResource(URI uri) {
        searchResultTableViewController.loadFilter(URLDecoder.decode(uri.getPath().substring(1), StandardCharsets.UTF_8));
    }

    public void raise() {
        dockItem.select();
    }

    public void secureStoreChanged(List<ScopedAuthenticationToken> validTokens) {
        searchResultTableViewController.secureStoreChanged(validTokens);
    }
}
