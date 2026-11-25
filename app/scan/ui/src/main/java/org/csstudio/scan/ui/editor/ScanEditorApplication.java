/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.editor;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.List;

import org.csstudio.scan.ui.ScanURI;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.framework.util.ResourceParser;

/** Application for Scan Editor
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanEditorApplication implements AppResourceDescriptor
{
	private static final List<String> FILE_EXTENSIONS = List.of("scn");
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
    public URL getIconURL()
    {
        return getClass().getResource("/icons/scan.png");
    }

    @Override
    public List<String> supportedFileExtentions()
    {
        return FILE_EXTENSIONS;
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

        // Check for file name
        final File file = ResourceParser.getFile(resource);
        if (file != null)
            instance.open(file);

	    // Check for scan ID
        ScanURI.checkForScanID(resource).ifPresent(id -> instance.open(id));

        return instance;
    }
}
