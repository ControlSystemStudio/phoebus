package org.csstudio.display.builder.representation.javafx.actionsdialog;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.Pair;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.model.properties.CallPVActionInfo;
import org.phoebus.ui.autocomplete.PVAutocompleteMenu;

import java.util.HashMap;
import java.util.Map;

public class CallPVActionDetailsController implements ActionDetailsController {
    private final CallPVActionInfo actionInfo;

    @FXML
    private TextField returnPV;
    @FXML
    private TextField description;
    @FXML
    private TextField pvName;

    @FXML
    private Button addParameter;

    @FXML
    private Button removeParameter;

    @FXML
    private TableView<Pair<String, String>> parameterTable;

    private final StringProperty descriptionProperty = new SimpleStringProperty();
    private final StringProperty pvNameProperty = new SimpleStringProperty();
    private final StringProperty returnPVProperty = new SimpleStringProperty();

    private final ObservableList<Pair<String, String>> parameterList = FXCollections.observableArrayList();

    public CallPVActionDetailsController(ActionInfo actionInfo) {
        this.actionInfo = (CallPVActionInfo) actionInfo;
    }

    @FXML
    public void initialize(){
        descriptionProperty.setValue(actionInfo.getDescription());
        pvNameProperty.setValue(actionInfo.getPV());
        returnPVProperty.setValue(actionInfo.getReturnPV());

        description.textProperty().bindBidirectional(descriptionProperty);
        pvName.textProperty().bindBidirectional(pvNameProperty);
        returnPV.textProperty().bindBidirectional(returnPVProperty);

        parameterTable.setItems(parameterList);
        parameterTable.setEditable(true);

        TableColumn<Pair<String, String>, String> nameColumn = new TableColumn<>("Parameter");
        nameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("key"));
        nameColumn.setEditable(true);

        TableColumn<Pair<String, String>, String> parameterColumn = new TableColumn<>("Value");
        parameterColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        parameterColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        parameterColumn.setEditable(true);

        parameterTable.getColumns().add(nameColumn);
        parameterTable.getColumns().add(parameterColumn);

        addParameter.setOnAction(e -> {
            parameterList.add(new Pair<>("A", "B"));
            System.out.println(parameterList.toString());
        });

        removeParameter.setOnAction(e -> {
            Pair<String, String> selectedItem = parameterTable.getSelectionModel().getSelectedItem();
            parameterTable.getItems().remove(selectedItem);
        });

        for (Map.Entry<String, String> arg: actionInfo.getArgs().entrySet()) {
            parameterList.add(new Pair<>(arg.getKey(), arg.getValue()));
        }

        PVAutocompleteMenu.INSTANCE.attachField(pvName);
    }


    @Override
    public ActionInfo getActionInfo() {
        HashMap<String, String> parameters = new HashMap<>();


        for (Pair<String, String> pair: parameterList) {
            parameters.put(pair.getKey(), pair.getValue());
        }

        return new CallPVActionInfo(
                this.descriptionProperty.get(),
                this.pvNameProperty.get(),
                parameters,
                this.returnPVProperty.get()
        );
    }
}
