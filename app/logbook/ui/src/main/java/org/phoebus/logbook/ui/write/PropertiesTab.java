/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.logbook.ui.write;

import org.phoebus.logbook.ui.Messages;

import javafx.scene.control.Tab;

/**
 * Placeholder for properties tab.
 * @author Evan Smith
 *
 */
public class PropertiesTab extends Tab
{
    public PropertiesTab()
    {
        formatTab();
    }
    
    private void formatTab()
    {
        setClosable(false);
        setText(Messages.Properties);
    }
}
