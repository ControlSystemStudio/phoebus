/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.editor;

import java.net.URI;

import org.phoebus.framework.spi.AppResourceDescriptor;

/** Application for Scan Editor
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanEditorApplication implements AppResourceDescriptor
{
    public static final String NAME = "scan_editor";
    public static final String DISPLAY_NAME = "Scan Editor";

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public String getDisplayName()
    {
        return DISPLAY_NAME;
    }

    @Override
    public ScanEditorInstance create()
    {
        return new ScanEditorInstance(this);
    }

    @Override
    public ScanEditorInstance create(final URI resource)
    {
        final ScanEditorInstance instance = create();
        // TODO Set resource
        return instance;
    }
}
