package org.phoebus.applications.display.navigation;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.io.File;

public class DisplayNavigationViewController {

    private File rootFile;
    // Model
    private ProcessOPI model;

    @FXML
    TextField rootFileLabel;
    @FXML
    ListView<File> listView;

    @FXML
    public void initialize() {
    }
    /**
     * The Root file for which the navigation path needs to be displayed.
     * The file must be an .opi or .bob
     * @param rootFile the display files whose navigation path is to be displayed
     */
    public void setRootFile(File rootFile) {
        this.rootFile = rootFile;
        model = new ProcessOPI(this.rootFile);
        refresh();
    }

    public void refresh(){
        rootFileLabel.setText(rootFile.getPath());
        listView.setItems(FXCollections.observableArrayList(model.process()));
    }
}
