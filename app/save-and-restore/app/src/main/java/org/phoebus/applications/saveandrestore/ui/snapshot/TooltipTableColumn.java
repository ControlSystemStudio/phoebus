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

package org.phoebus.applications.saveandrestore.ui.snapshot;

import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

import javax.tools.Tool;

/**
 * <code>TooltipTableColumn</code> is the common table column implementation, which can also provide the tooltip.
 *
 * @param <T> the type of the values displayed by this column
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 */
public class TooltipTableColumn<T> extends TableColumn<TableEntry, T> {

    private String text;

    private Label label;

    public void setTooltip(String tooltip) {
        label.setTooltip(new Tooltip(tooltip));
    }

    public void setLabelText(String labelText){
        label.textProperty().set(labelText);
    }

    public String getTooltip(){
        return label.getTooltip().textProperty().get();
    }

    public String getLabelText(){
        return label.textProperty().get();
    }

    public TooltipTableColumn(){
        label = new Label();
        setGraphic(label);
    }

    TooltipTableColumn(String text, String tooltip, int minWidth) {
        setup(text, tooltip, minWidth, -1, true);
    }

    TooltipTableColumn(String text, String tooltip, int minWidth, int prefWidth, boolean resizable) {
        setup(text, tooltip, minWidth, prefWidth, resizable);
    }

    public void setPreferredWidth(int prefWidth){
        setPrefWidth(prefWidth);
    }


    public void setup(String text, String tooltip, int minWidth, int prefWidth, boolean resizable) {
        label = new Label(text);
        label.setTooltip(new Tooltip(tooltip));
        label.setTextAlignment(TextAlignment.CENTER);
        setGraphic(label);

        if (minWidth != -1) {
            setMinWidth(minWidth);
        }
        if (prefWidth != -1) {
            setPrefWidth(prefWidth);
        }
        setResizable(resizable);

        this.text = text;
    }

    void setSaved(boolean saved) {
        if (saved) {
            label.setText(text);
        } else {
            String t = this.text;
            if (text.indexOf('\n') > 0) {
                t = "*" + t.replaceFirst("\n", "*\n");
            } else {
                t = "*" + t + "*";
            }
            label.setText(t);
        }
    }
}
