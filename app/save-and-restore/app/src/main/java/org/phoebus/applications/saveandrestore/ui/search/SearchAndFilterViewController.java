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

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.search.Filter;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreController;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Stack;

public class SearchAndFilterViewController implements Initializable {

    private SaveAndRestoreController saveAndRestoreController;

    @FXML
    private SearchWindowController searchWindowController;

    @FXML
    private FilterManagementController filterManagementController;

    @FXML
    private SearchQueryEditorController searchQueryEditorController;

    public SearchAndFilterViewController(SaveAndRestoreController saveAndRestoreController){
        this.saveAndRestoreController = saveAndRestoreController;
    }
    @FXML
    public void initialize(URL url, ResourceBundle resourceBundle){
        searchWindowController.setSearchAndFilterViewController(this);
        filterManagementController.setSearchAndFilterViewController(this);
        searchQueryEditorController.setSearchAndFilterViewController(this);
    }

    public void setFilter(Filter filter){
        searchQueryEditorController.setFilter(filter);
        searchWindowController.setFilter(filter);
        search(filter.getQueryString());
    }

    public void search(String queryString){
        searchWindowController.search(queryString);
    }

    public void filterDeleted(Filter filter){
        saveAndRestoreController.filterDeleted(filter);
        searchWindowController.clearFilter(filter);
    }

    public void filterAddedOrUpdated(Filter filter){
        saveAndRestoreController.filterAddedOrUpdated(filter);
        filterManagementController.loadFilters();
    }

    public void locateNode(Stack<Node> stack){
        saveAndRestoreController.locateNode(stack);
    }

    public void saveFilter(Filter filter){
        try {
            SaveAndRestoreService.getInstance().saveFilter(filter);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
