package org.phoebus.applications.utility.preferences;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import org.phoebus.framework.preferences.Preference;

import java.util.Arrays;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class PreferencesTreeController {

    // Model
    Preferences preferences;

    // UI components
    @FXML
    TreeTableView<PreferenceNode> treeTableView;

    @FXML
    TreeTableColumn<PreferenceNode, String> propertyColumn;
    @FXML
    TreeTableColumn<PreferenceNode, String> valueColumn;

    @FXML
    Button refresh;

    @FXML
    public  void initialize()
    {
        propertyColumn.setCellValueFactory(p -> {
            return p.getValue().getValue().nameDisplayProperty();
        });
        valueColumn.setCellValueFactory(p -> {
            return p.getValue().getValue().valueDisplayProperty();
        });
        refresh();
    }

    TreeItem<PreferenceNode> root = null;
    @FXML
    public void refresh()
    {
        Preferences preferences = Preferences.userRoot().node("org");
        try
        {
            Arrays.stream(preferences.childrenNames()).forEach(System.out::println);
            processNode(preferences, root);
            treeTableView.setRoot(root);

        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }


    private void processNode(Preferences node, TreeItem<PreferenceNode> parent) throws BackingStoreException
    {
        TreeItem<PreferenceNode> item = new TreeItem<>(new PreferenceNode(node, node.name(), ""));
        Arrays.stream(node.keys()).forEach(System.out::println);
        Arrays.stream(node.keys()).forEach(key -> {
            item.getChildren().add(new TreeItem<>(new PreferenceNode(node, key, node.get(key,""))));
        });


        List<String> children = Arrays.asList(node.childrenNames());
        while (!children.isEmpty())
        {
            for (String child : children)
            {
                processNode(node.node(child), item);
            }
            break;
        }
        if (parent == null)
        {
            root = item;
        }
        else
        {
            parent.getChildren().add(item);
        }
    }

    private static class PreferenceNode {
        private final Preferences preferences;
        private final SimpleStringProperty nameDisplay = new SimpleStringProperty();
        private final SimpleStringProperty valueDisplay = new SimpleStringProperty();

        private PreferenceNode(Preferences preferences, String name, String value) {
            this.preferences = preferences;
            nameDisplay.setValue(name);
            valueDisplay.setValue(value);
        }

        public Preferences getPreferences() {
            return preferences;
        }

        public String getNameDisplay() {
            return nameDisplay.get();
        }

        public StringProperty nameDisplayProperty() {
            return nameDisplay;
        }

        public String getValueDisplay() {
            return valueDisplay.get();
        }

        public StringProperty valueDisplayProperty() {
            return valueDisplay;
        }
    }
}
