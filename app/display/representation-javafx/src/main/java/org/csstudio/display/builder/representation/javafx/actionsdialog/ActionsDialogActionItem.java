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

import javafx.scene.image.Image;
import org.csstudio.display.builder.model.ActionEditorFactory;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.spi.ActionEditor;
import org.csstudio.display.builder.model.spi.ActionInfo;

/**
 * Wrapper class for the {@link ActionsDialog} action list view items. It contains
 * fields describing the action, and the UI components used to edit the action information.
 */
public class ActionsDialogActionItem {

    private final String description;
    private final ActionEditor actionEditor;
    private final Image image;
    /**
     * Constructor.
     * <p>
     * Note that the {@link ActionInfo} object is not maintained as a field in the class as its fields are
     * read-only.
     *
     * @param widget     Widget
     * @param actionInfo {@link ActionInfo}
     */
    public ActionsDialogActionItem(Widget widget, ActionInfo actionInfo) {
        this.description = actionInfo.getDescription();
        this.image = actionInfo.getImage();
        this.actionEditor = ActionEditorFactory.getActionEditor(widget, actionInfo);
    }
    /**
     * @return Description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return A potentially edited {@link ActionInfo} object retrieved from the associated {@link ActionEditor}.
     */
    public ActionInfo getActionInfo() {
        return actionEditor.getActionInfo();
    }

    /**
     *
     * @return The {@link Image} for this action, copied from {@link ActionInfo}.
     */
    public Image getImage(){
        return image;
    }

    /**
     *
     * @return The associated {@link ActionEditor}.
     */
    public ActionEditor getActionEditor(){
        return actionEditor;
    }
}
