/**
 * Copyright (C) 2019 European Spallation Source ERIC.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.applications.saveandrestore.ui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;
import se.esss.ics.masar.model.ConfigPv;
import se.esss.ics.masar.model.Provider;


public class AddPVDialogController {

    @FXML
    private TextField pvNameField;

    @FXML
    private RadioButton ca;

    @FXML
    private RadioButton pva;

    @FXML
    private ToggleGroup providerToggleGroup;

    @FXML
    private Button saveButton;


    private ConfigPv configPv;

    private SimpleStringProperty pvNameProperty = new SimpleStringProperty("");

    private ObjectProperty<Provider> providerChoice = new SimpleObjectProperty<>();

    @FXML
    public void initialize(){

        saveButton.setDisable(true);

        ca.getProperties().put("provider", Provider.ca);
        pva.getProperties().put("provider", Provider.pva);

        providerToggleGroup.selectedToggleProperty().addListener((obs, old, nv) -> {
            providerChoice.set((Provider)nv.getProperties().get("provider"));
        });

        pvNameField.textProperty().bindBidirectional(pvNameProperty);
        pvNameField.textProperty().addListener((ce -> {
            if(pvNameField.textProperty().get().isEmpty()){
                saveButton.setDisable(true);
            }
            else{
                saveButton.setDisable(false);
            }
        }));
    }

    @FXML
    public void closeDialog(ActionEvent event) {
        Node source = (Node)  event.getSource();
        Stage stage  = (Stage) source.getScene().getWindow();
        stage.close();
    }

   @FXML
   public void addPv(ActionEvent event){
       configPv = ConfigPv.builder()
               .pvName(pvNameProperty.get().trim())
               .provider(providerChoice.get())
               .build();
       closeDialog(event);
   }

   public void setModel(ConfigPv configPv){
        pvNameProperty.set(configPv.getPvName());
        providerChoice.set(Provider.pva);
   }

   public ConfigPv getConfigPv(){
        return configPv;
   }



}
