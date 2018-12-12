/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.dialog;

import org.phoebus.ui.Messages;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;

/** A Confirmation Alert with "Don't show again" toggle
 *
 *  <p>Compared to the plain {@link Alert},
 *  this variant only allows for content text because
 *  it already configures custom content.
 *  So calling
 *  <code>getDialogPane().setContent(..)</code>
 *  is no longer possible.
 *
 *  <p>After showing this alert, check if the user requested
 *  to hide this type of alert from now on.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlertWithToggle extends Alert
{
    final CheckBox hide = new CheckBox(Messages.DoNotShow);

    /** @param type {@link AlertType}
     *  @param header Header text
     *  @param buttons {@link ButtonType}s
     */
    public AlertWithToggle(final AlertType type,
                           final String header,
                           final ButtonType... buttons)
    {
        this(type, header, "", buttons);
    }

    /** @param type {@link AlertType}
     *  @param header Header text
     *  @param content Content text
     *  @param buttons {@link ButtonType}s
     */
    public AlertWithToggle(final AlertType type,
                           final String header,
                           final String content,
                           final ButtonType... buttons)
    {
        super(type, "", buttons);
        setHeaderText(header);
        setResizable(true);

        if (content.isEmpty())
            getDialogPane().setContent(hide);
        else
        {
            final Label content_label = new Label(content);
            final VBox layout = new VBox(5, content_label, new Separator(), hide);
            getDialogPane().setContent(layout);
        }
    }

    /** @return <code>true</code> if user selected to hide this dialog from now on */
    public boolean isHideSelected()
    {
        return hide.isSelected();
    }
}
