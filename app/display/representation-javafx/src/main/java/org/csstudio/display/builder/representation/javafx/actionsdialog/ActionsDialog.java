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
 */

package org.csstudio.display.builder.representation.javafx.actionsdialog;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.GridPane;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.ActionInfos;
import org.csstudio.display.builder.representation.javafx.Messages;
import org.phoebus.framework.nls.NLS;
import org.phoebus.framework.preferences.PhoebusPreferenceService;
import org.phoebus.ui.dialog.DialogHelper;

import java.io.IOException;

/**
 * Dialog for editing {@link org.csstudio.display.builder.model.properties.ActionInfo} list
 *
 */
public class ActionsDialog extends Dialog<ActionInfos> {

    private final Widget widget;

    private ActionsDialogController controller;

    /** Create dialog
     *  @param widget Widget
     *  @param initialActions Initial list of actions
     *  @param owner Node that started this dialog
     */
    public ActionsDialog(final Widget widget, final ActionInfos initialActions, final Node owner)
    {
        this.widget = widget;

        setTitle(Messages.ActionsDialog_Title);
        setHeaderText(Messages.ActionsDialog_Info);

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ActionsDialog.fxml"),
                NLS.getMessages(Messages.class));

        try {
            GridPane layout = fxmlLoader.load();
            getDialogPane().setContent(layout);
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            getDialogPane().getStylesheets().add(getClass().getResource("../opibuilder.css").toExternalForm());
            setResizable(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        controller = fxmlLoader.getController();
        controller.setActionInfos(initialActions);

        setResultConverter(button ->
        {
            if (button == ButtonType.OK)
                return controller.getActionInfos();
            return null;
        });

        DialogHelper.positionAndSize(this, owner,
                PhoebusPreferenceService.userNodeForClass(org.csstudio.display.builder.representation.javafx.ActionsDialog.class));
    }
}
